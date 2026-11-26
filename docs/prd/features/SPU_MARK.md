# SPU 标记区（跨端）

> Admin 维护；SKU **同步**同名字段。发版并入 **1.5.0**：[`CHANGELOG_ADMIN.md`](../../changelog/CHANGELOG_ADMIN.md)。  
> Admin 总览：[`PRD_ADMIN.md`](../PRD_ADMIN.md)。  
> **实现状态（1.5.0）**：实体 / 保存校验与 SKU 同步 / 编辑页 / 列表展示·筛选·批量 / 对外 sale 过滤 — **已交付**。  
> **小程序**：sale / recycle / info 三端列表筛选三标记（默认不限）+ 详情属性区标记行（无标记则隐藏）；列表不展示标签；在售请求带 `includeHidden=1`。  
> **本版不做**：反查「整装下品鉴款」接口、核心品专题筛、标记字段 Redis 热读、小程序列表标签。

## 1. 规则

| 标记 | 字段 | 默认 | 说明 |
|------|------|------|------|
| 对外隐藏 | `hide_external` | 0 | 即使 SKU 上架，**对外**入口查不到 |
| 核心产品 | `core_flag` | 0 | 价格透明 / 公众号文章推广等 |
| 品鉴款 | `tasting_flag` | 0 | 区分品鉴款 |
| 整装 SPU | `pack_spu_id` | NULL | 可空；品鉴款指向整装 SPU，便于反查 |

- 三标记 **彼此独立**，可组合。  
- **不与**上下架 `status`、删除 `deleted`、非卖品 `non_sale` 混用。  
- 改 SPU 标记 → 同步该 SPU 下 **全部 SKU**（含已删）同名字段（含 `pack_spu_id`）。  
- 取消品鉴款（`tasting_flag`→0）→ **强制清空** `pack_spu_id`（SPU + 同步 SKU）。  
- `pack_spu_id` **不可指向自己**；目标须存在且未删。  
- **不做**「已被当作整装就不能再标品鉴」等反向互斥；选整装弹窗只列 **未勾品鉴** 的未删 SPU。

## 2. 对外 / 对内

| 入口 | 过滤 `hide_external=1` |
|------|------------------------|
| 对外在售 H5 / 公众号（对外 `sale`） | **是**（查不到） |
| 内部 H5（`inner-sale` / `inner-recycle`） | **否** |
| 内部小程序 | **否** |
| Admin | **否** |

核心产品 / 品鉴款：默认 **不挡**列表。  

`inner-sale` 与对外共用 `/chai/sku/sale/*`：内部页请求带 **`includeHidden=1`** 跳过过滤；不传 / `0` 则过滤（对外默认）。小程序走 `/chai/sku/info`、回收走 `/chai/sku/recycle`，均不加该过滤。

## 3. 数据模型

`chai_spu` / `chai_sku` 同增字段：

```text
hide_external  tinyint(2) NOT NULL DEFAULT 0  -- 0否 1对外隐藏
core_flag      tinyint(2) NOT NULL DEFAULT 0  -- 0否 1核心产品
tasting_flag   tinyint(2) NOT NULL DEFAULT 0  -- 0否 1品鉴款
pack_spu_id    bigint UNSIGNED NULL           -- 整装 SPU；品鉴可空
```

索引：`INDEX(pack_spu_id)`、`INDEX(hide_external)`（对外列表）。`core_flag` / `tasting_flag` 索引按需后续加。

全量：[`docs/db/SQL_CHAI_DOMAIN.sql`](../../db/SQL_CHAI_DOMAIN.sql)；增量见 CHANGELOG **1.5.0**。

## 4. Admin

### 编辑页

- 位置：在 **「实物鉴定图」下方、保存按钮上方**；小节标题 **「标记区」**（浅底分区，标题靠左）。  
- 三标记 **竖排**，右侧短说明；默认不勾。  
- **仅勾选品鉴款**时显示整装：按钮打开弹窗 `GET /business/chai/spu/packPick`（列表风格；名称 / 品牌 / 编码 / **条码**；排除当前与品鉴款；单选）。  
- 取消品鉴勾选：前端清空整装；保存时后端再强制清 `pack_spu_id`。

### 列表

- SPU 列表展示标记标签：对外隐藏 / 核心 / 品鉴（无则 `-`）。  
- **筛选**（SPU / SKU）：对外隐藏、核心产品、品鉴款，字典 `YES_OR_NO`。  
- **批量**：右上角 **「操作」**（菜单加宽）→ 设为/取消 对外隐藏、核心产品、品鉴款（`POST /business/chai/spu/marks/{field}/{0|1}`；**只改标记位**；**不**批量填整装）。  
- 批量取消品鉴 → 同步清空 `pack_spu_id`。

## 5. API

- 对外 `sale` 列表/详情 + 公众号关键词回复：默认 `hide_external = 0`（读 SKU 字段；`null` 视同可见）。  
- 内部价目 / 内部小程序在售：`includeHidden=1` 跳过对外隐藏过滤；`info` / `recycle` 本身不加该过滤。  
- `sale` / `info` / `recycle` 列表支持可选筛选 `hideExternal` / `coreFlag` / `tastingFlag`（`1` 是 / `0` 否 / 不传不限）；VO 回传三字段。  
- 主数据写路径：无单独 Redis 整表缓存要求；若后续有「核心品列表」热读再评估（见 [`CACHE.md`](../../coding/CACHE.md)）。

## 5.1 内部小程序

- **筛选**（在售 / 回收 / 信息）：对外隐藏、核心产品、品鉴款，各 **不限 / 是 / 否**，默认不限。  
- **详情**：属性区「标记」行置于等级之上；色标与 Admin 一致（隐藏橙 / 核心蓝 / 品鉴绿）；无任一标记则整行不展示。  
- **列表**：本版不展示标记标签。  
- 在售 list/detail 固定 `includeHidden=1`，对外隐藏 SKU 仍可见。

## 6. 发版注意（1.5.0）

1. 执行 CHANGELOG 中本能力相关 `ALTER`。  
2. 部署 admin / api。  
3. 按下方测试用例冒烟（可与条码用例同轮）。

## 7. 测试用例

| # | 场景 | 步骤要点 | 期望 |
|---|------|----------|------|
| 1 | 默认值 | 新建 SPU 不勾标记并保存 | 三标记为 0，`pack_spu_id` 空；下属 SKU 同步为 0 / 空 |
| 2 | 编辑区分隔 | 打开编辑页 | 「标记区」在实物鉴定图下方，竖排三项+右侧说明 |
| 3 | 品鉴 + 整装 | 勾品鉴 → 弹窗选整装（可按条码） | SPU/SKU：`tasting_flag=1`，`pack_spu_id=A`；列表「品鉴」标签 |
| 4 | 取消品鉴 | 去掉品鉴勾选并保存 | `tasting_flag=0`，`pack_spu_id` 强制空；SKU 同步 |
| 5 | 不能指自己 | 整装弹窗不含当前；若强行指自己保存 | 校验失败「整装SPU不能选择自己」 |
| 6 | 批量标记 | 列表多选 → 操作 → 设为对外隐藏 | 所选 SPU 及下属 SKU `hide_external=1`；列表「对外隐藏」标签 |
| 7 | 批量取消品鉴 | 操作 → 取消品鉴款 | `tasting_flag=0` 且 `pack_spu_id` 清空（含 SKU） |
| 8 | 对外隐藏 | 上架 + 对外隐藏 | 对外 `sale` / 公众号查不到；`inner-sale`（带 `includeHidden=1`）与小程序仍可见 |
| 9 | 列表筛选 | SPU/SKU 筛「对外隐藏=是」等 | 仅对应标记命中行 |
| 10 | 小程序筛选 | 在售/回收/信息筛「核心=是」等 | 列表命中对应标记；默认不限 |
| 11 | 小程序详情标记 | 打开有/无标记的 SKU 详情 | 有则等级上显示色标行；无则不出现该行；列表无标签 |
| 12 | （后续）反查品鉴 | 整装 A 下挂多个品鉴 | 本版不做；字段就绪即可 |
