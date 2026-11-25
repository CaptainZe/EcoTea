# 商品条码（跨端）

> 覆盖 Admin 维护/打印、API 精确查、H5 / 公众号 / 小程序扫码。  
> 施工中的多码改造见 [`docs/todo/TODO_CHAI_BARCODE_MULTI.md`](../../todo/TODO_CHAI_BARCODE_MULTI.md)。  
> Admin 总览：[`PRD_ADMIN.md`](../PRD_ADMIN.md)。

## 1. 规则（跨端共用）

| 点 | 结论 |
|----|------|
| 粒度 | **挂在 SPU**；下属 SKU **共用**，不单独维护私有码 |
| 码数量 | **支持一 SPU 多码**（目标态：关联表）；当前生产仍为 SPU/SKU 单列 `barcode`，迁移见 TODO |
| 唯一性 | **不做** DB `UNIQUE(barcode)`；同一码可挂多个 SPU → 查询展示对应全部 SKU |
| 国标 69 | 手填 / 扫入；产品自带国标一般不贴标 |
| 系统码 29 | 内部 EAN-13：前缀 `29` + `spuId` 10 位 + ZXing 校验位；**每 SPU 最多一条 29\***；需打印粘贴 |
| 69 + 29 | **允许并存** |
| 主码 | **不强制**主码字段；标签优先打唯一的 29，否则在维护页指定要打的码 |
| 来源字段 | **不加** `barcode_source`；用前缀区分 29 / 69 |
| 编码库 | ZXing（校验位 + 出图 + 前端解码），不手写校验公式 |
| SKU 列表 | **不展示**条码列 |

录入允许：`69…` 或 `22～29…`（生成固定用 `29`）。

## 2. 数据模型

### 目标态（多码）

表建议：`chai_spu_barcode`（`spu_id` + `barcode` + 审计字段）；`INDEX(barcode)`、`INDEX(spu_id)`。

- 旧列 `chai_spu.barcode` / `chai_sku.barcode`：**废弃保留不删**；业务读写切到新表后停写旧列。  
- Admin 工具：`ChaiToolsController` 提供迁移（旧列有值 → 新表，幂等）。

### 当前态（单列，已上线）

| 字段 | 表 | 说明 |
|------|-----|------|
| `barcode` | `chai_spu` / `chai_sku` | `varchar(32)`；SKU 曾继承 SPU |

## 3. Admin

- 维护页：`/business/chai/spu/barcode/{id}`（录入/生成/预览/打印；大表单不放条码字段）。  
- 标签纸：**横版 80×60mm**；设备：扫枪 HID、得力 DL-720W（浏览器打印 + 驱动）。  
- 列表：条码精确筛选 + 码类型（无码 / 国标 / 系统码）；工具栏弹层扫码**暂缓**。  
- 库存单：扫码选品（方案 A，独立层）。  
- 运营工具箱：系统码统计、按品牌批量打 29 码（与开发 `ChaiToolsController` 分开）。

标签内容（示意）：品牌·品名、等级、规格、保质期、官方价、EAN-13 图与数字（**不展示批次**）。

## 4. API / 查询

扫码精确查推荐 **分步**（不必 SQL JOIN）：

```text
1) 码表（或过渡期旧列）WHERE barcode = ? → spu_id 列表
2) SKU 查询 WHERE spu_id IN (…) + 业务过滤
```

一码多 SPU → 返回这些 SPU 下符合条件的全部 SKU。  
有 `barcode` 参数时清空 keyword / 品牌等同次其它筛选（H5/小程序约定）。

## 5. H5 / 公众号

- 内部价目：摄像头 + ZXing（`inner-sale` / `sale` / `inner-recycle`）。  
- 公众号 `scanQRCode` 可选（见历史 B7）。

## 6. 小程序

内部小程序扫码查货：同 API 精确匹配约定；实现上走分步查码，不依赖列表 JOIN。

## 7. 设备与打印注意

- 浏览器 `@page` 无法强制驱动默认纸型；真机：纸张 80×60、缩放 100%、边距无。  
- 批量打印：按品牌排序连打；建议单次上限。
