# 业务 Redis 缓存规划

> 状态：第一期茶类主数据 **已全部接入**；组织域 **T1 tenant 热读 + T3 踢会话已接**；T2 `tenant_online` 可选未做；列表页 TTL 仍观望。  
> 编码约定（必须遵守）：`CODING_API.md` §8、`CODING_ADMIN.md` §3.1。  
> 基础设施：`DEPLOY_REDIS.md`、两端 `RedisUtils` / `LockUtils`、admin `RedisSeqUtils`（日序号）。  
> 原则：admin / api **共用同一 Redis**、同一前缀 `ecotea_`（`RedisConstant.KEY_PRE`）；key 用 `_` 分隔，不用 `:`。  
> WxJava token 等仍用 `wx:mp:`，不并入本规范。不管 tea 模块。  
> **后续新功能默认先评估缓存**（热读主数据走「api SET + admin DEL」；业务单据默认不缓存）。

## 目标

1. **主数据热读**：读多写少，**admin 改库成功后 DEL**，api / H5 miss 再加载。  
2. **组织域**：店铺按 `tenant_id` 热读；员工会话已接 Redis，Admin 改权限/状态后须踢会话。  
3. 销售 / 回收 **价目列表整页**、**报价/销售/回收业务单**：**默认不做**长缓存；有压测证据再局部短 TTL。

## 现状

| 已有 | 说明 |
|------|------|
| Redis 部署与连接 | 见 `DEPLOY_REDIS.md` |
| `RedisUtils` / `RedisTemplateHolder` | admin、api 对齐 |
| `LockUtils` | 分布式锁（NX） |
| `RedisSeqUtils` | 出入库等日序号 `ecotea_seq_{biz}_{yyyyMMdd}` |
| `sys_dict`（`ecotea_dict_{name}`） | **已接**：api `SysDictService.getByNameOk`；admin `DictUtil.clearCache` 双清 |
| `chai_brand`（`ecotea_chai_brand_online`） | **已接**：api `ChaiBrandService.listOnlineOrdered`；admin 写后 DEL |
| `chai_expiration`（`ecotea_chai_expiration_online`） | **已接**：api `ChaiExpirationService.listOnlineOrdered`；admin 写后 DEL |
| `chai_warehouse`（`ecotea_chai_warehouse_online`） | **已接**：api `ChaiWarehouseQueryService.listOnlineOrdered`；admin 写后 DEL |
| `wx_global_config`（`ecotea_wx_global_config_{type}`） | **已接**：api `WxGlobalConfigService.getByType`；admin 按 type DEL |
| 员工会话 `emp_session` / `emp_uid` | **已接**（登录态，非主数据缓存） |
| `tenant` 按 `tenant_id` | **已接**：api `TenantService.getByBizTenantId`；admin save 后 DEL |
| 改员工踢会话 | **已接**：admin `EmployeeSessionCacheService`（改密/状态/挂店 perm） |
| `tenant_online` | **未接**（可选） |
| 列表页 TTL / 业务单缓存 | **不做**（观望） |

## 职责

| 端 | 行为 |
|----|------|
| **admin** | 写库；主数据变更成功后 **DEL** 对应 key；后台分页 CRUD **直查库**（不强制走缓存） |
| **api** | 热读：Redis → miss → MySQL → SET（建议 `DAY_EXPIRE` 兜底） |
| **字典（必做）** | api：`DictUtils` / `SysDictService` **走 Redis**；admin 读继续 **EhCache**，改字典成功后同时 `DictUtil.clearCache` + **DEL** `ecotea_dict_{name}` |

## Key 约定

```text
# —— 第一期主数据（写后 DEL + DAY_EXPIRE）已接 ——
ecotea_dict_{name}                       # sys_dict 按 name
ecotea_chai_brand_online                 # 上架品牌列表
ecotea_chai_expiration_online            # 上架保质期列表
ecotea_chai_warehouse_online             # 上架仓库列表
ecotea_wx_global_config_{type}           # wx_global_config 按 type

# —— 员工会话（已接）——
ecotea_emp_session_{token}               # 会话 JSON，TTL 7 天
ecotea_emp_uid_{employeeId}              # → 当前 token（单点）

# —— 组织域（待接）——
ecotea_tenant_{tenantId}                 # 单店整行/启用摘要（业务键 tenant.tenant_id）
ecotea_tenant_online                     # 可选：启用店有序列表（选店补名/下拉）
```

- value：字符串；列表 / 配置用 JSON（api `JsonUtils`）。字典可缓存整行 `value` 原文（与现 `getByNameOk` 一致），命中后再在内存拆 `key=label`。  
- 空列表也可短 TTL 缓存，防穿透。  
- **禁止**生产用 `KEYS *` 扫删；批量失效用 SCAN 或版本号（本期单 key DEL 即可）。

## 第一期：主数据（写后 DEL）

### 1. sys_dict（必做）✅

- key：`ecotea_dict_{name}`，`name` 即字典标识（如 `CHAI_TYPE` / `CHAI_GRADE` / `CHAI_PROD_BATCH` 及规格单位 dict）。  
- api：`SysDictService.getByNameOk` Redis → miss → DB → SET（命中 `DAY_EXPIRE`；库无则 `"null"` + 5 分钟）；`DictUtils` / `ChaiDictController` / 价目补名 / `ChaiSpecUtil` 间接受益。  
- admin：读仍 EhCache；`DictUtil.clearCache` 同时清 EhCache + **DEL** Redis；`DictController` save / status、以及 `DictUtils.updateValue*` 写后走该入口。  
- 改字典少、展示面广，**必须立刻生效**（写后 DEL，勿依赖 TTL 自然过期才更新）。

### 2. chai_brand ✅

- 单 key `ecotea_chai_brand_online` = 上架且有序列表（与现 `listOnlineOrdered` 一致）；常量 `RedisConstant.CHAI_BRAND_ONLINE_KEY`。  
- api：`ChaiBrandService.listOnlineOrdered` Redis → miss → DB → SET（`DAY_EXPIRE`）；`resolveOnlineIdByNameExact` / `mapNamesByIds`（销售·回收价目）及 H5 品牌列表共用；补名时缓存未覆盖的 id 再查库。  
- admin：`ChaiBrandService` 在 `save` / 删除 / 拼音批量回填（有更新时）后 **DEL**（改状态走 `save`）。

### 3. chai_expiration ✅

- 单 key `ecotea_chai_expiration_online`；常量 `RedisConstant.CHAI_EXPIRATION_ONLINE_KEY`。  
- api：新建 `ChaiExpirationService.listOnlineOrdered` Redis → miss → DB → SET（`DAY_EXPIRE`，排序与 admin 一致）；`mapNamesByIds` 供销售/回收价目补名（缓存未覆盖再查库）。  
- admin：`ChaiExpirationService` 在 `save` / 删除后 **DEL**（改状态走 `save`）。

### 4. chai_warehouse ✅

- 单 key `ecotea_chai_warehouse_online`；常量 `RedisConstant.CHAI_WAREHOUSE_ONLINE_KEY`。  
- api：`ChaiWarehouseQueryService.listOnlineOrdered` Redis → miss → DB → SET（`DAY_EXPIRE`）；`listOnline`（仓下拉 VO）与销售/回收分仓展示 `mapByIds` 共用（缓存未覆盖再查库）。  
- admin：`ChaiWarehouseService` 在 `save` / 删除后 **DEL**（改状态走 `save`）。

### 5. wx_global_config ✅

- key 模板：`ecotea_wx_global_config_%s`（`RedisConstant.WX_GLOBAL_CONFIG_KEY`），value = 整行序列化。  
- type：`SUBSCRIBE_WELCOME` / `RECYCLE_DESC` / `CUSTOMER_SERVICE` / `SALE_H5_COPY`。  
- api：`getByType` Redis → miss → DB → SET（命中 `DAY_EXPIRE`；库无 `"null"` + 5 分钟）；欢迎语 / 客服 / H5 文案 / 回收说明均经此入口。  
- admin：`save` / `delete` 按 **type DEL**（改动少，**必须立刻生效**）。  
- **不缓存** `wx_mp_menu`（发布低频，直查库）。

## 组织域（tenant / 员工会话失效）

> 产品见 `PRD_TENANT.md`；编码见 `CODING_API.md` §8.4、`CODING_ADMIN.md` §3.1。

### T1. `tenant` 按业务键热读 ✅

| 项 | 约定 |
|----|------|
| key | `ecotea_tenant_{tenantId}`；常量 `RedisConstant.TENANT_KEY` |
| value | 整行 JSON；库无 → `"null"` + 5 分钟 |
| api | `TenantService.getByBizTenantId` / `mapByBizTenantIds` |
| admin | `TenantService.save`（含启停）后 `clearTenantCache` |
| 用途 | 登录选店校验、会话补店名、后续按店隔离读 |

### T2. 启用店列表（可选）⬜

| 项 | 约定 |
|----|------|
| key | `ecotea_tenant_online` |
| value | 启用店铺有序摘要列表 JSON（`tenantId` + `name` + `status` 等） |
| api | 需要「全量启用店」读路径时再接；选店列表若以员工挂店为准，**可不依赖**本 key |
| admin | 任意店铺 save / 启停后 **DEL** |

### T3. Admin 改员工 → 踢小程序会话 ✅

| 触发（Admin） | 失效动作 |
|---------------|----------|
| 改密码、改员工状态 | `EmployeeSessionCacheService.invalidateByEmployeeId` |
| 改挂店、改 perm（`replaceBinds`） | 同上 |
| 店铺停用 | 不扫全站会话；依赖选店/业务接口校验店状态 + T1 DEL |

实现：admin 与 api 共用 key 常量（各自 `RedisConstant`），admin 直接 `RedisUtils.delete`，**无**共用业务 jar。

### 组织域明确不做

- 不缓存 `employee` 全表 / 分页  
- 不缓存 `employee_tenant` / `employee_tenant_perm` 明细长 key（以会话内 perm + T3 踢会话为准）  
- 不缓存 `admin_tenant_scope`（Admin 后台直查库）

## 第二期（可选）：列表页 TTL

> **默认不做。** 下列说明仅供日后有压测/慢查询证据时参考。

销售 / 回收列表已支持多维筛选（`brandIds`、`types`、价区、`barcode`、`whId`、`recycleRecent` 等），整页 VO 缓存易 **key 爆炸、命中率低**。

若要做，约束建议：

| 建议 | 说明 |
|------|------|
| 仅缓存少数 shape | 如「无筛选默认首页」「纯品牌精确」「纯 spuId」；带多选/价区/`barcode` 的 **不缓存** |
| 仅 TTL | 库存/SKU 变更 **不** 精确 DEL；接受最多一个 TTL 的展示延迟 |
| 短 TTL | 默认首页 30～60s；品牌/同款 120s 量级 |
| 不做 | 模糊 keyword 长 TTL；库存单独 Hash；`KEYS` 扫删销售前缀 |

扫码精确查（`barcode=`）：单次唯一、即时，**不适合**列表页缓存。

关键词查价若与 H5 共用同一 shape，可共享同一页 key；「客服」等词只走 `wx_global_config`，不走销售 key。

## 明确不做

- tea_* 相关缓存  
- 生产 `KEYS *` 扫删  
- 以库存 Hash 为主方案  
- 单 SKU/SPU 详情缓存（写路径多，收益不如主数据）  
- 把日序号 / 分布式锁再包装成「业务缓存」（已是基础设施）  
- 报价单 / 销售单 / 回收单整单或列表长缓存（默认直查库）

## 实现顺序

1. ~~**sys_dict（必做）**~~ ✅ api 热读 + admin 写后双清（EhCache + Redis DEL）  
2. ~~**chai_brand**~~ ✅；~~**chai_expiration**~~ ✅  
3. ~~**chai_warehouse**~~ ✅  
4. ~~**wx_global_config**~~ ✅  
5. ~~**组织域 T1 + T3**~~ ✅ `tenant` 热读 + Admin 改员工踢会话  
6. **组织域 T2**：`tenant_online`（可选）⬜  
7. 列表页 TTL / 业务单缓存：**观望**，有证据再局部加  

## 验收要点（第一期主数据）

1. admin 改字典（如茶类 `CHAI_TYPE`、等级、生产批次）→ H5 筛选项 / 价目展示名 **立刻**新（写后 DEL）。  
2. admin 改品牌名 / 上下架 → H5 筛选与标题/搜品牌 **立刻**新。  
3. admin 改保质期名 → 价目展示同步。  
4. admin 改仓库 → 内部价目仓列表同步。  
5. admin 改欢迎语 / 客服 / 回收 / 销售文案 → 真机或 H5 **立刻**新（写后 DEL）。  
6. Redis miss 时行为与现直查库一致；DEL 后下一次请求应重新 SET。

## 验收要点（组织域，接入后）

1. admin 改店名 / 启停 → api 读 `tenant` **立刻**新（T1 DEL）。  
2. api 连续读同一 `tenant_id` 应命中 Redis（可看 key / 日志）。  
3. admin 改员工 perm 或停用 → 该员工小程序旧 token **失效**，须重新登录（T3）。

## 与旧版规划差异

| 旧 | 现 |
|----|----|
| 销售分页（brand/spu）优先实现 | **下调**；默认不做整页缓存 |
| 无仓库缓存 | **补** `chai_warehouse_online` |
| dict 可选、排在末尾 | **`sys_dict` 必做**，实现顺序 **最先**（茶类等展示依赖） |
| 实现顺序 brand → warehouse → wx → dict | 改为 **dict → brand/expiration → warehouse → wx → 列表观望** |
| 业务缓存「仅规划」 | 明确：基础设施已有，业务层未接 |
