# 管理后台版本记录

工程：`server-admin`（及约定的共享库表 / Redis）。与 Git 发版说明对齐。  
后续迭代在本文件**顶部**追加。

**SQL 分工**（详见 [`docs/README.md`](../README.md)「SQL 约定」）：

- `docs/db/`：**仅全量**脚本；改表时同步改对应 `SQL_*.sql`。  
- **本文件各版本节**：贴本版要执行的增量 SQL（可直接复制）+ checklist；**不**再写 `docs/db/tmp/`。  
- 历史版未贴全文的，checklist 保留要点即可，不必补进 tmp。

## 通用发布 checklist

- [ ] 目标库已按**本版**增量 SQL / checklist 执行  
- [ ] 全量 `docs/db/SQL_*.sql` 已与本版结构对齐（新环境可用）  
- [ ] 菜单 / 权限已赋权（若本版有增量）  
- [ ] 主数据写后 DEL / 踢会话（若本版改了缓存相关路径）  
- [ ] 冒烟：登录、SPU/SKU、库存、相关新入口  
- [ ] 版本号与下面记录一致  

---

## 版本日志

### 1.5.0 · 已发布 · 2026-11-26

**版本内容**

- SPU 支持多条码：表 `chai_spu_barcode`（一 SPU 多码；不做 barcode 全局唯一；同 SPU 同码唯一）  
- 维护页多码 CRUD；扫码分步查（码→SPU→SKU）；api 缓存 `ecotea_chai_barcode_spu_{barcode}`，admin 写后 DEL  
- 旧列停写、保留作迁移源；Tools 幂等迁入；工具箱统计/批量打印/SPU 码类型筛选读新表  
- 细则：[`docs/prd/features/BARCODE.md`](../prd/features/BARCODE.md)  
- SPU **标记区**：对外隐藏 / 核心产品 / 品鉴款（默认 0）；品鉴可挂整装 `pack_spu_id`；同步 SKU；编辑页「标记」区；列表标签 +「操作」批量改标记  
- 对外 `sale`/公众号默认过滤 `hide_external`；内部价目 `includeHidden=1`；小程序/回收不过滤  
- 细则与冒烟用例：[`docs/prd/features/SPU_MARK.md`](../prd/features/SPU_MARK.md)

**本版增量 SQL**（目标库执行）

```sql
-- 条码多码表
CREATE TABLE IF NOT EXISTS `chai_spu_barcode` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spu_id` bigint(20) UNSIGNED NOT NULL COMMENT 'SPU ID',
  `barcode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '条形码（EAN-13；一SPU可多码）',
  `operator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '最后操作人',
  `update_time` bigint(20) NOT NULL COMMENT '更新时间',
  `create_time` bigint(20) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_spu_barcode`(`spu_id`, `barcode`) USING BTREE,
  INDEX `idx_barcode`(`barcode`) USING BTREE,
  INDEX `idx_spu_id`(`spu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '茶叶-SPU条码（一SPU多码；barcode 不做全局唯一）' ROW_FORMAT = Dynamic;

-- SPU 标记区
ALTER TABLE `chai_spu`
  ADD COLUMN `hide_external` tinyint(2) NOT NULL DEFAULT 0 COMMENT '对外隐藏：0否 1是' AFTER `official_price`,
  ADD COLUMN `core_flag` tinyint(2) NOT NULL DEFAULT 0 COMMENT '核心产品：0否 1是' AFTER `hide_external`,
  ADD COLUMN `tasting_flag` tinyint(2) NOT NULL DEFAULT 0 COMMENT '品鉴款：0否 1是' AFTER `core_flag`,
  ADD COLUMN `pack_spu_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '整装SPU ID；品鉴款可空' AFTER `tasting_flag`,
  ADD INDEX `idx_hide_external`(`hide_external`),
  ADD INDEX `idx_pack_spu_id`(`pack_spu_id`);

ALTER TABLE `chai_sku`
  ADD COLUMN `hide_external` tinyint(2) NOT NULL DEFAULT 0 COMMENT '对外隐藏：0否 1是（同步自SPU）' AFTER `recycle_price_reduce_no_bag`,
  ADD COLUMN `core_flag` tinyint(2) NOT NULL DEFAULT 0 COMMENT '核心产品：0否 1是（同步自SPU）' AFTER `hide_external`,
  ADD COLUMN `tasting_flag` tinyint(2) NOT NULL DEFAULT 0 COMMENT '品鉴款：0否 1是（同步自SPU）' AFTER `core_flag`,
  ADD COLUMN `pack_spu_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '整装SPU ID；品鉴款可空（同步自SPU）' AFTER `tasting_flag`,
  ADD INDEX `idx_hide_external`(`hide_external`),
  ADD INDEX `idx_pack_spu_id`(`pack_spu_id`);
```

**发布 checklist**

- [ ] 已执行上方 `CREATE TABLE chai_spu_barcode`  
- [ ] 已执行上方 `chai_spu` / `chai_sku` 标记字段 `ALTER`  
- [ ] 全量 `docs/db/SQL_CHAI_DOMAIN.sql` 已含本表/本字段（新环境可用）  
- [ ] 部署 admin / api 后，茶业数据工具执行「SPU 条码迁入多码表」  
- [ ] 冒烟：按 [`BARCODE.md`](../prd/features/BARCODE.md)、[`SPU_MARK.md`](../prd/features/SPU_MARK.md) §测试用例

---

### 1.3.1

**版本内容**

- 商品搜索：SPU/SKU 增加 `keywords` / `search_text`（关键词预埋已实现；列表侧搜索接入见 `docs/todo/TODO_CHAI_SEARCH_KEYWORDS.md`）  
- SPU 条形码维护与扫码支持（一 SPU 一码，SKU 继承）  
- 库存列表表格导出  

**发布 checklist**

- [ ] `chai_spu` / `chai_sku`：增加 `keywords`、`search_text`；索引 `idx_brand`、`idx_type`、`idx_deleted_status`；按需 `DROP INDEX idx_deleted`  
- [ ] `chai_spu` / `chai_sku`：增加 `barcode`（`varchar(32)`，暂不加唯一索引）  
- [ ] 冒烟：条码维护/打印、库存导出；关键词字段可保存  

---

### 1.3.0

**版本内容**

- 库存品相逻辑调整（完整 / 外观完整无提袋 / 破损有提袋 / 破损无提袋）  
- 品牌排序优化（`name_initial` / `name_pinyin`）  
- 库存单据选品增加「换同款」  

**发布 checklist**

- [ ] `chai_stock` / `chai_stock_wh`：增加 `qty_no_bag`、`qty_damaged`、`qty_damaged_no_bag`；**删除**旧列 `damage_qty`  
- [ ] `chai_stock_bill_item`：增加 `quality`（1～4）；**删除**旧列 `appearance_damaged`  
- [ ] `chai_brand`：增加 `name_initial`、`name_pinyin`；可用工具批量回填拼音  
- [ ] 按环境清理无效菜单（手记曾删 `status != 1` 及指定 id；**生产慎用**，先备份核对）  
- [ ] 冒烟：出入库品相、选品换同款、品牌列表字母序  

---

### 1.2.1

**版本内容**

- 删除无用代码与旧业务表/字典  

**发布 checklist**

- [ ] `DROP`：`tea_quote_order_item`、`tea_quote_order`、`tea_partner`、`app_notice`、`daily_sequence`（确认环境无依赖）  
- [ ] `DELETE` 字典：`APP_NOTICE_TYPE`、`TEA_APPEARANCE_CONDITION`、`TEA_HAS_BAG`、`TEA_QUOTE_ORDER_*`、`TEA_PARTNER_*`、`PAY_METHOD`  
- [ ] 冒烟：后台无引用上述模块的死链/报错  

---

### 1.2.0

**版本内容**

- 微信生态相关配置（关注欢迎语、回收说明、客服、销售 H5 文案；订阅号菜单）  
- 菜单：微信生态；字典 `WX_GLOBAL_CONFIG_TYPE`（1 欢迎语 / 2 回收说明 / 3 客服 / 4 销售 H5 文案）  

**发布 checklist**

- [ ] 建表 `wx_global_config`、`wx_mp_menu`（或与全量 `docs/db/SQL_WX_DOMAIN.sql` 对齐）  
- [ ] 字典 / 菜单：`WX_GLOBAL_CONFIG_TYPE`、微信生态菜单赋权  
- [ ] 冒烟：后台改欢迎语/客服文案；api 侧读配置（写后 DEL 见 `docs/coding/CACHE.md`）  

---

### 1.1.2

**版本内容**

- SPU/SKU 增加「非卖品」；官方价可空  
- 库存增加破损标识（早期 `damage_qty` / `appearance_damaged`；后由 **1.3.0** 品相模型替代）  
- 清理已删 SPU/SKU 数据（手记含 `DELETE … WHERE deleted=1`）  

**发布 checklist**

- [ ] `chai_spu` / `chai_sku`：`non_sale`；`official_price` 可空；SKU 价格字段精度对齐  
- [ ] （若尚未跑 1.3.0）结存 `damage_qty`、明细 `appearance_damaged`；**已上 1.3.0 的环境以 1.3.0 为准，勿重复加旧列**  
- [ ] 数据清理脚本仅在确认后执行  
- [ ] 冒烟：非卖品建档与价目展示  

---

### 1.1.1

**版本内容**

- 新增茶品价目视图  
- SKU 无提袋扣减默认档位规则调整  
- 茶品列表增加「茶类」展示；SPU 列表增加「复制」  

无提袋扣减默认（按回收价 p）：

| 回收价 p | 默认扣减 |
|----------|----------|
| p &lt; 100 | 0 |
| [100, 500) | 10 |
| [500, 1000) | 15 |
| [1000, 1500) | 25 |
| [1500, 2000) | 35 |
| [2000, 3000) | 40 |
| [3000, 4000) | 45 |
| p ≥ 4000 | 50 |

**发布 checklist**

- [ ] 按档位 `UPDATE chai_sku.recycle_price_reduce_no_bag`（有回收价的行；建议事务 + 先 SELECT 核对）  
- [ ] 菜单/权限：价目视图（若有增量）  
- [ ] 冒烟：价目视图、SPU 复制、无提袋默认值  

---

### 1.1.0

**版本内容**

- （手记未写明细）  

**发布 checklist**

- [ ] 对照当时发版说明补记后勾选  

---

### 1.0.1

**版本内容**

- （手记未写明细）  

**发布 checklist**

- [ ] 对照当时发版说明补记后勾选  

---

### 1.0.0

**版本内容**

- 初版 / 基线（手记未写明细）  

**发布 checklist**

- [ ] 全量库表与菜单按当时基线就绪  
