-- 升级脚本：为 RBAC 增加业务管理类权限，彻底区分 leader 与 member
-- 幂等：重复执行不会产生重复权限或重复授权

INSERT INTO `permission` (`id`, `code`, `name`, `group_name`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT 9, 'activity:manage', '活动管理操作', '活动管理', 15, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'activity:manage');

INSERT INTO `permission` (`id`, `code`, `name`, `group_name`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT 10, 'task:manage', '任务分工操作', '活动管理', 25, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'task:manage');

INSERT INTO `permission` (`id`, `code`, `name`, `group_name`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT 11, 'questionnaire:manage', '报名问卷操作', '活动管理', 35, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire:manage');

INSERT INTO `permission` (`id`, `code`, `name`, `group_name`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT 12, 'reminder:manage', '定时提醒操作', '活动管理', 45, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'reminder:manage');

INSERT INTO `permission` (`id`, `code`, `name`, `group_name`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT 13, 'file:manage', '活动文件管理', '活动管理', 50, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'file:manage');

-- admin 与 leader 获得全部业务管理权限；member 保持只读
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `role` r
JOIN `permission` p
  ON p.`code` IN ('activity:manage', 'task:manage', 'questionnaire:manage', 'reminder:manage', 'file:manage')
WHERE r.`code` IN ('admin', 'leader');
