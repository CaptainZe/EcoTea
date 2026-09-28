# 商品搜索关次（keywords / search_text）待实现

> 状态：**仅规划，代码未接入**。不上 ES，先用 MySQL；SKU 上架约 3k / 总量约 6k，可支撑。  
> 目标：支持「溪谷 牛肉」这类跨品牌+品名+预埋词的多词 AND 搜索（空白分词）。

## 背景与边界

| 点 | 结论 |
|----|------|
| 为何不上 ES | 4C8G 资源紧；当前量级 MySQL 足够；量上来再评估 ES |
| 分词无法单独解决 | 「牛肉」≠「牛栏坑肉桂」子串，靠运营预埋关次 |
| 品牌名 | 写入 `search_text`（默认预埋），为了能搜到品牌片段 |
| 查询 | 空白（及规范化后的分隔）拆词 → 多词 AND `search_text LIKE`；**限制 AND 词数**（建议最多 3） |
| Admin | 现「商品名称」模糊可改为按 `search_text` 搜（或等价新条件）；brand / type 筛选保留 |
| H5 | 售卖/回收列表只查 **SKU.`search_text`** |

## 字段设计

| 字段 | 谁写 | 含义 |
|------|------|------|
| `keywords` | 运营可编 | 额外预埋词；库内空格分隔存储 |
| `search_text` | **系统生成，只读** | `品牌名 + 商品名 + keywords`（空段丢掉、空白规范） |

- SPU、SKU **都有**这两列。  
- 分隔符作用：避免品牌/品名/关次首尾相连导致 `%LIKE%` 误匹配（如「留香」+「牛栏」→「香牛」）。  
- 录入：chip 逐词增删，保存再 join；限制关次个数与单词长度；输入可兼容逗号/中文逗号，**保存统一成空格**。  
- SPU / SKU 均可编 keywords；**默认同步**，纳入现有「从 SPU 同步共享字段」；新建 SKU `copyFromSpu` 带上 keywords 并生成 search_text。

重建触发：改品牌名（级联相关商品）、改商品名、改 keywords、从 SPU 同步后。

## DDL（约定）

### 全量

- 更新 `db/SQL_CHAI_DOMAIN.sql` 为**最新完整**表结构 + 索引。

### 增量（临时，发版用）

- 另存例如 `db/tmp/YYYYMMDD_chai_search_keywords.sql`，用完可归档。

### 建议增量内容

```sql
ALTER TABLE `tea`.`chai_spu`
  ADD COLUMN `keywords` varchar(512) NOT NULL DEFAULT ''
    COMMENT '搜索关次（空格分隔，运营可编）' AFTER `name`,
  ADD COLUMN `search_text` varchar(1024) NOT NULL DEFAULT ''
    COMMENT '检索文本（品牌名+商品名+关次，系统生成）' AFTER `keywords`,
  ADD INDEX `idx_brand`(`brand`),
  ADD INDEX `idx_type`(`type`),
  ADD INDEX `idx_deleted_status`(`deleted`, `status`),
  DROP INDEX `idx_deleted`;

ALTER TABLE `tea`.`chai_sku`
  ADD COLUMN `keywords` varchar(512) NOT NULL DEFAULT ''
    COMMENT '搜索关次（空格分隔，运营可编）' AFTER `name`,
  ADD COLUMN `search_text` varchar(1024) NOT NULL DEFAULT ''
    COMMENT '检索文本（品牌名+商品名+关次，系统生成）' AFTER `keywords`,
  ADD INDEX `idx_brand`(`brand`),
  ADD INDEX `idx_type`(`type`),
  ADD INDEX `idx_deleted_status`(`deleted`, `status`),
  DROP INDEX `idx_deleted`;
```

说明：

- **`search_text` 不加 BTree**（`%词%` 用不上）。  
- 删掉单独 `idx_deleted`：已有只按 `deleted` 的查询（看板 count、列表仅筛删除等），但 `(deleted, status)` **最左前缀**可覆盖，避免与复合索引冗余。  
- 先 `ADD INDEX idx_deleted_status` 再 `DROP INDEX idx_deleted`。

## 分步实现（按序）

### S1 DDL

- [ ] 全量改 `db/SQL_CHAI_DOMAIN.sql`（两表加列；索引：`idx_brand`、`idx_type`、`idx_deleted_status`；去掉 `idx_deleted`）
- [ ] 写临时增量 SQL 并在目标库执行

### S2 实体

- [ ] admin / api：`ChaiSpu`、`ChaiSku` 增加 `keywords`、`searchText`

### S3 重建逻辑

- [ ] 公共拼接工具：品牌名 + name + keywords → `search_text`
- [ ] SPU/SKU 保存时写 keywords、重算 search_text
- [ ] 品牌改名：级联重算相关 SPU/SKU 的 search_text
- [ ] `copyFromSpu` /「从 SPU 同步共享字段」纳入 keywords，并重算 search_text

### S4 Admin 编辑 UI

- [ ] SPU、SKU 关次 chip（可单独删）；个数/长度限制
- [ ] search_text 只读展示（可选）
- [ ] 同步按钮行为与商品名等共享字段一致

### S5 Admin 列表

- [ ] 「商品名称」模糊改为（或等价新增）按 `search_text` 多词 AND；限制 AND 词数
- [ ] brand / type / status / deleted 等筛选不变

### S6 H5 API

- [ ] `ChaiSkuSaleQueryService` / `ChaiSkuRecycleQueryService`：keyword 空白分词 + AND `search_text`；限制 AND 词数
- [ ] 去掉「整串名称 LIKE / 仅品牌全名精确」作为主路径（或保留兼容策略需另定）

### S7 Tools 刷库

- [ ] `ChaiToolsController` 增加：历史数据回填 `search_text`（keywords 空则仅品牌+品名）
- [ ] 发版后执行一次；可与「仅空值 / 全量重算」同款交互

## 查询与性能（当前量级）

- 先等值过滤：`deleted` / `status` / `brand` / `type` / 有货等，再多词 `LIKE`。  
- AND 词数上限（建议 3）。  
- 3k～6k 行 `%LIKE%` 可接受；偏慢再考虑 ngram FULLTEXT；更大或要相关度再上 ES。

## 刻意不做（本期）

- Elasticsearch / Meilisearch  
- 单独 `aliases` 字段（已由 `keywords` + `search_text` 覆盖）  
- 给 `search_text` 加普通 BTree  
- 无预埋时的「牛肉≈牛栏坑」语义联想
