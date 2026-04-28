CREATE TABLE `app_secret_key` (
                                  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `type` int(11) NOT NULL DEFAULT '1' COMMENT '类型',
                                  `access_key` varchar(1000) NOT NULL DEFAULT '' COMMENT 'AccessKey',
                                  `access_secret` varchar(1000) NOT NULL DEFAULT '' COMMENT 'AccessSecret',
                                  `remark` varchar(1000) NOT NULL DEFAULT '' COMMENT '备注',
                                  `operator` varchar(255) NOT NULL DEFAULT '' COMMENT '最后操作人',
                                  `update_time` bigint(20) NOT NULL COMMENT '更新时间',
                                  `create_time` bigint(20) NOT NULL COMMENT '创建时间',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `unique_key` (`type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='APP密钥管理';

CREATE TABLE `app_notice` (
                              `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                              `type` int(11) NOT NULL DEFAULT '1' COMMENT '类型',
                              `content` text COMMENT '公告内容',
                              `operator` varchar(255) NOT NULL DEFAULT '' COMMENT '最后操作人',
                              `update_time` bigint(20) NOT NULL COMMENT '更新时间',
                              `create_time` bigint(20) NOT NULL COMMENT '创建时间',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `unique_key` (`type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='App公告';

CREATE TABLE `tea_sku` (
                           `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                           `sku_code` varchar(64) NOT NULL COMMENT 'SKU唯一编码',
                           `name` varchar(255) NOT NULL COMMENT '商品名称',
                           `brand` int(11) NOT NULL COMMENT '品牌',
                           `type` int(11) NOT NULL COMMENT '茶类',
                           `grade` int(11) NOT NULL COMMENT '等级',
                           `spec` varchar(255) NOT NULL DEFAULT '' COMMENT '规格',
                           `year` int(11) NOT NULL COMMENT '年份',
                           `production_batch` varchar(255) NOT NULL DEFAULT '' COMMENT '生产批次',
                           `expiration` int(11) NOT NULL COMMENT '保质期',
                           `barcode` varchar(255) NOT NULL DEFAULT '' COMMENT '条形码',
                           `image_urls` text COMMENT '图片列表',
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