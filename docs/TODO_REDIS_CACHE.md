# 业务 Redis 缓存规划（待实现）

> 状态：**仅规划，代码未接入**。基础设施见 `DEPLOY_REDIS.md`。  
> 原则：admin / api 共用同一 Redis、同一前缀 `ecotea_`（`RedisConstant.KEY_PRE`）；key 用 `_` 分隔，不用 `:`。  
> 不管 tea 模块（后续删除）。WxJava token 等仍用 `wx:mp:`，不并入本规范。  
> 相关业务：`PRD_WX_MP.md`（步骤 9 暂缓实现本文件）。

## 职责

| 端 | 行为 |
|----|------|
| admin | 可写库；**主数据**（字典/品牌/保质期/微信配置）改后 **DEL** 对应 key；字典读仍用 EhCache |
| api | 读优先 Redis；miss → MySQL → SET |
| 销售列表 | **不强制写后失效**；靠 **TTL 自动过期**，可接受短延迟（见 §5） |

## Key 约定

```text
# —— 主数据（写后 DEL + DAY_EXPIRE 兜底）——
ecotea_dict_{name}                      # sys_dict
ecotea_chai_brand_online                # 上架品牌列表（~200）
ecotea_chai_expiration_online           # 上架保质期列表（~20）
ecotea_wx_global_config_{type}          # wx_global_config.config JSON（按 type）

# —— 销售有货分页（仅 TTL，不写后 DEL）——
ecotea_chai_sale_p_all_0_{page}_{size}
ecotea_chai_sale_p_brand_{brandId}_{page}_{size}
ecotea_chai_sale_p_spu_{spuId}_{page}_{size}
ecotea_chai_sale_p_brand_spu_{brandId}_{spuId}_{page}_{size}
ecotea_chai_sale_p_like_{kwHash}_{page}_{size}   # 可选；本期可不做
```

- value：字符串；VO / 列表用 JSON（api `JsonUtils`）。  
- 空结果也可短 TTL 缓存，防穿透。

## 分步实现（按序）

### 1. sys_dict

- **api**：`SysDictService` / `DictUtils`：Redis → miss → DB → SET（`DAY_EXPIRE`）。  
- **admin**：读继续 **EhCache**（`DictUtil`）。  
- **删 Redis**：集中在 `DictServiceImpl`（`save` / `updateDictValue` / `updateStatus` 成功后按 `name` DEL）。  
- EhCache 清理可仍用 `DictUtil.clearCache`。

### 2. chai_brand（全 Redis 热读）

- 单 key `ecotea_chai_brand_online`；按名/id 在内存从列表建索引。  
- admin：`save` / 删除 / 改状态后 DEL；热读走 Redis；后台分页 CRUD 可直查库。  
- api：`resolveBrandIdExact`、补品牌名走该缓存。

### 3. chai_expiration（全 Redis 热读）

- 单 key `ecotea_chai_expiration_online`；写后 DEL；api 补保质期名。

### 4. wx_global_config

- key：`ecotea_wx_global_config_{type}`，value = 该行 `config` JSON（或整行序列化）。  
- **api**：`WxGlobalConfigService.getByType` 及欢迎语/回收/客服/销售文案读取走缓存。  
- **admin**：`WxGlobalConfigService.save` / `delete` 成功后按 **type** DEL（配置改动少，要求尽快生效，**必须写删**）。  
- **不缓存** `wx_mp_menu`（发布低频，直查库即可）。

### 5. 销售有货列表 + 关键词查价（TTL，可延迟）

入口：`ChaiSkuSaleQueryService.pageSaleList(keyword, spuId, page, size)`；  
H5 `GET /chai/sku/sale/list` 与关键词回复（`page=1,size=10`）**共用**本缓存。

#### 5.1 策略

- 缓存整页 **`ChaiSkuSalePageVO`**（含 list、total、matchType、库存/破损等），**一页一个 key**。  
- **不做**写路径精确失效（库存单据/SKU 变更不 DEL）；只靠 TTL，接受最多一个 TTL 的展示延迟。  
- **不单独**缓存库存表；库存已打进 VO。  
- **不缓存**微信被动回复整段文案（复用列表缓存即可）。

#### 5.2 查询形态 → key（须带 page、size）

先规范化 `page`/`size`，再判定 shape（与现逻辑一致：有品牌精确则不用 LIKE）：

| shape | 条件 | key | 建议 TTL |
|-------|------|-----|----------|
| `all` | 无 keyword、无 spuId | `…_all_0_{page}_{size}` | **30–60s**（默认浏览，库存更敏感） |
| `brand` | keyword 命中品牌全称 | `…_brand_{brandId}_{page}_{size}` | **120–300s** |
| `spu` | 仅 `spuId` | `…_spu_{spuId}_{page}_{size}` | **120–300s** |
| `brand_spu` | 品牌 + spu | `…_brand_spu_{brandId}_{spuId}_{page}_{size}` | **120–300s** |
| `like` | 品名模糊 | `…_like_{md5(kw)}_{page}_{size}` | **30s** 或 **本期可不做** |

- 品牌 key 用 **brandId**（先 `resolveBrandIdExact`），不要用中文 keyword。  
- H5 默认 `size=20` 与关键词 `size=10` 自然分 key，互不覆盖。  
- **优先实现 `brand` + `spu`（+ 可选 `brand_spu`）**；`all` 可选；`like` 词空间大、命中差，可不做。

#### 5.3 读写流程

```text
规范化 page/size → 判定 shape → 拼 key → GET
  hit  → 反序列化返回
  miss → 现有 DB 查询（含库存聚合）→ SET + TTL → 返回
```

关键词「客服」等：只走 §4 微信配置缓存，不走销售列表。

#### 5.4 明确不做（销售侧）

- 以「库存 Hash + 每次仍查 SKU」为主方案（复杂，且已接受 TTL 延迟）。  
- 生产用 `KEYS *` 按前缀扫删销售 key。  
- 为每个模糊 keyword 做长 TTL。

## 不做（全局）

- tea_* 相关缓存  
- 生产用 `KEYS *` 扫删（若以后要批量失效，用 SCAN 或版本号）

## 实现顺序建议

1. sys_dict  
2. chai_brand / chai_expiration  
3. wx_global_config（关注/H5/关键词客服立刻受益）  
4. 销售分页：`brand` + `spu`（TTL）→ 可选 `all` → 视情况 `like`  

## 验收要点（实现时）

1. admin 改字典 → api `DictUtils` 立刻读到新值。  
2. admin 改品牌名/上下架 → H5 品牌搜与标题同步。  
3. admin 改保质期名称 → 价目展示同步。  
4. admin 改欢迎语/客服/回收/销售文案 → 真机或 H5 **立刻**新内容（写后 DEL）。  
5. 品牌精确搜、同款分页：二次请求命中 Redis；改库存后最多约一个 TTL 内旧数据可接受。  
6. 关键词查价与 H5 同 brand/size 共享缓存；客服词不误走销售 key。  
