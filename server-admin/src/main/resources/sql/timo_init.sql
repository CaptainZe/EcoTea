SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_action_log`;
CREATE TABLE `sys_action_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '日志名称',
  `type` tinyint(4) DEFAULT NULL COMMENT '日志类型',
  `ipaddr` varchar(255) DEFAULT NULL COMMENT '操作IP地址',
  `clazz` varchar(255) DEFAULT NULL COMMENT '产生日志的类',
  `method` varchar(255) DEFAULT NULL COMMENT '产生日志的方法',
  `model` varchar(255) DEFAULT NULL COMMENT '产生日志的表',
  `record_id` bigint(20) DEFAULT NULL COMMENT '产生日志的数据id',
  `message` text COMMENT '日志消息',
  `create_date` datetime DEFAULT NULL COMMENT '记录时间',
  `oper_name` varchar(255) DEFAULT NULL COMMENT '产生日志的用户昵称',
  `oper_by` bigint(20) DEFAULT NULL COMMENT '产生日志的用户',
  PRIMARY KEY (`id`),
  KEY `FK32gm4dja0jetx58r9dc2uljiu` (`oper_by`),
  CONSTRAINT `FK32gm4dja0jetx58r9dc2uljiu` FOREIGN KEY (`oper_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) DEFAULT NULL COMMENT '部门名称',
  `pid` bigint(20) DEFAULT NULL COMMENT '父级ID',
  `pids` varchar(255) DEFAULT NULL COMMENT '所有父级编号',
  `sort` int(11) DEFAULT NULL COMMENT '排序',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_date` datetime DEFAULT NULL COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建用户',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新用户',
  `status` tinyint(4) DEFAULT NULL COMMENT '状态（1:正常,2:冻结,3:删除）',
  PRIMARY KEY (`id`),
  KEY `FKifwd1h4ciusl3nnxrpfpv316u` (`create_by`),
  KEY `FK83g45s1cjqqfpifhulqhv807m` (`update_by`),
  CONSTRAINT `FK83g45s1cjqqfpifhulqhv807m` FOREIGN KEY (`update_by`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `FKifwd1h4ciusl3nnxrpfpv316u` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) DEFAULT NULL COMMENT '字典名称',
  `name` varchar(255) DEFAULT NULL COMMENT '字典键名',
  `type` tinyint(4) DEFAULT NULL COMMENT '字典类型',
  `value` text COMMENT '字典键值',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_date` datetime DEFAULT NULL COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建用户',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新用户',
  `status` tinyint(4) DEFAULT NULL COMMENT '状态（1:正常,2:冻结,3:删除）',
  PRIMARY KEY (`id`),
  KEY `FKag4shuprf2tjot9i1mhh37kk6` (`create_by`),
  KEY `FKoyng5jlifhsme0gc1lwiub0lr` (`update_by`),
  CONSTRAINT `FKag4shuprf2tjot9i1mhh37kk6` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `FKoyng5jlifhsme0gc1lwiub0lr` FOREIGN KEY (`update_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '文件名',
  `path` varchar(255) DEFAULT NULL COMMENT '文件路径',
  `mime` varchar(255) DEFAULT NULL COMMENT 'MIME文件类型',
  `size` bigint(20) DEFAULT NULL COMMENT '文件大小',
  `md5` varchar(255) DEFAULT NULL COMMENT 'MD5值',
  `sha1` varchar(255) DEFAULT NULL COMMENT 'SHA1值',
  `create_by` bigint(20) DEFAULT NULL COMMENT '上传者',
  `create_date` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `FKkkles8yp0a156p4247cc22ovn` (`create_by`),
  CONSTRAINT `FKkkles8yp0a156p4247cc22ovn` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) DEFAULT NULL COMMENT '菜单名称',
  `pid` bigint(20) DEFAULT NULL COMMENT '父级编号',
  `pids` varchar(255) DEFAULT NULL COMMENT '所有父级编号',
  `url` varchar(255) DEFAULT NULL COMMENT 'URL地址',
  `perms` varchar(255) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `type` tinyint(4) DEFAULT NULL COMMENT '类型（1:一级菜单,2:子级菜单,3:不是菜单）',
  `sort` int(11) DEFAULT NULL COMMENT '排序',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_date` datetime DEFAULT NULL COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建用户',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新用户',
  `status` tinyint(4) DEFAULT NULL COMMENT '状态（1:正常,2:冻结,3:删除）',
  PRIMARY KEY (`id`),
  KEY `FKoxg2hi96yr9pf2m0stjomr3we` (`create_by`),
  KEY `FKsiko0qcr8ddamvrxf1tk4opgc` (`update_by`),
  CONSTRAINT `FKoxg2hi96yr9pf2m0stjomr3we` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `FKsiko0qcr8ddamvrxf1tk4opgc` FOREIGN KEY (`update_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) DEFAULT NULL COMMENT '角色名称（中文名）',
  `name` varchar(255) DEFAULT NULL COMMENT '标识名称',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_date` datetime DEFAULT NULL COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建用户',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新用户',
  `status` tinyint(4) DEFAULT NULL COMMENT '状态（1:正常,2:冻结,3:删除）',
  PRIMARY KEY (`id`),
  KEY `FKdkwvv0rm6j3d5l6hwsy2dplol` (`create_by`),
  KEY `FKrouqqi3f2bgc5o83wdstlh6q4` (`update_by`),
  CONSTRAINT `FKdkwvv0rm6j3d5l6hwsy2dplol` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `FKrouqqi3f2bgc5o83wdstlh6q4` FOREIGN KEY (`update_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint(20) NOT NULL,
  `menu_id` bigint(20) NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`),
  KEY `FKf3mud4qoc7ayew8nml4plkevo` (`menu_id`),
  CONSTRAINT `FKf3mud4qoc7ayew8nml4plkevo` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`),
  CONSTRAINT `FKkeitxsgxwayackgqllio4ohn5` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(255) DEFAULT NULL COMMENT '用户名',
  `nickname` varchar(255) DEFAULT NULL COMMENT '用户昵称',
  `password` char(64) DEFAULT NULL COMMENT '密码',
  `salt` varchar(255) DEFAULT NULL COMMENT '密码盐',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `picture` varchar(255) DEFAULT NULL COMMENT '头像',
  `sex` tinyint(4) DEFAULT NULL COMMENT '性别（1:男,2:女）',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(255) DEFAULT NULL COMMENT '电话号码',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_date` datetime DEFAULT NULL COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL COMMENT '更新时间',
  `status` tinyint(4) DEFAULT NULL COMMENT '状态（1:正常,2:冻结,3:删除）',
  PRIMARY KEY (`id`),
  KEY `FKb3pkx0wbo6o8i8lj0gxr37v1n` (`dept_id`),
  CONSTRAINT `FKb3pkx0wbo6o8i8lj0gxr37v1n` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `FKhh52n8vd4ny9ff4x9fb8v65qx` (`role_id`),
  CONSTRAINT `FKb40xxfch70f5qnyfw8yme1n1s` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `FKhh52n8vd4ny9ff4x9fb8v65qx` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` (`id`, `title`, `pid`, `pids`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (1, '公司', 0, '[0]', 1, '', '2018-12-02 17:41:23', '2026-04-24 16:15:26', 1, 1, 1);
INSERT INTO `sys_dept` (`id`, `title`, `pid`, `pids`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (2, '事业部', 1, '[0],[1]', 1, '', '2018-12-02 17:51:04', '2026-04-24 16:16:31', 1, 1, 1);
INSERT INTO `sys_dept` (`id`, `title`, `pid`, `pids`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (3, '技术部', 1, '[0],[1]', 2, '', '2018-12-02 17:51:42', '2026-04-24 16:16:43', 1, 1, 1);
INSERT INTO `sys_dept` (`id`, `title`, `pid`, `pids`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (100, '商家', 0, '[0]', 2, '', '2026-04-24 16:19:13', '2026-04-24 16:19:13', 1, 1, 1);
INSERT INTO `sys_dept` (`id`, `title`, `pid`, `pids`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (102, '茶叶回收商', 100, '[0],[100]', 1, '', '2026-04-24 16:19:28', '2026-04-24 16:19:28', 1, 1, 1);

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (1, '数据状态', 'DATA_STATUS', 2, '1:正常,2:冻结,3:删除', '', '2018-10-05 16:03:11', '2018-10-05 16:11:41', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (2, '字典类型', 'DICT_TYPE', 2, '1:动态SQL,2:键值对', '', '2023-08-19 10:17:47', '2023-08-19 10:17:47', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (3, '用户性别', 'USER_SEX', 2, '1:男,2:女', '', '2018-10-05 20:12:32', '2018-10-05 20:12:32', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (4, '菜单类型', 'MENU_TYPE', 2, '1:目录,2:菜单,3:按钮', '', '2018-10-05 20:24:57', '2019-11-06 20:08:46', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (5, '搜索栏状态', 'SEARCH_STATUS', 2, '1:正常,2:冻结', '', '2018-10-05 20:25:45', '2019-02-26 00:34:34', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (6, '日志类型', 'LOG_TYPE', 2, '1:业务,2:登录,3:系统', '', '2018-10-05 20:28:47', '2019-02-26 00:31:43', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (100, '是否枚举', 'YES_OR_NO', 2, '0:否,1:是', '', '2026-02-28 17:15:25', '2026-02-28 17:15:25', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (102, 'App密钥 - 类型', 'APP_SECRET_KEY_TYPE', 2, '1:阿里云OSS', '', '2026-04-21 09:59:25', '2026-04-21 09:59:25', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (104, 'App公告 - 类型', 'APP_NOTICE_TYPE', 2, '1:茶类公告', '', '2026-04-24 10:40:45', '2026-04-24 10:40:45', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (106, 'SKU - 状态', 'SKU_STATUS', 2, '1:上架,2:下架', '', '2026-04-23 09:36:53', '2026-04-23 09:36:53', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (150, '茶叶 - 品牌', 'TEA_BRAND', 2, '1:中茶,2:八马,3:华祥苑,4:大益,5:天福茗茶,6:小罐茶,7:正山堂,8:武夷星,9:曦瓜,10:孝文家茶,11:日春,12:溪谷留香,13:瑞泉,14:叶福新,15:岩霸,16:熹茗,17:宽庐,18:品品香,19:青悠谷,20:春丘大叶,21:北岩,22:手尚工夫,23:心头肉', '', '2026-04-22 17:27:29', '2026-04-23 09:35:48', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (151, '茶叶 - 种类', 'TEA_TYPE', 2, '1:白茶,2:绿茶,3:红茶,4:青茶,5:黑茶,6:黄茶', '', '2026-04-22 17:29:32', '2026-04-23 09:33:05', 1, 1, 1);
INSERT INTO `sys_dict` (`id`, `title`, `name`, `type`, `value`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (152, '茶叶 - 等级', 'TEA_GRADE', 2, '0:无,1:特级,2:一级', '', '2026-04-23 09:38:08', '2026-04-23 09:38:08', 1, 1, 1);

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (1, '菜单管理', 2, '[0],[2]', '/system/menu/index', 'system:menu:index', '', 2, 4, '子级菜单', '2018-09-29 00:02:10', '2026-04-21 19:50:25', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (2, '系统管理', 0, '[0]', '#', '#', 'fa fa-cog', 1, 3, '一级菜单', NULL, '2026-04-23 15:08:54', NULL, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (3, '添加', 1, '[0],[2],[1]', '/system/menu/add', 'system:menu:add', '', 3, 1, '不是菜单', '2018-09-29 00:06:57', '2019-10-29 16:50:10', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (4, '角色管理', 2, '[0],[2]', '/system/role/index', 'system:role:index', '', 2, 3, '子级菜单', '2018-09-29 00:08:14', '2026-04-21 19:50:25', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (5, '添加', 4, '[0],[2],[4]', '/system/role/add', 'system:role:add', '', 3, 1, '不是菜单', '2018-09-29 00:09:04', '2019-10-29 16:50:10', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (6, '概览', 0, '[0]', '/index', 'index', 'layui-icon layui-icon-home', 1, 2, '一级菜单', NULL, '2026-04-23 15:59:11', NULL, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (7, '系统用户管理', 2, '[0],[2]', '/system/user/index', 'system:user:index', '', 2, 1, '子级菜单', NULL, '2019-11-05 17:47:36', NULL, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (8, '编辑', 1, '[0],[2],[1]', '/system/menu/edit', 'system:menu:edit', '', 3, 2, '不是菜单', '2018-09-29 00:57:33', '2019-10-29 16:50:11', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (9, '详细', 1, '[0],[2],[1]', '/system/menu/detail', 'system:menu:detail', '', 3, 3, '不是菜单', '2018-09-29 01:03:00', '2019-10-29 16:50:11', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (10, '修改状态', 1, '[0],[2],[1]', '/system/menu/status', 'system:menu:status', '', 3, 4, '不是菜单', '2018-09-29 01:03:43', '2019-10-29 16:50:12', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (11, '编辑', 4, '[0],[2],[4]', '/system/role/edit', 'system:role:edit', '', 3, 2, '不是菜单', '2018-09-29 01:06:13', '2019-10-29 16:50:11', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (12, '授权', 4, '[0],[2],[4]', '/system/role/auth', 'system:role:auth', '', 3, 3, '不是菜单', '2018-09-29 01:06:57', '2019-10-29 16:50:11', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (13, '详细', 4, '[0],[2],[4]', '/system/role/detail', 'system:role:detail', '', 3, 4, '不是菜单', '2018-09-29 01:08:00', '2019-10-29 16:50:12', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (14, '修改状态', 4, '[0],[2],[4]', '/system/role/status', 'system:role:status', '', 3, 5, '不是菜单', '2018-09-29 01:08:22', '2019-10-29 16:50:12', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (15, '编辑', 7, '[0],[2],[7]', '/system/user/edit', 'system:user:edit', '', 3, 2, '不是菜单', '2018-09-29 21:17:17', '2019-10-29 16:50:11', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (16, '添加', 7, '[0],[2],[7]', '/system/user/add', 'system:user:add', '', 3, 1, '不是菜单', '2018-09-29 21:17:58', '2019-10-29 16:50:10', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (17, '修改密码', 7, '[0],[2],[7]', '/system/user/pwd', 'system:user:pwd', '', 3, 3, '不是菜单', '2018-09-29 21:19:40', '2019-10-29 16:50:11', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (18, '权限分配', 7, '[0],[2],[7]', '/system/user/role', 'system:user:role', '', 3, 4, '不是菜单', '2018-09-29 21:20:35', '2019-10-29 16:50:12', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (19, '详细', 7, '[0],[2],[7]', '/system/user/detail', 'system:user:detail', '', 3, 5, '不是菜单', '2018-09-29 21:21:00', '2019-10-29 16:50:12', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (20, '修改状态', 7, '[0],[2],[7]', '/system/user/status', 'system:user:status', '', 3, 6, '不是菜单', '2018-09-29 21:21:18', '2019-10-29 16:50:12', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (21, '字典管理', 2, '[0],[2]', '/system/dict/index', 'system:dict:index', '', 2, 5, '子级菜单', '2018-10-05 00:55:52', '2026-04-21 19:50:25', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (22, '字典添加', 21, '[0],[2],[21]', '/system/dict/add', 'system:dict:add', '', 3, 1, '不是菜单', '2018-10-05 00:56:26', '2019-10-29 16:50:10', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (23, '字典编辑', 21, '[0],[2],[21]', '/system/dict/edit', 'system:dict:edit', '', 3, 2, '不是菜单', '2018-10-05 00:57:08', '2019-10-29 16:50:11', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (24, '字典详细', 21, '[0],[2],[21]', '/system/dict/detail', 'system:dict:detail', '', 3, 3, '不是菜单', '2018-10-05 00:57:42', '2019-10-29 16:50:11', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (25, '修改状态', 21, '[0],[2],[21]', '/system/dict/status', 'system:dict:status', '', 3, 4, '不是菜单', '2018-10-05 00:58:27', '2019-10-29 16:50:12', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (26, '行为日志', 2, '[0],[2]', '/system/actionLog/index', 'system:actionLog:index', '', 2, 6, '子级菜单', '2018-10-14 16:52:01', '2026-04-21 19:50:25', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (27, '日志详细', 26, '[0],[2],[26]', '/system/actionLog/detail', 'system:actionLog:detail', '', 3, 1, '不是菜单', '2018-10-14 21:07:11', '2019-10-29 16:50:10', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (28, '日志删除', 26, '[0],[2],[26]', '/system/actionLog/delete', 'system:actionLog:delete', '', 3, 2, '不是菜单', '2018-10-14 21:08:17', '2019-10-29 16:50:11', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (148, '茶叶品类', 0, '[0]', '#', '#', 'fa fa-leaf', 1, 5, '', '2026-03-02 14:23:52', '2026-04-23 15:08:54', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (152, '业务管理', 0, '[0]', '#', '#', 'fa fa-codepen', 1, 4, '', '2026-04-21 09:57:29', '2026-04-23 15:08:54', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (154, '密钥管理', 152, '[0],[152]', '/business/sys/appSecretKey/index', 'business:sys:appSecretKey:index', '', 2, 1, '', '2026-04-21 09:57:51', '2026-04-21 09:57:51', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (156, '编辑', 154, '[0],[152],[154]', '#', 'business:sys:appSecretKey:edit', '', 3, 1, '', '2026-04-21 09:58:45', '2026-04-21 09:58:45', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (158, '部门管理', 2, '[0],[2]', '/system/dept/index', 'system:dept:index', '', 2, 2, '', '2026-04-21 19:50:25', '2026-04-21 19:50:25', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (160, '添加', 158, '[0],[2],[158]', '/system/dept/add', 'system:dept:add', '', 3, 1, '', '2026-04-21 19:57:36', '2026-04-21 19:57:36', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (162, '编辑', 158, '[0],[2],[158]', '/system/dept/edit', 'system:dept:edit', '', 3, 2, '', '2026-04-21 19:57:49', '2026-04-21 19:57:49', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (164, '详细', 158, '[0],[2],[158]', '/system/dept/detail', 'system:dept:detail', '', 3, 3, '', '2026-04-21 19:58:01', '2026-04-21 19:58:01', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (166, '改变状态', 158, '[0],[2],[158]', '/system/dept/status', 'system:dept:status', '', 3, 4, '', '2026-04-21 19:58:14', '2026-04-21 19:58:14', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (168, '茶叶管理', 148, '[0],[148]', '/business/tea/teaSku/index', 'business:tea:teaSku:index', '', 2, 2, '', '2026-04-22 17:22:46', '2026-04-22 17:22:46', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (170, '编辑', 168, '[0],[148],[168]', '#', 'business:tea:teaSku:edit', '', 3, 1, '', '2026-04-23 13:38:48', '2026-04-23 13:38:48', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (172, '删除', 168, '[0],[148],[168]', '#', 'business:tea:teaSku:delete', '', 3, 2, '', '2026-04-23 13:39:04', '2026-04-23 13:39:04', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (176, '登录', 0, '[0]', '#', 'index', '', 3, 1, '', '2026-04-23 15:34:19', '2026-04-23 15:34:19', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (178, '报价查询', 0, '[0]', '/business/tea/teaQuotation/index', 'business:tea:teaQuotation:index', 'fa fa-leaf', 1, 7, '', '2026-04-24 08:43:20', '2026-04-24 12:32:53', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (180, '公告管理', 152, '[0],[152]', '/business/sys/appNotice/index', 'business:sys:appNotice:index', '', 2, 2, '', '2026-04-24 10:50:59', '2026-04-24 10:50:59', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (182, '编辑', 180, '[0],[152],[180]', '#', 'business:sys:appNotice:edit', '', 3, 1, '', '2026-04-24 10:51:17', '2026-04-24 10:51:17', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `title`, `pid`, `pids`, `url`, `perms`, `icon`, `type`, `sort`, `remark`, `create_date`, `update_date`, `create_by`, `update_by`, `status`) VALUES (184, '报价必读', 0, '[0]', '/business/tea/teaQuotation/readme', 'business:tea:teaQuotation:index', 'fa fa-book', 1, 6, '', '2026-04-24 12:32:53', '2026-04-24 12:35:34', 1, 1, 1);
    
-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '管理员', 'admin', '', '2018-09-29 00:12:40', '2023-08-19 14:33:16', 1, 1, 1);

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '超级管理员', '3dd0affe1e514fa059d00bf63134fe48d45acd3f350aaed83b0b54ef05579092', '3ABR79', 2, NULL, '2', '10086444444@163.com', '111111', '11111111111111', NULL, '2019-11-05 15:01:42', 1);

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);

SET FOREIGN_KEY_CHECKS = 1;