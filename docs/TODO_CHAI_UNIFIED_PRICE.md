# SPU/SKU 统一价（unified_price）待实现

> 状态：**暂缓，先不做**；方案已定，待排期。  
> 目标：部分商品不按半年批次区分价格，SPU 标记「统一价」后只允许 1 个 SKU，展示统一为「统一价」。

## 背景与边界

| 点 | 结论 |
|----|------|
| 业务含义 | 该 SPU 全店统一价，**不按年份/生产批次** 拆 SKU |
| 现有约束 | `year` / `prod_batch` 均为 NOT NULL；SKU 唯一键 `(spu_id, year, prod_batch)` |
| 年/批次无效时 | 用哨兵 **`0` / `0`** 占位（不改可空、不动唯一索引） |
| 展示 | Admin + API/H5：统一价时年/批次相关展示均为 **「统一价」**（不要显示 `0`、不要留空） |
| 录入 | **仅 SPU 维护**；SKU 继承（与品牌、茶类、keywords 一致） |

## 字段设计

| 字段 | 表 | 谁写 | 含义 |
|------|-----|------|------|
| `unified_price` | `chai_spu` | 运营在 SPU 表单选是/否 | 0=按半年；1=统一价 |
| `unified_price` | `chai_sku` | **继承自 SPU**，SKU 侧不可单独改 | 冗余便于列表/H5 直查 |

- 类型：`tinyint NOT NULL DEFAULT 0`（对齐 `non_sale`）。  
- 统一价=1 时：SPU 与唯一那条 SKU 的 `year`、`prod_batch` 均为 `0`。  
- `copyFromSpu` /「从 SPU 同步共享字段」/ `saveBatchForSpu` / 同步已删 SKU：强制带上 `unified_price`。

## 业务规则

1. **统一价=是**
   - SPU 表单：年份、生产批次禁用/隐藏，提交写 `0,0`。
   - 维护 SKU：不预填 6 个半年，只允许 **1** 条未删除 SKU；隐藏年/批次编辑与「添加锚点」。
   - `saveBatchForSpu`：有效 SKU 数必须为 1，且 year/batch 强制 `0,0`；`unified_price` 从 SPU 写入。
2. **统一价=否**（普通半年价）
   - 行为与现网一致：锚点年/批次必填，可多 SKU。
3. **切换校验**
   - 已有 **多于 1 条** 未删除 SKU 时，禁止改为统一价（须先删到 1 条或清空后再改）。
   - 统一价改回普通价：须重新填写锚点年/批次，方可再扩多半年 SKU。

## 展示约定

| 场景 | 行为 |
|------|------|
| Admin 列表（SPU/SKU/库存/开单选品等） | 年、批次列：统一价时显示「统一价」 |
| Admin 维护 SKU 卡片头 / 锚点区 | 同上 |
| API `prodBatchShow`（及同类格式化） | `unifiedPrice==1` → 返回 `"统一价"` |
| H5 列表标签、详情/报价「批次」行 | 依赖 `prodBatchShow` 显示「统一价」；空值逻辑不要把「统一价」当无 |

建议集中封装展示文案（admin 工具方法 + api `formatProdBatch`），避免各页硬编码。

## DDL（约定）

### 全量

- 更新 `db/SQL_CHAI_DOMAIN.sql`：`chai_spu`、`chai_sku` 增加 `unified_price`。

### 增量（临时，发版用）

- `db/tmp/YYYYMMDD_chai_unified_price.sql`（发版时再落文件）

## 分步实现（按序，待做）

### U1 DDL

- [ ] 全量改 `db/SQL_CHAI_DOMAIN.sql`
- [ ] 写临时增量 SQL（目标库手工执行）

### U2 实体

- [ ] admin：`ChaiSpu`、`ChaiSku` 增加 `unifiedPrice`
- [ ] api：`ChaiSku` 增加同名字段
- [ ] 继承写入：`applySharedFromSpu` / `saveBatchForSpu` / 同步已删 SKU

### U3 Admin SPU

- [ ] `spu/edit.html`：统一价是/否；选「是」时禁用年/批次并提交 `0,0`
- [ ] `ChaiSpuController` / `ChaiSpuService` 保存校验与切换校验
- [ ] 列表年/批次列展示「统一价」

### U4 Admin SKU

- [ ] `sku/editBySpu`：统一价只预填/允许 1 卡；藏年批次与加锚点
- [ ] `saveBatchForSpu` 强制条数与 `0,0`、`unified_price` 继承
- [ ] SKU 相关列表/选品展示「统一价」

### U5 API / H5

- [ ] `formatProdBatch`（售卖/回收等）：统一价 → `"统一价"`
- [ ] VO 可继续用 `prodBatchShow`；必要时透出 `unifiedPrice`
- [ ] 抽查 H5 列表、详情、报价、购物车等展示是否均为「统一价」

## 刻意不做（本期）

- 把 `year` / `prod_batch` 改为可空或拆唯一索引  
- 工具页「一键软删多余 SKU 再改统一价」（先人工删到 1 条）  
- 与搜索关键词（S5/S6）耦合；两套需求独立  
