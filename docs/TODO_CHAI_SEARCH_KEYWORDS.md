# 商品搜索关键词（keywords / search_text）待实现

> 状态：S1～S4、S7 已完成；Admin 关键词录入入口暂隐藏（数据与保存逻辑保留）；S5～S6 搜索优化暂缓。不上 ES，先用 MySQL。  
> 目标：支持「溪谷 牛肉」这类跨品牌+品名+预埋词的多词 AND 搜索（空白分词）。

## 背景与边界

| 点 | 结论 |
|----|------|
| 为何不上 ES | 4C8G 资源紧；当前量级 MySQL 足够；量上来再评估 ES |
| 分词无法单独解决 | 「牛肉」≠「牛栏坑肉桂」子串，靠运营预埋关键词 |
| 品牌名 | 写入 `search_text`（默认预埋），为了能搜到品牌片段 |
| 查询 | 空白（及规范化后的分隔）拆词 → 多词 AND `search_text LIKE`；**限制 AND 词数**（建议最多 3） |
| Admin | 现「商品名称」模糊可改为按 `search_text` 搜（或等价新条件）；brand / type 筛选保留 |
| H5 | 售卖/回收列表只查 **SKU.`search_text`** |

## 字段设计

| 字段 | 谁写 | 含义 |
|------|------|------|
| `keywords` | **仅 SPU 维护** | 额外预埋词；库内空格分隔存储 |
| `search_text` | **系统生成，只读** | `品牌名 + 商品名 + keywords`（空段丢掉、空白规范） |

- SPU、SKU **都有**这两列（SKU 冗余便于 H5 直查）。  
- **SKU 的 keywords / search_text 与品牌、茶类一样继承 SPU**，SKU 侧不可单独编辑。  
- 分隔符作用：避免品牌/品名/关键词首尾相连导致 `%LIKE%` 误匹配。  
- 录入：SPU 大表单 + 独立「维护关键词」入口，均为 chip；保存再 join；限制关键词个数与单词长度；输入可兼容逗号，**保存统一成空格**。  
- `copyFromSpu` /「从 SPU 同步共享字段」/ `saveBatchForSpu` 强制带上 keywords 并重算 search_text。

重建触发：改品牌名（级联该品牌 SPU/SKU）、SPU 改 keywords/换品牌、从 SPU 同步共享字段、保存 SKU 批次时继承。

## DDL（约定）

### 全量

- 更新 `db/SQL_CHAI_DOMAIN.sql` 为**最新完整**表结构 + 索引。

### 增量（临时，发版用）

- `db/tmp/20260929_chai_search_keywords.sql`

说明：

- **`search_text` 不加 BTree**（`%词%` 用不上）。  
- 单独 `idx_deleted` 已改为 `idx_deleted_status(deleted, status)`。

## 分步实现（按序）

### S1 DDL

- [x] 全量改 `db/SQL_CHAI_DOMAIN.sql`
- [x] 写临时增量 SQL：`db/tmp/20260929_chai_search_keywords.sql`（目标库需手工执行）

### S2 实体

- [x] admin：`ChaiSpu`、`ChaiSku` 增加 `keywords`、`searchText`
- [x] api：`ChaiSku` 增加同名字段（api 无独立 SPU 实体，H5 搜 SKU）

### S3 重建逻辑

- [x] `ChaiSearchTextUtil`：规范化关键词 + 拼接 search_text
- [x] SPU 保存：写 keywords、重算 search_text；keywords/品牌变更时同步全部 SKU
- [x] 品牌改名：级联重算该品牌下 SPU/SKU 的 search_text
- [x] `applySharedFromSpu` / `saveBatchForSpu` / 同步已删 SKU：继承 keywords 并重算 search_text

### S4 Admin 编辑 UI

- [x] **独立「维护关键词」入口**（SPU 列表「关键词」弹层）
- [x] **SPU 大表单**同步提供关键词 chip 编辑
- [x] chip 增删；个数/长度限制（`ChaiSearchTextUtil`）
- [x] search_text 只读预览
- [x] SKU 编辑页不提供关键词编辑（仅继承/同步）
- [ ] **暂隐录入入口**（列表「关键词」链接、大表单 chip 区 `display:none`；hidden 仍回传已有 keywords；恢复时去掉隐藏即可）

### S5 Admin 列表（暂缓）

- [ ] 「商品名称」模糊改为（或等价新增）按 `search_text` 多词 AND；限制 AND 词数
- [ ] brand / type / status / deleted 等筛选不变

### S6 H5 API（暂缓）

- [ ] `ChaiSkuSaleQueryService` / `ChaiSkuRecycleQueryService`：keyword 空白分词 + AND `search_text`；限制 AND 词数
- [ ] 去掉「整串名称 LIKE / 仅品牌全名精确」作为主路径（或保留兼容策略需另定）

### S7 Tools 刷库

- [x] `ChaiToolsController` 增加：历史数据回填 `search_text`（keywords 空则仅品牌+品名）
- [x] 发版后执行一次；可与「仅空值 / 全量重算」同款交互（茶业数据工具页）

## 查询与性能（当前量级）

- 先等值过滤：`deleted` / `status` / `brand` / `type` / 有货等，再多词 `LIKE`。  
- AND 词数上限（建议 3）。  
- 3k～6k 行 `%LIKE%` 可接受；偏慢再考虑 ngram FULLTEXT；更大或要相关度再上 ES。

## 刻意不做（本期）

- Elasticsearch / Meilisearch  
- 单独 `aliases` 字段（已由 `keywords` + `search_text` 覆盖）  
- 给 `search_text` 加普通 BTree  
- 无预埋时的「牛肉≈牛栏坑」语义联想  
- SKU 独立编辑关键词
