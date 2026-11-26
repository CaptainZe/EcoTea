# 商品条码（跨端）

> 覆盖 Admin 维护/打印、API 精确查、H5 / 公众号 / 小程序扫码。  
> Admin 总览：[`PRD_ADMIN.md`](../PRD_ADMIN.md)。发版：**1.5.0** 见 [`CHANGELOG_ADMIN.md`](../../changelog/CHANGELOG_ADMIN.md)。

## 1. 规则（跨端共用）

| 点 | 结论 |
|----|------|
| 粒度 | **挂在 SPU**；下属 SKU **共用**，不单独维护私有码 |
| 码数量 | **一 SPU 多码**（表 `chai_spu_barcode`） |
| 唯一性 | **不做** DB `UNIQUE(barcode)`；同一码可挂多个 SPU → 查询展示对应全部 SKU；同 SPU 同码唯一 `uk_spu_barcode` |
| 国标 69 | 手填 / 扫入；产品自带国标一般不贴标 |
| 系统码 29 | 内部 EAN-13：前缀 `29` + `spuId` 10 位 + ZXing 校验位；**每 SPU 最多一条 29\***；需打印粘贴 |
| 69 + 29 | **允许并存** |
| 主码 | **不强制**主码字段；标签优先打唯一的 29，否则在维护页选中要打的码 |
| 来源字段 | **不加** `barcode_source`；用前缀区分 29 / 69 |
| 编码库 | ZXing（校验位 + 出图 + 前端解码），不手写校验公式 |
| SKU 列表 | **不展示**条码列 |

录入允许：`69…` 或 `22～29…`（生成固定用 `29`）。

## 2. 数据模型

### 现行（多码）

表：`chai_spu_barcode`

| 字段 | 说明 |
|------|------|
| `spu_id` | SPU |
| `barcode` | EAN-13；`INDEX(barcode)`、`INDEX(spu_id)`、`UNIQUE(spu_id, barcode)` |
| `operator` / `update_time` / `create_time` | 审计 |

全量 DDL：[`docs/db/SQL_CHAI_DOMAIN.sql`](../../db/SQL_CHAI_DOMAIN.sql)。

### 废弃列（保留不删）

| 字段 | 表 | 说明 |
|------|-----|------|
| `barcode` | `chai_spu` / `chai_sku` | 业务**停写**（新建空串、更新保留原值）；迁移源 |

- Admin 工具：`/business/tools/chai` →「SPU 条码迁入多码表」（旧列 → 新表，幂等，可重复跑）。  
- 扫码解析过渡期仍**兼读**旧列（迁移完成后可再收紧）。

## 3. Admin

- 维护页：`/business/chai/spu/barcode/{id}` — 多码列表、添加/删除、生成系统码并落库、选中预览/打印；大表单不放条码字段。  
- 接口：`POST .../barcode/add|delete|generate/{id}`；写新表，**不写**旧列；写后 DEL Redis。  
- 标签纸：**横版 80×60mm**；设备：扫枪 HID、得力 DL-720W（浏览器打印 + 驱动）。  
- 列表：条码精确筛选 + 码类型（无码 / 国标 / 系统码）— 均读新表。  
- 库存单：扫码选品（分步：码 → SPU → SKU）。  
- 运营工具箱：系统码统计、按品牌批量打 29 码（读新表；与开发刷库 `/business/tools/chai` 分开）。

标签内容（示意）：品牌·品名、等级、规格、保质期、官方价、EAN-13 图与数字（**不展示批次**）。

## 4. API / 查询

分步（不必 SQL JOIN）：

```text
1) 码 → spu_id 列表（chai_spu_barcode；过渡期兼读旧列）
2) SKU 查询 WHERE spu_id IN (…) + 业务过滤
```

实现：`ChaiSpuBarcodeQueryService.resolveSpuIds`；sale / recycle / info 价目均走此路径。

**缓存（api）**：`ecotea_chai_barcode_spu_{barcode}` → JSON `[spuId,…]`（有值 `DAY_EXPIRE`；空列表 5 分钟）。  
**admin** 增删条码后 **DEL**。约定见 [`docs/coding/CACHE.md`](../../coding/CACHE.md)。

一码多 SPU → 返回这些 SPU 下符合条件的全部 SKU。  
有 `barcode` 参数时清空 keyword / 品牌等同次其它筛选（H5/小程序约定）。

## 5. H5 / 公众号

- 内部价目：摄像头 + ZXing（`inner-sale` / `sale` / `inner-recycle`）；请求 api 带 `barcode`。  
- 公众号 `scanQRCode` 可选（见历史 B7）。

## 6. 小程序

内部小程序扫码查货：同 API 精确匹配；走分步查码，不依赖列表 JOIN。

## 7. 设备与打印注意

- 浏览器 `@page` 无法强制驱动默认纸型；真机：纸张 80×60、缩放 100%、边距无。  
- 批量打印：按品牌排序连打；单次上限见 `ChaiSpuService.MAX_LABEL_PRINT`。

## 8. 发版注意（1.5.0）

1. 执行 CHANGELOG 中 `CREATE TABLE chai_spu_barcode`。  
2. 部署 admin / api。  
3. 茶业数据工具执行「条码迁入多码表」。  
4. 按下方 **测试用例** 冒烟。

## 9. 测试用例

| # | 场景 | 步骤要点 | 期望 |
|---|------|----------|------|
| 1 | 多码维护 69+29 | SPU 条码页添加合法 69；再点「生成系统码」 | 列表两条；29 落库；再点生成不新增第二条 29 |
| 2 | 同 SPU 同码 | 对已有码再「添加」一次 | 提示已存在，列表不重复 |
| 3 | 删除与打印 | 选中某码删除；选中 29 打印预览 | 删除后列表少一条；打印标签数字与选中码一致 |
| 4 | 一码多 SPU | 两 SPU 挂同一 69；Admin 库存扫码 / API 价目带 `barcode` | 返回两侧全部符合条件的 SKU |
| 5 | 列表筛选 | SPU 列表按精确条码、码类型（无码/国标/系统码） | 与新表数据一致（不依赖旧列） |
| 6 | 迁移幂等 | 旧列有值且新表尚无 → 跑 Tools 迁移；再跑一次 | 首次插入；二次跳过已存在；扫码可用新表 |
| 7 | 工具箱打印 | 运营工具箱按品牌打 29 | 统计/打印只含新表 29*；标签图与码正确 |
| 8 | 写后缓存 | admin 增删条码后，api 立刻用同码查询 | 结果即时更新（DEL `ecotea_chai_barcode_spu_{barcode}`） |
| 9 | H5 / 小程序 | 内部价目或小程序扫同一码 | 与 API 分步查结果一致 |
| 10 | 停写旧列 | 新建 SPU/SKU 后看库表旧 `barcode` | 新行为空串；维护多码只写 `chai_spu_barcode` |
