-- ----------------------------------------------------------------------
-- 2026-10-25 增量升级：RBAC 权限系统（多角色）
--  1. 新建 role / permission / user_role / role_permission
--  2. user 增加 status（1启用 0禁用）
--  3. 内置角色与权限种子数据
--  4. 把旧 user.role 迁入 user_role，然后删除 user.role 列
--
-- 执行前请先停止后端（旧代码仍在读 user.role），并备份数据库。
-- 本脚本支持重复执行（建表/种子幂等，加列/删列前会判断是否存在）。
-- ----------------------------------------------------------------------

-- 1. 角色表
CREATE TABLE IF NOT EXISTS `role` (
  `id`          BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `code`        VARCHAR(32)  NOT NULL COMMENT '角色编码',
  `name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) NULL COMMENT '角色说明',
  `builtin`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否内置角色: 0否 1是（不可删除、编码不可改）',
  `create_time` DATETIME     NULL COMMENT '创建时间',
  `update_time` DATETIME     NULL COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色';

-- 2. 权限表
CREATE TABLE IF NOT EXISTS `permission` (
  `id`          BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `code`        VARCHAR(64)  NOT NULL COMMENT '权限编码',
  `name`        VARCHAR(64)  NOT NULL COMMENT '权限名称',
  `group_name`  VARCHAR(32)  NOT NULL COMMENT '权限分组（后台展示用）',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME     NULL COMMENT '创建时间',
  `update_time` DATETIME     NULL COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '权限';

-- 3. 用户-角色（联合主键，不做逻辑删除）
CREATE TABLE IF NOT EXISTS `user_role` (
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `role_id` BIGINT NOT NULL COMMENT '角色 ID',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户-角色';

-- 4. 角色-权限（联合主键，不做逻辑删除）
CREATE TABLE IF NOT EXISTS `role_permission` (
  `role_id`       BIGINT NOT NULL COMMENT '角色 ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限 ID',
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_permission` (`permission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色-权限';

-- 5. 内置角色
INSERT INTO `role` (`id`, `code`, `name`, `description`, `builtin`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'admin',   '系统管理员', '拥有全部权限，可管理用户与角色', 1, NOW(), NOW(), 0),
(2, 'leader',  '活动负责人', '可管理活动、任务、问卷与提醒', 1, NOW(), NOW(), 0),
(3, 'member',  '普通成员',   '可查看活动、任务、问卷与提醒', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `description` = VALUES(`description`), `builtin` = 1;

-- 6. 权限种子
INSERT INTO `permission` (`id`, `code`, `name`, `group_name`, `sort`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'activity:view',        '我的活动', '活动管理', 10, NOW(), NOW(), 0),
(2, 'task:view',            '任务分工', '活动管理', 20, NOW(), NOW(), 0),
(3, 'questionnaire:view',   '报名问卷', '活动管理', 30, NOW(), NOW(), 0),
(4, 'reminder:view',        '定时提醒', '活动管理', 40, NOW(), NOW(), 0),
(5, 'system:user:view',     '用户管理', '系统管理', 10, NOW(), NOW(), 0),
(6, 'system:user:manage',   '用户管理操作', '系统管理', 20, NOW(), NOW(), 0),
(7, 'system:role:view',     '角色权限', '系统管理', 30, NOW(), NOW(), 0),
(8, 'system:role:manage',   '角色权限操作', '系统管理', 40, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `group_name` = VALUES(`group_name`), `sort` = VALUES(`sort`);

-- 7. 角色-权限授权：admin 全集，leader/member 业务菜单
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
(2, 1), (2, 2), (2, 3), (2, 4),
(3, 1), (3, 2), (3, 3), (3, 4);

-- 8. user 增加 status（已存在则跳过）
SET @status_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'status');
SET @sql = IF(@status_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT ''账号状态: 1启用 0禁用'' AFTER `nickname`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 9. 把旧 user.role 迁入 user_role（仅当 role 列仍存在）
SET @role_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'role');
SET @sql = IF(@role_col_exists > 0,
    'INSERT IGNORE INTO `user_role` (`user_id`, `role_id`) SELECT u.id, r.id FROM `user` u JOIN `role` r ON r.code = u.role WHERE u.role IS NOT NULL AND u.role <> ''''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 10. 没有任何角色的用户兜底为普通成员
INSERT IGNORE INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, (SELECT id FROM `role` WHERE code = 'member' LIMIT 1)
FROM `user` u
WHERE NOT EXISTS (SELECT 1 FROM `user_role` ur WHERE ur.user_id = u.id);

-- 11. 删除旧 user.role 列（已删除则跳过）
SET @role_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'role');
SET @sql = IF(@role_col_exists > 0, 'ALTER TABLE `user` DROP COLUMN `role`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
