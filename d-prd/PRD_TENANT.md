# EcoTea 多租户与三端规划（PRD）

> 文档名：`PRD_TENANT.md`  
> 状态：**规划定稿；Admin B1/B2/C + API D 已落地** — 产品与模型约定；DDL 见 [`d-db/SQL_TENANT.sql`](../d-db/SQL_TENANT.sql)（**仅组织表**）。  
> 配套：`PRD_WX_MINI.md`（小程序）、`PRD_WX_MP.md`（订阅号 H5）、[`d-docs/CODING_ADMIN.md`](../d-docs/CODING_ADMIN.md) / [`d-docs/CODING_API.md`](../d-docs/CODING_API.md)（编码边界）；小程序 UI 见 [`d-ui/UI_SPEC_WX_MINI_INNER.md`](../d-ui/UI_SPEC_WX_MINI_INNER.md)。

---

## 1. 目标

在现有单空间能力上引入**店铺租户**与**员工账号**，使：

- **SPU / SKU 与售价、回收价**全平台共享、统一价；
- **平台**负责开店、员工名额、首任店长与 Admin 数据范围；
- **小程序**为店员作业主入口；**店长可在本店建员工**（受配额限制）；
- **店铺 Admin**可登录只读（可绑多店）；
- **订阅号 + H5**继续对外获客。

**当前优先：** 组织账号（§6 A～D）已落地；小程序登录/选店骨架已起，下一阶段按 `PRD_WX_MINI.md` 做价目、开单篮、报价单与销售/回收单。  
**库存：** 现网 Admin 库存模块**代码与表结构暂不动**（销售/回收/盘点仍混在出入库事由里）；按店拆分与重构放到库存专题，再与小程序库存一起做。

**业务单：** 报价单 / 销售单 / 回收单为**新产品能力**，需**新建业务表**（DDL 另立，**不**写入 `SQL_TENANT.sql`）；产品规则见 `PRD_WX_MINI.md`。

---

## 2. 已锁定定调

| 项 | 定调 |
|----|------|
| 售价 / 回收价 | **全平台统一价**；本期不做店级覆盖 |
| SPU / SKU | **全平台共享** |
| 租户形态 | **扁平**店铺租户；不设特殊 COMPANY |
| 租户业务键 | 自增 `id` 忽略；业务用手填 **`tenant.tenant_id`**（唯一、勿改）；**无** `tenant_code` |
| 租户省/市 | **`province` / `city` 必填**（PCA）；区县、详细地址可选 |
| 负责人 | **`contact_name` / `contact_mobile`** 档案字段（可选填）；**不是**登录账号，不替代 `employee` |
| 员工名额 | **`employee_quota`**，仅平台可改 |
| 员工建号 | 先建 `employee` 再账密登录；微信为绑定后置 |
| 谁可建员工 | 平台任意店；店长本店 + `SHOP`（店铺管理）；店铺 Admin 不建号 |
| Admin 数据范围 | **`admin_tenant_scope`** 一人多店；`0`=平台 |
| 小程序权限 | `perm_code` **代码枚举**，不落库字典；**粗粒度**按工作台分组，可后调 |
| **库存（当前）** | **不加 `tenant_id`、不改库存代码**；等小程序库存/表重构专题 |
| 报价 / 销售 / 回收业务单 | 产品已定（见 `PRD_WX_MINI.md`）；**新建表**另立 DDL，不在组织脚本 |
| 订阅号绑定字段 | 本期不加 `wx_mp_app_id` |

---

## 3. 组织模型（当前阶段）

```
Platform（平台运营）
├── Shared catalog（chai_* 商品，不变）
├── tenant / employee / …（本阶段新建）
└── 库存 chai_*（现网不动，仍全局单空间使用）
```

### 3.1 本阶段表一览

| 表 | 说明 |
|----|------|
| `tenant` | 店铺；手填 `tenant_id`；配额；省/市必填；负责人档案 |
| `admin_tenant_scope` | `(user_id, tenant_id)`；一人多店；`0`=平台 |
| `employee` | 小程序员工 |
| `employee_tenant` | 员工所属店 |
| `employee_tenant_perm` | 店内权限码 |

DDL：[`d-db/SQL_TENANT.sql`](../d-db/SQL_TENANT.sql)。  
MySQL 中 `UNIQUE INDEX` 与 `UNIQUE KEY` **等价**；本项目与现有 `chai_*` 脚本统一用 `UNIQUE INDEX`。

**不用** `sys_dept` 冒充租户。

**后置（不在本脚本）：** `chai_warehouse` / `chai_stock` / `chai_stock_wh` / `chai_stock_bill` 的 `tenant_id` 与唯一键调整——待库存重构。

### 3.2 `tenant` 字段要点

| 字段 | 说明 |
|------|------|
| `id` | 自增，业务不用 |
| `tenant_id` | 手填、唯一；组织侧外键只认它 |
| `name` / `status` | 名称、启停 |
| `employee_quota` | 启用员工名额；仅平台可改 |
| `province` / `city` | **必填**（PCA）；应用层禁止空串 |
| `district` / `address` | 可选 |
| `contact_name` / `contact_mobile` | 负责人档案，可选；与店长 `employee` 可同人但不强绑 |

**名额语义**

| 值 | 含义 |
|----|------|
| `0` | 不可再挂启用员工到本店 |
| `>0` | 启用占用数须小于配额才允许新建/启用/挂店 |

```text
已占用 = COUNT(DISTINCT et.employee_id)
  FROM employee_tenant et
  JOIN employee e ON e.id = et.employee_id
 WHERE et.tenant_id = ? AND et.status = 1 AND e.status = 1
```

只改密码/perm 不占名额；停用或解绑本店释放名额。

### 3.3 员工建号与绑定

| 步骤 | 说明 |
|------|------|
| ① 建号 | `employee` + `employee_tenant` + perm；校验配额 |
| ② 登录 | 小程序账密 → 选店 |
| ③ 绑定（后置） | `wx_openid` 绑已有员工；不作自助注册 |

| 角色 | 建号范围 |
|------|----------|
| 平台 Admin | 任意店；改配额；首任店长 |
| 小程序店长 | 本店 + `SHOP`；不可改配额 |
| 店铺 Admin | 不建号 |

### 3.4 `admin_tenant_scope`

| `tenant_id` | 含义 |
|-------------|------|
| `0` | 平台（看各店；保存时清掉同用户其它店行） |
| `>0` | 可看该业务店；一人可多行 |

唯一键：`(user_id, tenant_id)`。建议超管显式 `(user_id, 0)`。

### 3.5 小程序权限（代码枚举，粗粒度）

> 与工作台分组对齐；细码（如「只看价不开单」）后置可再拆。新功能硬切，不做旧码兼容。

| `perm_code` | 工作台分组 | 含义 |
|-------------|------------|------|
| `SALE` | 销售 | 在售价目、销售报价单、销售单 |
| `RECYCLE` | 回收 | 回收价目、回收报价单、回收单 |
| `STOCK` | 库存 | 本店仓管 / 出入库（**暂缓**） |
| `FINANCE` | 财务 | 本店对账等（**暂缓**） |
| `SHOP` | 店铺管理 | 本店员工管理（受配额）等 |

「店长」可为勾选模板（含 `SHOP` 等），模板不落库。

### 3.6 `chai_staff`

过渡期 Admin 出入库仍用；目标由 `employee` 替代；**不在本阶段迁移**。

---

## 4. 身份与可见性

| 入口 | 账号 | 用途 |
|------|------|------|
| Admin | `sys_user` + 菜单 + `admin_tenant_scope` | 平台：开店/配额/员工；店铺：只读 |
| 小程序 | `employee` + 店 + perm | 查价；店长管员工；库存写后置 |

目标态库存写仅小程序、Admin 不改店库存——**等库存专题**；此前 Admin 库存模块保持现网行为。

---

## 5. 三端职责（与当前优先级对齐）

### 5.1 Admin（当前先做）

- 租户 CRUD（`tenant_id`、省/市必填、负责人、配额）  
- `admin_tenant_scope`  
- 员工 / 挂店 / perm / 首任店长  
- 共享主数据与统一价（已有）  
- 库存模块：**暂不改造**  

### 5.2 订阅号 + H5

照旧；多店库存展示等库存带租户后再做。见 `PRD_WX_MP.md`。

### 5.3 小程序（见 `PRD_WX_MINI.md`）

- 登录、选店、工作台入口墙（粗粒度 perm）  
- 在售 / 回收价目；销售文案、单商品存图；**不做**销售长图  
- 开单篮 → 报价单 → 转销售单 / 回收单（或直接开单）；**新建业务表**  
- 店长 `SHOP` 员工管理（配额）  
- 库存 `STOCK` / 财务 `FINANCE`：**暂缓**  

### 5.4 功能矩阵

| 能力 | 平台 Admin | 店铺 Admin | 小程序（近） | 更后 |
|------|------------|------------|--------------|------|
| 开店 / 配额 / scope / 首任店长 | 写 | — | — | — |
| 员工建号 | 任意店 | — | 本店 `SHOP` | — |
| 查价 / 文案 / 存图 | — | — | 读现网 API | 按店库存 |
| 报价单 / 销售单 / 回收单 | 只读汇总（后置） | 只读本店（后置） | 写本店（新表） | — |
| 仓管 / 出入库 | 现网不动 | 现网不动 | — | 重构后小程序写 |

---

## 6. 分步实现（当前）

| 步骤 | 内容 |
|------|------|
| **A** | 执行组织 DDL（[`SQL_TENANT.sql`](../d-db/SQL_TENANT.sql)，无库存 ALTER） |
| **B1** | Admin：`platform` 店铺租户 CRUD / 启停（手填 `tenant_id`、省/市、负责人、配额）— **已做代码** |
| **B2** | Admin：`admin_tenant_scope`（平台/多店）— **已做代码** |
| **C** | Admin：员工 + 挂店 + perm + 配额校验 + 首任店长— **已做代码** |
| **D** | API：employee 登录 / token / 选店— **已做代码**（`/employee/*`，Header `X-Employee-Token`） |
| **E** | 小程序（`wx/wx-mini-inner`）：登录/选店骨架已起；价目、开单篮、报价与业务单、员工管理按 `PRD_WX_MINI.md` 分批 |
| **F** | 业务单 DDL + API（报价单 / 销售单 / 回收单；**独立脚本**，不改 `SQL_TENANT.sql`） |

**库存专题（更后，可重构表结构后再加租户）：** 仓/结存/单据隔离、Admin 收口写、小程序 `STOCK`、业务单与纯库存拆分。

---

## 7. 明确不做 / 后置

- 当前组织阶段已完成；**仍后置：** 库存表加列与库存代码改动、`STOCK`/`FINANCE` 小程序能力  
- 业务单表：**要做**，但另立 DDL/专题，不塞进组织脚本  
- 加盟结算、在线收付、C 端商城、服务号、店级改价  
- 权限码落库字典、`tenant_code`、本期 `wx_mp_app_id`  
- 店员进 `sys_user`；负责人字段当登录账号  

---

## 8. 文档与脚本索引

| 路径 | 内容 |
|------|------|
| **本文** | 组织优先、库存后置 |
| [`d-db/SQL_TENANT.sql`](../d-db/SQL_TENANT.sql) | 仅 5 张组织表 |
| `PRD_WX_MINI.md` | 小程序产品 |
| `PRD_WX_MP.md` | 订阅号 + H5 |
| [`../d-docs/CODING_ADMIN.md`](../d-docs/CODING_ADMIN.md) / [`../d-docs/CODING_API.md`](../d-docs/CODING_API.md) | 编码边界 |
| [`../d-ui/UI_SPEC_WX_MINI_INNER.md`](../d-ui/UI_SPEC_WX_MINI_INNER.md) | 内部小程序 UI |
