CREATE TABLE `tea_sku` (
                           `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                           `sku_code` varchar(64) NOT NULL COMMENT 'SKU唯一编码',
                           `star_level` int(11) NOT NULL DEFAULT 5 COMMENT '星级（热门度）',
                           `name` varchar(255) NOT NULL COMMENT '商品名称',
                           `brand` int(11) NOT NULL COMMENT '品牌',
                           `type` int(11) NOT NULL COMMENT '茶类',
                           `grade` int(11) NOT NULL COMMENT '等级',
                           `spec` varchar(255) NOT NULL DEFAULT '' COMMENT '规格',
                           `year` int(11) NOT NULL COMMENT '年份',
                           `production_batch` varchar(255) NOT NULL DEFAULT '' COMMENT '生产批次',
                           `expiration` int(11) NOT NULL COMMENT '保质期',
                           `barcode` varchar(255) NOT NULL DEFAULT '' COMMENT '条形码',
                           `image_urls` text COMMENT '展示图片列表',
                           `real_image_urls` text COMMENT '实物图片列表',
                           `official_price` decimal(10,2) DEFAULT NULL COMMENT '官方价',
                           `sale_price` decimal(10,2) NOT NULL COMMENT '销售价',
                           `recycle_price` decimal(10,2) NOT NULL COMMENT '回收价',
                           `recycle_price_reduce_per` int(11) NOT NULL COMMENT '回收价压价百分比',
                           `recycle_price_reduce_no_bag` decimal(10,2) NOT NULL COMMENT '回收价扣减（无提袋）',
                           `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '状态',
                           `operator` varchar(255) NOT NULL DEFAULT '' COMMENT '最后操作人',
                           `update_time` bigint(20) NOT NULL COMMENT '更新时间',
                           `create_time` bigint(20) NOT NULL COMMENT '创建时间',
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `unique_key` (`sku_code`) USING BTREE,
                           KEY `idx_brand` (`brand`),
                           KEY `idx_type` (`type`),
                           KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='茶叶SKU表';

CREATE TABLE `tea_quote_order` (
                                   `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `order_no` varchar(128) NOT NULL COMMENT '报价单号',
                                   `type` int(11) NOT NULL DEFAULT '100' COMMENT '订单类型 TEA_QUOTE_ORDER_TYPE',
                                   `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                   `user_name` varchar(255) NOT NULL DEFAULT '' COMMENT '用户昵称',
                                   `total_amount` decimal(10,2) NOT NULL COMMENT '报价总金额',
                                   `total_quantity` int(11) NOT NULL COMMENT '总数量',
                                   `total_manual_amount` decimal(10,2) DEFAULT NULL COMMENT '人工调整后总金额',

                                   `express_company` int(11) NOT NULL DEFAULT '0' COMMENT '快递公司',
                                   `express_no` varchar(255) NOT NULL DEFAULT '' COMMENT '快递单号',
                                   `pay_method` int(11) NOT NULL DEFAULT '0' COMMENT '支付方式',
                                   `pay_info` varchar(1000) NOT NULL DEFAULT '' COMMENT '支付信息(json)',

                                   `status` int(11) NOT NULL DEFAULT '0' COMMENT '状态',
                                   `operator` varchar(64) NOT NULL COMMENT '最后操作人',
                                   `update_time` bigint(20) NOT NULL COMMENT '更新时间',
                                   `create_time` bigint(20) NOT NULL COMMENT '创建时间',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `unique_key` (`order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='茶叶回收报价单';

CREATE TABLE `tea_quote_order_item` (
                                        `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                        `order_id` bigint(20) unsigned NOT NULL COMMENT '报价单ID',

                                        `sku_id` bigint unsigned NOT NULL COMMENT 'SKU ID（内部关联）',
                                        `sku_code` varchar(64) NOT NULL COMMENT 'SKU编码（冗余快照）',
                                        `sku_name` varchar(255) NOT NULL DEFAULT '' COMMENT 'SKU名称（冗余快照）',
                                        `sku_brand` varchar(64) NOT NULL DEFAULT '' COMMENT 'SKU品牌（冗余快照）',
                                        `sku_spec` varchar(255) NOT NULL DEFAULT '' COMMENT 'SKU规格（冗余快照）',
                                        `sku_production_batch` varchar(255) NOT NULL DEFAULT '' COMMENT 'SKU生产批次（冗余快照）',

                                        `appearance_condition` tinyint(2) NOT NULL DEFAULT '1' COMMENT '外观情况',
                                        `has_bag` tinyint(2) NOT NULL DEFAULT '1' COMMENT '有无提袋',
                                        `base_recycle_price` decimal(10,2) NOT NULL COMMENT '回收价单价',
                                        `amount` decimal(10,2) NOT NULL COMMENT '回收价总价',
                                        `manual_amount` decimal(10,2) DEFAULT NULL COMMENT '人工调整后小计',
                                        `manual_remark` varchar(500) DEFAULT NULL COMMENT '人工调整备注',
                                        `quantity` int(11) NOT NULL COMMENT '数量',

                                        `update_time` bigint(20) NOT NULL COMMENT '更新时间',
                                        `create_time` bigint(20) NOT NULL COMMENT '创建时间',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_order_id` (`order_id`),
                                        KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='茶叶回收报价明细';

-- 已有库增量：人工改价字段（若列已存在请忽略报错）
ALTER TABLE `tea_quote_order` ADD COLUMN `total_manual_amount` decimal(10,2) DEFAULT NULL COMMENT '人工调整后总金额' AFTER `total_quantity`;
ALTER TABLE `tea_quote_order_item` ADD COLUMN `manual_amount` decimal(10,2) DEFAULT NULL COMMENT '人工调整后小计' AFTER `amount`;
ALTER TABLE `tea_quote_order_item` ADD COLUMN `manual_remark` varchar(500) DEFAULT NULL COMMENT '人工调整备注' AFTER `manual_amount`;

ALTER TABLE `tea_quote_order` ADD COLUMN `type` int(11) NOT NULL DEFAULT '100' COMMENT '订单类型 TEA_QUOTE_ORDER_TYPE' AFTER `order_no`;

INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (188, '茶叶报价单类型', 'TEA_QUOTE_ORDER_TYPE', 2, '100:自建,200:代建', '', '2026-05-10 12:00:00', '2026-05-10 12:00:00', 1, 1, 1);

-- 内部审核菜单（权限 business:tea:teaQuotationOrder:index）；若 id 冲突请调整
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (186, '报价单审核', 148, '[0],[148]', '/business/tea/teaQuotationOrder/index', 'business:tea:teaQuotationOrder:index', 'fa fa-check-square-o', 2, 4, '', '2026-05-09 12:00:00', '2026-05-09 12:00:00', 1, 1, 1);

-- 报单人下拉：角色标识 teaPartner（与 TeaQuotationOrderController.TEA_PARTNER_ROLE_NAME 一致）。请将外部报单用户关联到此角色。
INSERT INTO `sys_role` (`id`, `title`, `name`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (99, '茶叶合作报单', 'teaPartner', '外部报单用户', '2026-05-09 12:00:00', '2026-05-09 12:00:00', 1, 1, 1);