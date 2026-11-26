# 业务 Redis 缓存约定

> 编码硬性规则见 [`CODING_API.md`](CODING_API.md) §8、[`CODING_ADMIN.md`](CODING_ADMIN.md) §3.1。  
> 基础设施：[`docs/deploy/DEPLOY_REDIS.md`](../deploy/DEPLOY_REDIS.md)。  
> 原则：admin / api **共用同一 Redis**、前缀 `ecotea_`（`RedisConstant.KEY_PRE`）；key 用 `_` 分隔，不用 `:`。  
> WxJava token 等仍用 `wx:mp:`，不并入本规范。不管 tea 模块。  
> **新功能默认先评估缓存**（热读主数据：api SET + admin DEL；业务单据默认不缓存）。

## 职责

| 端 | 行为 |
|----|------|
| **admin** | 写库成功后 **DEL** 对应 key；后台分页 CRUD **直查库** |
| **api** | Redis → miss → MySQL → SET（建议 `DAY_EXPIRE`；库无可用短 TTL 防穿透） |
| **字典** | api 走 Redis；admin 读 EhCache，写成功后 EhCache + Redis 双清 |

**禁止**生产用 `KEYS *` 扫删。

## Key 一览

```text
# 主数据（写后 DEL + DAY_EXPIRE）— 已接
ecotea_dict_{name}
ecotea_chai_brand_online
ecotea_chai_expiration_online
ecotea_chai_warehouse_online
ecotea_chai_barcode_spu_{barcode}   # 条码→SPU id 列表 JSON；有值 DAY；空列表 5 分钟
ecotea_wx_global_config_{type}

# 员工会话 — 已接
ecotea_emp_session_{token}
ecotea_emp_uid_{employeeId}

# 组织域
ecotea_tenant_{tenantId}          # 已接
ecotea_tenant_online              # 可选，未接
```

常量定义在两端各自的 `RedisConstant`（码值一致，禁止共用 jar）。

## 已接能力摘要

| Key / 能力 | api | admin 失效 |
|------------|-----|------------|
| `dict_{name}` | `SysDictService.getByNameOk` | `DictUtil.clearCache` |
| `chai_brand_online` | `ChaiBrandService.listOnlineOrdered`（H5 常用置顶） | save / 删除 / 拼音回填 |
| `chai_expiration_online` | `ChaiExpirationService` | save / 删除 |
| `chai_warehouse_online` | `ChaiWarehouseQueryService` | save / 删除 |
| `chai_barcode_spu_{barcode}` | `ChaiSpuBarcodeQueryService.resolveSpuIds`（扫码分步第 1 步） | 条码 add / delete |
| `wx_global_config_{type}` | `WxGlobalConfigService.getByType` | save / delete 按 type |
| `tenant_{tenantId}` | `TenantService.getByBizTenantId` | `TenantService.save` |
| 踢员工会话 | — | 改密码/状态/挂店/perm → `EmployeeSessionCacheService` |

## 明确不做

- tea_* 缓存；库存 Hash 主方案；单 SKU/SPU 详情长缓存  
- 销售/回收价目整页、报价/销售/回收业务单长缓存（默认直查库）  
- 生产 `KEYS *`；把日序号/锁再包装成「业务缓存」  
- 不缓存 `employee` 全表、`admin_tenant_scope`；挂店 perm 以会话 + 踢会话为准  

## 可选后续

- `tenant_online`（启用店列表）  
- 列表页短 TTL：仅在有压测证据时局部加；扫码只缓存「码→spuIds」，不缓存价目整页  

## 验收要点

1. admin 改字典/品牌/保质期/仓库/文案/店名 → 读端立刻新（写后 DEL）。  
2. admin 改员工 perm 或停用 → 小程序旧 token 失效。  
3. miss 行为与直查库一致；DEL 后下次请求重新 SET。  
4. admin 增删 SPU 条码 → `chai_barcode_spu_{barcode}` 失效，扫码立刻反映。
