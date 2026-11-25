# 条码多码改造（施工中）

> 定稿规则见 [`docs/prd/features/BARCODE.md`](../prd/features/BARCODE.md)。  
> **完成后**：把变更点并入 `BARCODE.md` / `SQL_CHAI_DOMAIN.sql`，再 **删除本文件**。

## 范围

- [ ] DDL：`chai_spu_barcode`；同步全量 `docs/db/SQL_CHAI_DOMAIN.sql`；增量 SQL 发版时写入 `CHANGELOG_ADMIN` 对应版本节（不落 `db/tmp`）  
- [ ] admin / api Entity + 维护页改为多码（每 SPU 最多一条 `29*`）  
- [ ] 查询分步：码 → `spu_id` → SKU；Admin / API / H5 / 小程序扫码路径切换  
- [ ] 停写旧 `chai_spu.barcode` / `chai_sku.barcode`（列保留）  
- [ ] `ChaiToolsController`：旧列有值迁入新表（幂等、可重复跑）  
- [ ] 工具箱统计 / 批量打印改读新表  
- [ ] 验收：一码多 SPU 列出全部 SKU；69+29 并存；迁移后扫码与打印正常  

## 不做

- DB `UNIQUE(barcode)`  
- SKU 私有码  
- SKU 列表展示条码  
- 强制主码字段  
