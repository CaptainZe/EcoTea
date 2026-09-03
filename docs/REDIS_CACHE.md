# 业务 Redis 缓存规划（待实现）

> 状态：**仅规划，代码未接入**。基础设施见 `REDIS_DEPLOY.md`。  
> 原则：admin / api 共用同一 Redis、同一前缀 `ecotea_`（`RedisConstant.KEY_PRE`）；key 用 `_` 分隔，不用 `:`。  
> 不管 tea 模块（后续删除）。销售价目列表结果**不做** Redis 缓存（后续要加库存展示，再单独设计）。

## 职责

| 端 | 行为 |
|----|------|
| admin | 可写库；改数据后删对应 Redis key；字典读仍用 EhCache |
| api | 主要读 Redis；miss 回源 MySQL 再回填 |

## Key 约定

```text
ecotea_dict_{name}                 # sys_dict：可用字典内容（建议缓存 value 或整条可用记录）
ecotea_chai_brand_online           # 上架品牌列表（约 200 条，单 key 足够）
ecotea_chai_expiration_online      # 上架保质期列表（约 20 条，单 key 足够）
```

- TTL：建议 `DAY_EXPIRE` 作兜底；一致性靠写后 DEL。  
- WxJava token 等仍用 `wx:mp:`，不并入上述业务规范。

## 分步实现（后续按序）

### 1. sys_dict

- **api**：`SysDictService` / `DictUtils` 读 Redis → miss → DB → SET。  
- **admin**：读继续 **EhCache**（`DictUtil`）。  
- **删 Redis**：集中在 `DictServiceImpl`（`save` / `updateDictValue` / `updateStatus` 成功后按 `name` DEL）；不必散落在 Controller。  
- EhCache 清理可仍用现有 `DictUtil.clearCache`，或一并收到 Service。

### 2. chai_brand（全 Redis 热读）

- 量级约 **200** → 只维护 `ecotea_chai_brand_online`；按名/按 id 在内存从列表建索引。  
- admin：`save` / 删除 / 改状态后 DEL 该 key；`listOnlineOrdered` 等热读走 Redis。  
- 后台分页 CRUD 可继续直查库。  
- api：价目里品牌精确匹配、补品牌名走该缓存。

### 3. chai_expiration（全 Redis 热读）

- 量级约 **20** → 同品牌，单 key `ecotea_chai_expiration_online`。  
- 写后 DEL；api 补保质期名称走缓存。

### 4. 销售价目查询（明确暂缓缓存）

- 当前 `/chai/sku/sale/list` 仅联调/试跑；**列表结果不进 Redis**。  
- 后续需展示**库存数量**时，再定库存数据源与是否短 TTL；与主数据缓存解耦。

## 不做

- tea_* 相关缓存  
- 生产用 `KEYS *` 扫删（若以后有前缀批量失效，用 SCAN 或版本号）

## 验收要点（实现时）

1. admin 改字典 → api `DictUtils` 立刻读到新值。  
2. admin 改品牌名/上下架 → H5 品牌搜与标题同步。  
3. admin 改保质期名称 → 价目展示同步。
