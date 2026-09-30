# 业务 Redis 缓存规划

> 状态：**规划已按当前业务重排；业务缓存代码尚未接入**。  
> 基础设施已就绪：`DEPLOY_REDIS.md`、两端 `RedisUtils` / `LockUtils`、admin `RedisSeqUtils`（日序号）。  
> 原则：admin / api **共用同一 Redis**、同一前缀 `ecotea_`（`RedisConstant.KEY_PRE`）；key 用 `_` 分隔，不用 `:`。  
> WxJava token 等仍用 `wx:mp:`，不并入本规范。不管 tea 模块。

## 目标（本期）

优先做 **主数据热读缓存**：读多写少，**admin 改库成功后 DEL 对应 key**，api / H5 下次 miss 再加载。  
销售 / 回收 **列表整页缓存默认不做**（筛选维度已膨胀，key 组合多、命中差）；确有性能证据再局部加。

## 现状

| 已有 | 说明 |
|------|------|
| Redis 部署与连接 | 见 `DEPLOY_REDIS.md` |
| `RedisUtils` / `RedisTemplateHolder` | admin、api 对齐 |
| `LockUtils` | 分布式锁（NX） |
| `RedisSeqUtils` | 出入库等日序号 `ecotea_seq_{biz}_{yyyyMMdd}` |
| 业务主数据 / 列表缓存 | **未接** |

## 职责

| 端 | 行为 |
|----|------|
| **admin** | 写库；主数据变更成功后 **DEL** 对应 key；后台分页 CRUD **直查库**（不强制走缓存） |
| **api** | 热读：Redis → miss → MySQL → SET（建议 `DAY_EXPIRE` 兜底） |
| **字典** | api 走 Redis（若有读）；admin 读继续 **EhCache**，改字典时同时清 EhCache + DEL Redis |

## Key 约定（第一期：主数据）

```text
# 写后 DEL + DAY_EXPIRE 兜底
ecotea_chai_brand_online                 # 上架品牌列表（有序，~百级）
ecotea_chai_expiration_online            # 上架保质期列表（~二十）
ecotea_chai_warehouse_online             # 上架仓库列表（内部价目筛仓）
ecotea_wx_global_config_{type}           # wx_global_config 按 type（见 WxGlobalConfigType）
ecotea_dict_{name}                       # sys_dict（api 侧；有读再做）
```

- value：字符串；列表 / 配置用 JSON（api `JsonUtils`）。  
- 空列表也可短 TTL 缓存，防穿透。  
- **禁止**生产用 `KEYS *` 扫删；批量失效用 SCAN 或版本号（本期单 key DEL 即可）。

## 第一期：主数据（写后 DEL）

### 1. chai_brand

- 单 key `ecotea_chai_brand_online` = 上架且有序列表（与现 `listOnlineOrdered` 一致）。  
- api：`resolveBrandIdExact`、补品牌名、H5 筛选品牌列表，优先读缓存；命中后在内存按名/id 索引。  
- admin：`save` / 删除 / 改状态成功后 **DEL**。

### 2. chai_expiration

- 单 key `ecotea_chai_expiration_online`。  
- api：价目补保质期名。  
- admin：写后 **DEL**。

### 3. chai_warehouse（相对旧规划新增）

- 单 key `ecotea_chai_warehouse_online`。  
- api：内部价目仓下拉 / `ChaiWarehouseQueryService.listOnline`。  
- admin：写后 **DEL**。

### 4. wx_global_config

- key：`ecotea_wx_global_config_{type}`，value = 该行 `config` JSON（或整行序列化）。  
- type：`SUBSCRIBE_WELCOME` / `RECYCLE_DESC` / `CUSTOMER_SERVICE` / `SALE_H5_COPY`。  
- api：`WxGlobalConfigService.getByType` 及欢迎语、客服、H5 文案、关键词「客服」等。  
- admin：`save` / `delete` 按 **type DEL**（改动少，**必须立刻生效**）。  
- **不缓存** `wx_mp_menu`（发布低频，直查库）。

### 5. sys_dict（可选，api 确有读再做）

- api：`DictUtils` / 相关 Service：Redis → DB → SET。  
- admin：读 EhCache；`DictServiceImpl` 写成功后按 `name` DEL Redis，并 `DictUtil.clearCache`。

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

1. **chai_brand** + **chai_expiration**（api 热读 + admin DEL）  
2. **chai_warehouse**  
3. **wx_global_config**（关注/H5/关键词客服立刻受益）  
4. **sys_dict（api）**（若需要）  
5. 列表页 TTL：**观望**，有证据再局部加  

## 验收要点（第一期）

1. admin 改品牌名 / 上下架 → H5 筛选与标题/搜品牌 **立刻**新。  
2. admin 改保质期名 → 价目展示同步。  
3. admin 改仓库 → 内部价目仓列表同步。  
4. admin 改欢迎语 / 客服 / 回收 / 销售文案 → 真机或 H5 **立刻**新（写后 DEL）。  
5. （若做字典）admin 改字典 → api 立刻读到新值。  
6. Redis miss 时行为与现直查库一致；DEL 后下一次请求应重新 SET。

## 与旧版规划差异

| 旧 | 现 |
|----|----|
| 销售分页（brand/spu）优先实现 | **下调**；默认不做整页缓存 |
| 无仓库缓存 | **补** `chai_warehouse_online` |
| 实现顺序以 dict → brand → wx → 销售 | 改为 **brand/expiration → warehouse → wx → dict → 列表观望** |
| 业务缓存「仅规划」 | 明确：基础设施已有，业务层未接 |
