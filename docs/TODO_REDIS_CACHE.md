# 业务 Redis 缓存规划

> 状态：**规划已按当前业务重排**；第一期主数据（`sys_dict` / `chai_brand` / `chai_expiration` / `chai_warehouse` / `wx_global_config`）**已全部接入**；列表页 TTL 仍观望。  
> 基础设施已就绪：`DEPLOY_REDIS.md`、两端 `RedisUtils` / `LockUtils`、admin `RedisSeqUtils`（日序号）。  
> 原则：admin / api **共用同一 Redis**、同一前缀 `ecotea_`（`RedisConstant.KEY_PRE`）；key 用 `_` 分隔，不用 `:`。  
> WxJava token 等仍用 `wx:mp:`，不并入本规范。不管 tea 模块。

## 目标（本期）

优先做 **主数据热读缓存**：读多写少，**admin 改库成功后 DEL 对应 key**，api / H5 下次 miss 再加载。  
其中 **`sys_dict` 必做**：茶类、等级、生产批次、规格单位等展示与筛选项都依赖字典，api 价目/H5 已在直查库，必须先接 Redis。  
销售 / 回收 **列表整页缓存默认不做**（筛选维度已膨胀，key 组合多、命中差）；确有性能证据再局部加。

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
| 列表页 TTL | **未接**（观望） |

## 职责

| 端 | 行为 |
|----|------|
| **admin** | 写库；主数据变更成功后 **DEL** 对应 key；后台分页 CRUD **直查库**（不强制走缓存） |
| **api** | 热读：Redis → miss → MySQL → SET（建议 `DAY_EXPIRE` 兜底） |
| **字典（必做）** | api：`DictUtils` / `SysDictService` **走 Redis**；admin 读继续 **EhCache**，改字典成功后同时 `DictUtil.clearCache` + **DEL** `ecotea_dict_{name}` |

## Key 约定（第一期：主数据）

```text
# 写后 DEL + DAY_EXPIRE 兜底
ecotea_dict_{name}                       # sys_dict 按 name（必做；茶类/等级/批次等）
ecotea_chai_brand_online                 # 上架品牌列表（有序，~百级）
ecotea_chai_expiration_online            # 上架保质期列表（~二十）
ecotea_chai_warehouse_online             # 上架仓库列表（内部价目筛仓）
ecotea_wx_global_config_{type}           # wx_global_config 按 type（见 WxGlobalConfigType）
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

## 实现顺序

1. ~~**sys_dict（必做）**~~ ✅ api 热读 + admin 写后双清（EhCache + Redis DEL）  
2. ~~**chai_brand**~~ ✅；~~**chai_expiration**~~ ✅  
3. ~~**chai_warehouse**~~ ✅  
4. ~~**wx_global_config**~~ ✅  
5. 列表页 TTL：**观望**，有证据再局部加  

## 验收要点（第一期）

1. admin 改字典（如茶类 `CHAI_TYPE`、等级、生产批次）→ H5 筛选项 / 价目展示名 **立刻**新（写后 DEL）。  
2. admin 改品牌名 / 上下架 → H5 筛选与标题/搜品牌 **立刻**新。  
3. admin 改保质期名 → 价目展示同步。  
4. admin 改仓库 → 内部价目仓列表同步。  
5. admin 改欢迎语 / 客服 / 回收 / 销售文案 → 真机或 H5 **立刻**新（写后 DEL）。  
6. Redis miss 时行为与现直查库一致；DEL 后下一次请求应重新 SET。

## 与旧版规划差异

| 旧 | 现 |
|----|----|
| 销售分页（brand/spu）优先实现 | **下调**；默认不做整页缓存 |
| 无仓库缓存 | **补** `chai_warehouse_online` |
| dict 可选、排在末尾 | **`sys_dict` 必做**，实现顺序 **最先**（茶类等展示依赖） |
| 实现顺序 brand → warehouse → wx → dict | 改为 **dict → brand/expiration → warehouse → wx → 列表观望** |
| 业务缓存「仅规划」 | 明确：基础设施已有，业务层未接 |
