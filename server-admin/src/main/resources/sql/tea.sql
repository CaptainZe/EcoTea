CREATE TABLE `materials`
(
    `id`          bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `name`        varchar(100)   NOT NULL DEFAULT '' COMMENT '商品名称',
    `description` varchar(1000)  NOT NULL DEFAULT '' COMMENT '商品描述',
    `brand`       varchar(100)   NOT NULL DEFAULT '' COMMENT '品牌',
    `year`        varchar(20)    NOT NULL DEFAULT '' COMMENT '年份',
    `price`       DECIMAL(10, 2) NOT NULL DEFAULT '0' COMMENT '价格',
    `operator`    varchar(255)   NOT NULL DEFAULT '' COMMENT '最后操作人',
    `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '更新时间',
    `create_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='物料管理';