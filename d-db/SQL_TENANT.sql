/*
  EcoTea 多租户 — 组织账号（当前阶段）
  产品约定：d-prd/PRD_TENANT.md

  本脚本仅建组织相关表，不改库存/仓库表（库存加 tenant_id 与重构后置）。

  - tenant：自增 id 忽略；业务键 tenant_id 手填；employee_quota；省/市必填；负责人档案字段
  - admin_tenant_scope：一人多店；tenant_id=0 为平台
  - employee / employee_tenant / employee_tenant_perm
  - UNIQUE INDEX 与 UNIQUE KEY 在 MySQL 中等价（与现有 chai_ 脚本写法一致）
  - 脚本不预置租户行

  MySQL 5.7+；执行前备份
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. tenant
-- ============================================================
CREATE TABLE IF NOT EXISTS `tenant` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '技术主键，业务不用',
  `tenant_id` bigint(20) UNSIGNED NOT NULL COMMENT '业务租户ID，创建时手填，唯一，勿改',
  `name` varchar(255) NOT NULL COMMENT '店铺名称',
  `status` tinyint(2) NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  `employee_quota` int(11) NOT NULL DEFAULT 0 COMMENT '本店启用员工名额上限；0=不可再挂启用员工；仅平台可改',
  `province` varchar(32) NOT NULL COMMENT '省编码(PCA)，必填',
  `city` varchar(32) NOT NULL COMMENT '市编码(PCA)，必填',
  `district` varchar(32) NOT NULL DEFAULT '' COMMENT '区县编码(PCA)，可选',
  `address` varchar(500) NOT NULL DEFAULT '' COMMENT '详细地址，可选',
  `contact_name` varchar(64) NOT NULL DEFAULT '' COMMENT '负责人姓名（档案，非登录账号）',
  `contact_mobile` varchar(32) NOT NULL DEFAULT '' COMMENT '负责人手机（档案）',
  `remark` varchar(500) NOT NULL DEFAULT '',
  `operator` varchar(255) NOT NULL DEFAULT '',
  `update_time` bigint(20) NOT NULL DEFAULT 0,
  `create_time` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_id`(`tenant_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺租户';

-- ============================================================
-- 2. employee（账密 + 预留微信绑定）
-- ============================================================
CREATE TABLE IF NOT EXISTS `employee` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL COMMENT '登录名',
  `password` varchar(128) NOT NULL DEFAULT '' COMMENT '加密密码',
  `salt` varchar(64) NOT NULL DEFAULT '',
  `real_name` varchar(64) NOT NULL DEFAULT '',
  `mobile` varchar(32) NOT NULL DEFAULT '',
  `wx_openid` varchar(64) NULL DEFAULT NULL COMMENT '小程序 openid；NULL=未绑定',
  `wx_unionid` varchar(64) NULL DEFAULT NULL COMMENT '可选',
  `wx_bind_time` bigint(20) NOT NULL DEFAULT 0 COMMENT '绑定时间毫秒；0未绑定',
  `status` tinyint(2) NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  `operator` varchar(255) NOT NULL DEFAULT '',
  `update_time` bigint(20) NOT NULL DEFAULT 0,
  `create_time` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  UNIQUE INDEX `uk_wx_openid`(`wx_openid`) USING BTREE,
  INDEX `idx_wx_unionid`(`wx_unionid`) USING BTREE,
  INDEX `idx_mobile`(`mobile`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工账号（小程序）';

CREATE TABLE IF NOT EXISTS `employee_tenant` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `employee_id` bigint(20) UNSIGNED NOT NULL,
  `tenant_id` bigint(20) UNSIGNED NOT NULL COMMENT 'tenant.tenant_id',
  `status` tinyint(2) NOT NULL DEFAULT 1,
  `operator` varchar(255) NOT NULL DEFAULT '',
  `update_time` bigint(20) NOT NULL DEFAULT 0,
  `create_time` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_emp_tenant`(`employee_id`, `tenant_id`) USING BTREE,
  INDEX `idx_tenant`(`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工所属店铺';

CREATE TABLE IF NOT EXISTS `employee_tenant_perm` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `employee_id` bigint(20) UNSIGNED NOT NULL,
  `tenant_id` bigint(20) UNSIGNED NOT NULL COMMENT 'tenant.tenant_id',
  `perm_code` varchar(32) NOT NULL COMMENT '代码枚举：SALE/RECYCLE/STOCK/FINANCE/SHOP',
  `operator` varchar(255) NOT NULL DEFAULT '',
  `update_time` bigint(20) NOT NULL DEFAULT 0,
  `create_time` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_emp_tenant_perm`(`employee_id`, `tenant_id`, `perm_code`) USING BTREE,
  INDEX `idx_tenant`(`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工在店铺下的权限码';

-- ============================================================
-- 3. admin_tenant_scope（一人多店）
-- ============================================================
CREATE TABLE IF NOT EXISTS `admin_tenant_scope` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT 'sys_user.id',
  `tenant_id` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=平台；>0=tenant.tenant_id',
  `operator` varchar(255) NOT NULL DEFAULT '',
  `update_time` bigint(20) NOT NULL DEFAULT 0,
  `create_time` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_tenant`(`user_id`, `tenant_id`) USING BTREE,
  INDEX `idx_tenant`(`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台用户数据范围（支持多店）';

SET FOREIGN_KEY_CHECKS = 1;

/*
  ---------------------------------------------------------------------------
  建店示例（应用侧校验 province/city 非空；负责人档案可选填）
  ---------------------------------------------------------------------------
  -- INSERT INTO tenant (
  --   tenant_id, name, status, employee_quota,
  --   province, city, district, address,
  --   contact_name, contact_mobile,
  --   remark, operator, update_time, create_time
  -- ) VALUES (
  --   1000, '总部店', 1, 50,
  --   '330000', '330100', '', '',
  --   '张三', '13800000000',
  --   '', 'system', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000
  -- );

  -- 平台超管数据范围（示例 user_id=1）
  -- INSERT INTO admin_tenant_scope (user_id, tenant_id, operator, update_time, create_time)
  -- VALUES (1, 0, 'system', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);

  -- 库存表加 tenant_id / 回填：不在本脚本；待库存重构与小程序库存专题一并做
*/
