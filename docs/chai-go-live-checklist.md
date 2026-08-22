# chai 域上线配置检查清单

> 新茶叶商品主数据（表前缀 `chai_*`），与旧 `tea`（报价等）并行，互不替换。  
> 用途：环境上线 / 联调前按本清单逐项配置与验收。

---

## 总览

| 序号 | 类别 | 说明 |
|------|------|------|
| 1 | 数据库表 | `chai_brand` / `chai_expiration` / `chai_spu` / `chai_sku` |
| 1b | 库存主数据表 | `chai_staff` / `chai_warehouse` |
| 1c | 库存结存与单据 | `chai_stock` / `chai_stock_wh` / `chai_stock_bill` / `chai_stock_bill_item` |
| 2 | 字典 | 新建 5 个商品字典 + 3 个库存字典 + 复用 `STAR_LEVEL` |
| 3 | 菜单与权限 | 商品目录 + 库存主数据 + 结存 + 单据 |
| 4 | 角色授权 | 勾选后重新登录 |
| 5 | 冒烟验收 | 品牌 → 保质期 → SPU → SKU；经手人 → 仓库 → 入库 → 结存 → 出库/调拨/作废/归档 |

---

## 1. 数据库表

- [ ] 已执行建表 SQL（以业务最终定稿为准）
- [ ] `chai_brand` — 品牌；`name` 唯一
- [ ] `chai_expiration` — 保质期；`months=0` 表示长期
- [ ] `chai_spu` — SPU；编码运行时生成 `CHAI-%08d`（spuId）
- [ ] `chai_sku` — SKU；编码 `CHAI-%08d-%08d`（spuId-skuId）；含 `UNIQUE(spu_id, year, prod_batch)`、`KEY(spu_id)`
- [ ] `chai_spu` / `chai_sku` 已加 `deleted`（软删，默认 0）
- [ ] 旧表 `tea_sku` 已加 `sync_flag`（tea→chai 同步标记）

```sql
ALTER TABLE `tea`.`tea_sku`
ADD COLUMN `sync_flag` tinyint(2) NOT NULL DEFAULT 0 COMMENT '是否已同步' AFTER `status`;

ALTER TABLE `chai_spu`
  ADD COLUMN `deleted` tinyint(2) NOT NULL DEFAULT 0 COMMENT '0有效 1已删除' AFTER `status`,
  ADD KEY `idx_deleted` (`deleted`);

ALTER TABLE `chai_sku`
  ADD COLUMN `deleted` tinyint(2) NOT NULL DEFAULT 0 COMMENT '0有效 1已删除' AFTER `status`,
  ADD KEY `idx_deleted` (`deleted`);
```

说明：品牌 / 保质期走业务表 id，不走字典。

### 1b. 库存主数据表（已落地 CRUD）

- [ ] `chai_staff` — 经手人；`nick_name` 唯一；展示/快照格式 `真实姓名(花名)`
- [ ] `chai_warehouse` — 仓库；`name` 唯一；省市区 PCA 可空（选则须选全）
- [ ] 被库存单据 / 分仓结存引用时禁止物理删（有单据后再验收引用拦截）
- [ ] `chai_stock` / `chai_stock_wh` — 结存表已建；`chai_stock_wh.version` 乐观锁；列表只读 JOIN SKU
- [ ] 库存列表菜单：`/business/chai/stock/index`，权限 `business:chai:stock:index`

### 1c. 库存单据表

- [ ] `chai_stock_bill` — 入/出/调头表；`bill_no` 唯一；`status`：1已过账 / 2作废 / 3归档
- [ ] `chai_stock_bill_item` — 明细；过账时写 `sku_snap`（整份 SKU JSON）；含行备注 `remark`
- [ ] 单据号日序：`DailySeqType` 10/11/12 → `CHAI-SBRK/SBCK/SBDB-{yyMMdd}-{4位}`
- [ ] 有 `total_qty > 0` 的 SKU/SPU 禁止软删（向导删卡片 / 删 SPU）

示例 DDL（以业务定稿为准）：

```sql
CREATE TABLE `chai_stock_bill` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `bill_no` varchar(32) NOT NULL,
  `bill_type` tinyint(2) NOT NULL COMMENT '1入库 2出库 3调拨',
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '1已过账 2作废 3归档',
  `reason` int(11) NOT NULL,
  `handler_id` bigint(20) unsigned NOT NULL DEFAULT '0',
  `handler_name` varchar(255) NOT NULL DEFAULT '',
  `from_wh_id` bigint(20) unsigned NOT NULL,
  `to_wh_id` bigint(20) unsigned NOT NULL DEFAULT '0',
  `total_qty` int(11) NOT NULL DEFAULT '0',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `remark` varchar(500) NOT NULL DEFAULT '',
  `operator` varchar(255) NOT NULL DEFAULT '',
  `update_time` bigint(20) NOT NULL,
  `create_time` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_bill_no` (`bill_no`),
  KEY `idx_type_status` (`bill_type`,`status`),
  KEY `idx_from_wh` (`from_wh_id`),
  KEY `idx_handler` (`handler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='茶叶-库存单据';

CREATE TABLE `chai_stock_bill_item` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `bill_id` bigint(20) unsigned NOT NULL,
  `sku_id` bigint(20) unsigned NOT NULL,
  `sku_code` varchar(32) NOT NULL DEFAULT '',
  `name` varchar(255) NOT NULL DEFAULT '',
  `qty` int(11) NOT NULL,
  `price` decimal(12,2) NOT NULL DEFAULT '0.00',
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '行备注：破损/无提袋等',
  `sku_snap` text,
  PRIMARY KEY (`id`),
  KEY `idx_bill` (`bill_id`),
  KEY `idx_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='茶叶-库存单据明细';
```

若表已建、缺行备注：

```sql
ALTER TABLE `chai_stock_bill_item`
  ADD COLUMN `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '行备注：破损/无提袋等' AFTER `amount`;
```

---

## 2. 字典（sys_dict）

TIMO 格式：`name` = 标识，`value` = `码:文案,码:文案,...`

### 2.1 必须新建

| 勾选 | 字典标识 | 建议 value | 用途 | 码值约束 |
|------|----------|------------|------|----------|
| [ ] | `CHAI_STATUS` | `0:下架,1:上架` | 品牌/保质期/SPU/SKU 状态 | **必须** `0`/`1`，与 `ChaiStatus` 一致 |
| [ ] | `CHAI_PROD_BATCH` | `100:上半年,200:下半年` | SPU/SKU 生产批次 | **必须** `100`/`200`，半年推算依赖 |
| [ ] | `CHAI_TYPE` | 按业务自定，例：`1:红茶,2:绿茶,...` | 茶类 | Integer 码 |
| [ ] | `CHAI_GRADE` | 按业务自定，例：`0:普通,1:特级,...` | 等级 | Integer 码；新建 SPU 默认 `0` |
| [ ] | `CHAI_SPEC_LABEL` | 按业务自定，例：`1:泡,2:袋,...` | 规格单位 | Integer 码，写入 `spec.unit_label` |
| [ ] | `CHAI_STOCK_BILL_TYPE` | `1:入库,2:出库,3:调拨` | 单据类型 | 与 `ChaiStockBillType` 一致 |
| [ ] | `CHAI_STOCK_BILL_STATUS` | `1:已过账,2:作废,3:归档` | 单据状态 | 与 `ChaiStockBillStatus` 一致 |
| [ ] | `CHAI_STOCK_REASON` | 见下 | 事由 | 入100–199 / 出200–299 / 调300–399 |

`CHAI_STOCK_REASON` 建议 value：

```text
100:回收,101:退货,102:盘盈,198:盘点(首次),199:其它入库,200:销售,201:送礼,202:自用,203:报损,299:其它出库,300:调度
```

期初入库建议事由 **198 盘点(首次)**。

### 2.2 复用已有

| 勾选 | 字典标识 | 说明 |
|------|----------|------|
| [ ] | `STAR_LEVEL` | 星级；与旧 `tea` 共用，确认环境已有 |

### 2.3 注意

- `CHAI_TYPE` / `CHAI_GRADE` / `CHAI_SPEC_LABEL` 文案可按业务改。
- **`CHAI_STATUS`、`CHAI_PROD_BATCH` 码值不要改**，否则上下架与半年推算会错。

---

## 3. 菜单与权限（sys_menu）

结构：**1 个目录 + 4 个列表菜单 + 各资源按钮**。  
`pid` 按环境填写；列表 URL **必须带 `/index`**。

### 3.1 目录

| 勾选 | 标题 | URL | 权限标识 | 类型 |
|------|------|-----|----------|------|
| [ ] | 茶叶商品（chai） | — | — | 目录 |

### 3.2 列表菜单

| 勾选 | 标题 | URL | 权限标识 | 类型 |
|------|------|-----|----------|------|
| [ ] | 茶叶品牌 | `/business/chai/brand/index` | `business:chai:brand:index` | 菜单 |
| [ ] | 茶叶保质期 | `/business/chai/expiration/index` | `business:chai:expiration:index` | 菜单 |
| [ ] | 茶叶SPU | `/business/chai/spu/index` | `business:chai:spu:index` | 菜单 |
| [ ] | 茶叶SKU | `/business/chai/sku/index` | `business:chai:sku:index` | 菜单 |
| [ ] | 茶叶经手人 | `/business/chai/staff/index` | `business:chai:staff:index` | 菜单 |
| [ ] | 茶叶仓库 | `/business/chai/warehouse/index` | `business:chai:warehouse:index` | 菜单 |
| [ ] | 茶叶库存 | `/business/chai/stock/index` | `business:chai:stock:index` | 菜单 |
| [ ] | 茶叶库存单据 | `/business/chai/stockBill/index` | `business:chai:stockBill:index` | 菜单 |

### 3.3 按钮权限

| 勾选 | 父菜单 | 标题 | 权限标识 |
|------|--------|------|----------|
| [ ] | 茶叶品牌 | 添加/编辑/上下架 | `business:chai:brand:edit` |
| [ ] | 茶叶品牌 | 删除 | `business:chai:brand:delete` |
| [ ] | 茶叶保质期 | 添加/编辑/上下架 | `business:chai:expiration:edit` |
| [ ] | 茶叶保质期 | 删除 | `business:chai:expiration:delete` |
| [ ] | 茶叶SPU | 添加/编辑/上下架 | `business:chai:spu:edit` |
| [ ] | 茶叶SPU | 删除 | `business:chai:spu:delete` |
| [ ] | 茶叶SKU | 维护SKU（向导） | `business:chai:sku:edit` |
| [ ] | 茶叶经手人 | 添加/编辑/上下架 | `business:chai:staff:edit` |
| [ ] | 茶叶经手人 | 删除 | `business:chai:staff:delete` |
| [ ] | 茶叶仓库 | 添加/编辑/上下架 | `business:chai:warehouse:edit` |
| [ ] | 茶叶仓库 | 删除 | `business:chai:warehouse:delete` |
| [ ] | 茶叶库存单据 | 新建/过账/作废/归档 | `business:chai:stockBill:edit` |

不要再配 `business:chai:sku:delete`。SKU 不在列表删除；不需要的半年在 SPU 列表点「SKU」进入向导后删卡片保存。若环境已配该按钮，删掉即可。  
`sku:edit` 的入口在 **SPU 列表「SKU」** 和 SPU 编辑页「下一步」，不在 SKU 列表。

### 3.4 权限用途速查

| 权限标识 | 主要用途 |
|----------|----------|
| `business:chai:brand:index` | 品牌列表、打开编辑页 |
| `business:chai:brand:edit` | 保存、上下架 |
| `business:chai:brand:delete` | 删除 |
| `business:chai:expiration:index` | 保质期列表、打开编辑页 |
| `business:chai:expiration:edit` | 保存、上下架 |
| `business:chai:expiration:delete` | 删除 |
| `business:chai:spu:index` | SPU 列表、打开编辑页；SKU 列表「SPU」也会打开编辑页 |
| `business:chai:spu:edit` | 保存 SPU、上下架（级联全部 SKU）；无 SKU 不可上架 |
| `business:chai:spu:delete` | 删除 SPU（级联删 SKU） |
| `business:chai:sku:index` | 独立 SKU 列表（只读浏览 +「SPU」「同SPU」） |
| `business:chai:sku:edit` | SKU 向导、批量保存；SPU 页「SKU / 下一步」依赖此权限 |
| `business:chai:staff:index` | 经手人列表、打开编辑页 |
| `business:chai:staff:edit` | 保存、上下架 |
| `business:chai:staff:delete` | 删除（被单据引用则拦截） |
| `business:chai:warehouse:index` | 仓库列表、打开编辑页 |
| `business:chai:warehouse:edit` | 保存、上下架 |
| `business:chai:warehouse:delete` | 删除（被分仓/单据引用则拦截） |
| `business:chai:stock:index` | 库存结存列表、分仓弹窗（只读） |
| `business:chai:stockBill:index` | 库存单据列表、详情 |
| `business:chai:stockBill:edit` | 新建并过账、作废、归档、选品搜索 |

---

## 4. 角色授权

- [ ] 业务角色已勾选上述菜单与按钮权限
- [ ] 相关账号重新登录（或清 Shiro 缓存）后侧边栏可见「茶叶商品（chai）」
- [ ] 无权限账号无法打开对应 URL（抽查即可）

---

## 5. 冒烟验收

按顺序打勾：

- [ ] 品牌：新增 / 编辑 / 上下架 / 列表排序（大到小）
- [ ] 保质期：新增（含 `months=0` 长期）/ 编辑 / 上下架
- [ ] SPU：仅保存 → 无 SKU、状态为下架；列表可见
- [ ] SPU：「下一步（维护SKU）」→ 无 SKU 时预填 6 个半年；填价后保存
- [ ] SPU 列表「SKU」可打开同一向导；已有 SKU 时加载已有、不重新生成
- [ ] SKU 独立列表：筛选、「SPU」「同SPU」（无列表上下架/删除/维护）
- [ ] 不需要的半年：从 SPU 列表进向导，删除对应卡片后保存
- [ ] SPU 上下架会同步其下全部 SKU；无 SKU 时不可上架
- [ ] 维护页保存后若 SPU 仍下架，可选择是否同时上架
- [ ] 删除 SPU 时级联删除其下 SKU
- [ ] 旧 `tea` 菜单与功能不受影响
- [ ] 旧茶叶SKU列表：「同步」打开预填页；规格/生产批次手填；提交后新建下架 SPU + 6 个半年 SKU
- [ ] 同步成功后该行变为「已同步」，不可再点同步；「标未同步」后可再同步（会再新建一套）
- [ ] 行内/批量「标已同步」「标未同步」只改标记，不创建 chai
- [ ] 经手人：新增（花名唯一）/ 编辑 / 上下架 / 列表排序
- [ ] 仓库：新增（名称唯一、省市区可选）/ 编辑 / 上下架
- [ ] 库存列表：空表可打开；筛选（含仓库、有库存）；「分仓」弹窗（无分仓时提示）
- [ ] 入库：事由 198、选仓、加 SKU、保存即过账；结存列表出现数量
- [ ] 出库 / 调拨：数量不足时失败；成功后合计/分仓正确
- [ ] 作废：仅已过账可作废；冲回数量；不可再作废/归档
- [ ] 归档：仅已过账可归档；不冲数量；不可再作废/归档
- [ ] 有库存的 SKU（向导删卡片）/ SPU 删除被拦截

---

## 6. 关键业务规则（验收参考）

| 规则 | 说明 |
|------|------|
| 仅存 SPU | 不写 SKU；无 SKU 时 SPU 只能下架 |
| SKU 状态 | 跟随 SPU 总开关；列表不提供 SKU 上下架/删除 |
| SKU 唯一 | 同 SPU 下 `(year, prod_batch)` 唯一 |
| 半年默认 | 以 SPU 的 year + prod_batch 为锚点，向前推 6 期（含锚点） |
| 价格 | SKU 销售价/回收价/压价%/无提袋扣减提交时必填（官价可空） |
| 编码 | SPU：`CHAI-{spuId}`；SKU：`CHAI-{spuId}-{skuId}`；保存后生成，勿手填 |
| 结存创建 | 仅入库过账时按需建 `chai_stock` / `chai_stock_wh` |
| 单据状态机 | 保存=已过账；已过账→作废(冲)或归档(不冲)；2/3 终态 |
| 仓位 | 入/出：`from_wh_id≠0`，`to_wh_id=0`；调拨两边都≠0 |
| 软删拦截 | `total_qty > 0` 禁止软删对应 SKU / SPU |
