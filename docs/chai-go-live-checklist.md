# chai 域上线配置检查清单

> 新茶叶商品主数据（表前缀 `chai_*`），与旧 `tea`（报价等）并行，互不替换。  
> 用途：环境上线 / 联调前按本清单逐项配置与验收。

---

## 总览

| 序号 | 类别 | 说明 |
|------|------|------|
| 1 | 数据库表 | `chai_brand` / `chai_expiration` / `chai_spu` / `chai_sku` |
| 2 | 字典 | 新建 5 个 + 复用 `STAR_LEVEL` |
| 3 | 菜单与权限 | 1 目录 + 4 菜单 + 按钮权限 |
| 4 | 角色授权 | 勾选后重新登录 |
| 5 | 冒烟验收 | 品牌 → 保质期 → SPU → SKU |

---

## 1. 数据库表

- [ ] 已执行建表 SQL（以业务最终定稿为准）
- [ ] `chai_brand` — 品牌；`name` 唯一
- [ ] `chai_expiration` — 保质期；`months=0` 表示长期
- [ ] `chai_spu` — SPU；编码运行时生成 `CHAI-%08d`（spuId）
- [ ] `chai_sku` — SKU；编码 `CHAI-%08d-%08d`（spuId-skuId）；含 `UNIQUE(spu_id, year, prod_batch)`、`KEY(spu_id)`

说明：品牌 / 保质期走业务表 id，不走字典。

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

### 3.3 按钮权限

| 勾选 | 父菜单 | 标题 | 权限标识 |
|------|--------|------|----------|
| [ ] | 茶叶品牌 | 添加/编辑/上下架 | `business:chai:brand:edit` |
| [ ] | 茶叶品牌 | 删除 | `business:chai:brand:delete` |
| [ ] | 茶叶保质期 | 添加/编辑/上下架 | `business:chai:expiration:edit` |
| [ ] | 茶叶保质期 | 删除 | `business:chai:expiration:delete` |
| [ ] | 茶叶SPU | 添加/编辑/上下架 | `business:chai:spu:edit` |
| [ ] | 茶叶SPU | 删除 | `business:chai:spu:delete` |
| [ ] | 茶叶SKU | 维护 | `business:chai:sku:edit` |

不要再配 `business:chai:sku:delete`。SKU 不在列表删除；不需要的半年在维护页删卡片后保存。若环境已配该按钮，删掉即可。

### 3.4 权限用途速查

| 权限标识 | 主要用途 |
|----------|----------|
| `business:chai:brand:index` | 品牌列表、打开编辑页 |
| `business:chai:brand:edit` | 保存、上下架 |
| `business:chai:brand:delete` | 删除 |
| `business:chai:expiration:index` | 保质期列表、打开编辑页 |
| `business:chai:expiration:edit` | 保存、上下架 |
| `business:chai:expiration:delete` | 删除 |
| `business:chai:spu:index` | SPU 列表、打开编辑页 |
| `business:chai:spu:edit` | 保存 SPU、上下架（级联全部 SKU）；无 SKU 不可上架 |
| `business:chai:spu:delete` | 删除 SPU（级联删 SKU） |
| `business:chai:sku:index` | 独立 SKU 列表（只读浏览 +「维护」入口） |
| `business:chai:sku:edit` | SKU 向导、批量保存；SPU 页「SKU / 下一步」也依赖此权限 |

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
- [ ] SKU 独立列表：筛选、「维护」进向导（无列表上下架/删除）
- [ ] 不需要的半年：维护页删除对应卡片后保存
- [ ] SPU 上下架会同步其下全部 SKU；无 SKU 时不可上架
- [ ] 维护页保存后若 SPU 仍下架，可选择是否同时上架
- [ ] 删除 SPU 时级联删除其下 SKU
- [ ] 旧 `tea` 菜单与功能不受影响

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
