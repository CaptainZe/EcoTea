/*
 Navicat Premium Data Transfer

 Source Server         : optimobi_14
 Source Server Type    : MySQL
 Source Server Version : 50726
 Source Host           : 10.15.2.14:3307
 Source Schema         : tea

 Target Server Type    : MySQL
 Target Server Version : 50726
 File Encoding         : 65001

 Date: 23/09/2026 01:37:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for chai_brand
-- ----------------------------
DROP TABLE IF EXISTS `chai_brand`;
CREATE TABLE `chai_brand`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '品牌名称',
  `name_initial` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '品牌名称首字母',
  `name_pinyin` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '品牌名称全字母',
  `logo` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 'logo',
  `order_num` int(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` int(11) NOT NULL DEFAULT 0 COMMENT '状态',
  `operator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '最后操作人',
  `update_time` bigint(20) NOT NULL COMMENT '更新时间',
  `create_time` bigint(20) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_key`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 154 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '茶叶-品牌表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chai_expiration
-- ----------------------------
DROP TABLE IF EXISTS `chai_expiration`;
CREATE TABLE `chai_expiration`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '显示名',
  `months` int(11) NOT NULL DEFAULT 0 COMMENT '月数',
  `order_num` int(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` int(11) NOT NULL DEFAULT 0 COMMENT '状态',
  `operator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '最后操作人',
  `update_time` bigint(20) NOT NULL COMMENT '更新时间',
  `create_time` bigint(20) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 114 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '茶叶-保质期表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chai_sku
-- ----------------------------
DROP TABLE IF EXISTS `chai_sku`;
CREATE TABLE `chai_sku`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spu_id` bigint(20) UNSIGNED NOT NULL COMMENT 'SPU ID',
  `sku_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SKU唯一编码',
  `star_level` int(11) NOT NULL DEFAULT 5 COMMENT '星级（热门度）',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称',
  `brand` bigint(20) NOT NULL COMMENT '品牌',
  `expiration` bigint(20) NOT NULL COMMENT '保质期',
  `type` int(11) NOT NULL COMMENT '茶类',
  `grade` int(11) NOT NULL COMMENT '等级',
  `year` int(11) NOT NULL COMMENT '年份',
  `prod_batch` int(11) NOT NULL COMMENT '生产批次',
  `spec` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '规格',
  `show_image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '展示图片列表',
  `real_image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '实物图片列表',
  `non_sale` tinyint(2) NOT NULL DEFAULT 0 COMMENT '是否非卖品：0否 1是',
  `official_price` decimal(12, 2) NULL DEFAULT NULL COMMENT '官方价(元)；非卖品为 NULL',
  `sale_price` decimal(12, 2) NOT NULL COMMENT '销售价',
  `recycle_price` decimal(12, 2) NOT NULL COMMENT '回收价',
  `recycle_price_reduce_per` int(11) NOT NULL COMMENT '回收价压价百分比',
  `recycle_price_reduce_no_bag` decimal(12, 2) NOT NULL COMMENT '回收价扣减（无提袋）',
  `status` int(11) NOT NULL DEFAULT 0 COMMENT '状态',
  `deleted` tinyint(2) NOT NULL DEFAULT 0 COMMENT '0有效 1已删除',
  `operator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '最后操作人',
  `update_time` bigint(20) NOT NULL COMMENT '更新时间',
  `create_time` bigint(20) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_key1`(`sku_code`) USING BTREE,
  UNIQUE INDEX `unique_key2`(`spu_id`, `year`, `prod_batch`) USING BTREE,
  INDEX `idx_spu_id`(`spu_id`) USING BTREE,
  INDEX `idx_deleted`(`deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 224 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '茶叶-SKU表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chai_spu
-- ----------------------------
DROP TABLE IF EXISTS `chai_spu`;
CREATE TABLE `chai_spu`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spu_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SPU唯一编码',
  `star_level` int(11) NOT NULL DEFAULT 5 COMMENT '星级（热门度）',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称',
  `brand` bigint(20) NOT NULL COMMENT '品牌',
  `expiration` bigint(20) NOT NULL COMMENT '保质期',
  `type` int(11) NOT NULL COMMENT '茶类',
  `grade` int(11) NOT NULL COMMENT '等级',
  `year` int(11) NOT NULL COMMENT '年份',
  `prod_batch` int(11) NOT NULL COMMENT '生产批次',
  `spec` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '规格',
  `show_image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '展示图片列表',
  `real_image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '实物图片列表',
  `non_sale` tinyint(2) NOT NULL DEFAULT 0 COMMENT '是否非卖品：0否 1是',
  `official_price` decimal(12, 2) NULL DEFAULT NULL COMMENT '官方价(元)；非卖品为 NULL',
  `status` int(11) NOT NULL DEFAULT 0 COMMENT '状态',
  `deleted` tinyint(2) NOT NULL DEFAULT 0 COMMENT '0有效 1已删除',
  `operator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '最后操作人',
  `update_time` bigint(20) NOT NULL COMMENT '更新时间',
  `create_time` bigint(20) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_key`(`spu_code`) USING BTREE,
  INDEX `idx_deleted`(`deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '茶叶-SPU表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chai_staff
-- ----------------------------
DROP TABLE IF EXISTS `chai_staff`;
CREATE TABLE `chai_staff`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `nick_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '花名',
  `real_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '真实姓名',
  `mobile` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '手机',
  `order_num` int(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint(2) NOT NULL DEFAULT 0 COMMENT '状态：0下架 1上架',
  `operator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '最后操作人',
  `update_time` bigint(20) NOT NULL COMMENT '更新时间',
  `create_time` bigint(20) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_nick_name`(`nick_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 110 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '茶叶-经手人' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chai_stock
-- ----------------------------
DROP TABLE IF EXISTS `chai_stock`;
CREATE TABLE `chai_stock`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sku_id` bigint(20) UNSIGNED NOT NULL COMMENT 'chai_sku.id，无外键',
  `total_qty` int(11) NOT NULL DEFAULT 0 COMMENT '各仓合计，过账时重算，>=0',
  `qty_no_bag` int(11) NOT NULL DEFAULT 0 COMMENT '外观完整无提袋，计入 total_qty',
  `qty_damaged` int(11) NOT NULL DEFAULT 0 COMMENT '外观破损有提袋，计入 total_qty',
  `qty_damaged_no_bag` int(11) NOT NULL DEFAULT 0 COMMENT '外观破损无提袋，计入 total_qty',
  `operator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '最后过账人',
  `update_time` bigint(20) NOT NULL COMMENT '更新时间',
  `create_time` bigint(20) NOT NULL COMMENT '首次建档时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_sku`(`sku_id`) USING BTREE,
  INDEX `idx_total_qty`(`total_qty`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 276 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '茶叶-库存结存' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chai_stock_bill
-- ----------------------------
DROP TABLE IF EXISTS `chai_stock_bill`;
CREATE TABLE `chai_stock_bill`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bill_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '单据号',
  `bill_type` tinyint(2) NOT NULL COMMENT '1入库 2出库 3调拨',
  `status` tinyint(2) NOT NULL DEFAULT 1 COMMENT '1已过账 2作废 3归档',
  `reason` int(11) NOT NULL COMMENT '原因',
  `handler_id` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '经手人id，0未选',
  `handler_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '过账时经手人快照：真实姓名(花名)',
  `from_wh_id` bigint(20) UNSIGNED NOT NULL COMMENT '入库=入到哪 / 出库=从哪出 / 调拨=调出；均不为0',
  `to_wh_id` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '仅调拨=调入仓，入出为0',
  `total_qty` int(11) NOT NULL DEFAULT 0 COMMENT '明细件数合计',
  `total_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '明细金额合计；调拨为0',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '备注',
  `operator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '最后操作人（过账或作废或归档）',
  `update_time` bigint(20) NOT NULL COMMENT '更新时间',
  `create_time` bigint(20) NOT NULL COMMENT '过账时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_bill_no`(`bill_no`) USING BTREE,
  INDEX `idx_type_status`(`bill_type`, `status`) USING BTREE,
  INDEX `idx_reason`(`reason`) USING BTREE,
  INDEX `idx_handler`(`handler_id`) USING BTREE,
  INDEX `idx_from_wh`(`from_wh_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 178 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '茶叶-库存单据' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chai_stock_bill_item
-- ----------------------------
DROP TABLE IF EXISTS `chai_stock_bill_item`;
CREATE TABLE `chai_stock_bill_item`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bill_id` bigint(20) UNSIGNED NOT NULL COMMENT '单据id',
  `sku_id` bigint(20) UNSIGNED NOT NULL COMMENT 'SKU id，无外键',
  `sku_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '过账时编码（查询）',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '过账时名称（查询）',
  `qty` int(11) NOT NULL COMMENT '件数，必须>0',
  `quality` int(11) NOT NULL DEFAULT 1 COMMENT '品相：1完整 2外观完整无提袋 3外观破损有提袋 4外观破损无提袋',
  `price` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '成交单价，手填；不取SKU目录价',
  `amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '行金额=qty*price',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '行备注：破损/无提袋等',
  `sku_snap` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '过账时完整ChaiSku持久化字段JSON，之后不随SKU改',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bill`(`bill_id`) USING BTREE,
  INDEX `idx_sku`(`sku_id`) USING BTREE,
  INDEX `idx_sku_code`(`sku_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 528 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '茶叶-库存单据明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chai_stock_wh
-- ----------------------------
DROP TABLE IF EXISTS `chai_stock_wh`;
CREATE TABLE `chai_stock_wh`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stock_id` bigint(20) UNSIGNED NOT NULL COMMENT 'chai_stock.id',
  `wh_id` bigint(20) UNSIGNED NOT NULL COMMENT 'chai_warehouse.id',
  `qty` int(11) NOT NULL DEFAULT 0 COMMENT '该仓结存，>=0',
  `qty_no_bag` int(11) NOT NULL DEFAULT 0 COMMENT '外观完整无提袋，计入 qty',
  `qty_damaged` int(11) NOT NULL DEFAULT 0 COMMENT '外观破损有提袋，计入 qty',
  `qty_damaged_no_bag` int(11) NOT NULL DEFAULT 0 COMMENT '外观破损无提袋，计入 qty',
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_stock_wh`(`stock_id`, `wh_id`) USING BTREE,
  INDEX `idx_wh`(`wh_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 416 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '茶叶-分仓结存' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chai_warehouse
-- ----------------------------
DROP TABLE IF EXISTS `chai_warehouse`;
CREATE TABLE `chai_warehouse`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '仓库名称',
  `short_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '仓库名称',
  `province` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '省编码(PCA)',
  `city` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '市编码(PCA)',
  `district` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '区/县编码(PCA)',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '详细地址',
  `order_num` int(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint(2) NOT NULL DEFAULT 0 COMMENT '状态：0下架 1上架',
  `operator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '最后操作人',
  `update_time` bigint(20) NOT NULL COMMENT '更新时间',
  `create_time` bigint(20) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_key`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 112 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '茶叶-仓库' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
