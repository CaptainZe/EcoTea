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

CREATE TABLE daily_sequence (
                                `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `biz_type` int(11) NOT NULL COMMENT '业务类型',
                                `biz_date` varchar(32) NOT NULL COMMENT '业务日期 YYYYMMDD',
                                `current_seq` int NOT NULL COMMENT '当前序号',
                                `update_time` bigint(20) NOT NULL COMMENT '更新时间',
                                `create_time` bigint(20) NOT NULL COMMENT '创建时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `unique_key` (`biz_type`, `biz_date`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='当天自增号';