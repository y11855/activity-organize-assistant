-- =============================================================
-- 升级脚本：日志管理系统
-- 日期：2026-11-28
-- 说明：
--   1. sys_log：统一日志表（操作 / 登录 / 接口 / 异常 / 审计），由 LogInterceptor 异步写入
--   2. 新增日志中心权限码 log:view、log:manage，仅授予系统管理员
-- 幂等：重复执行不会重复建表或重复授权
-- =============================================================

-- 1. 系统日志表
CREATE TABLE IF NOT EXISTS `sys_log`
(
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
    `log_type`       VARCHAR(16)  NOT NULL COMMENT '日志类型: operation操作 login登录 api接口 exception异常 audit审计',
    `module`         VARCHAR(32)  NULL COMMENT '业务模块中文名（按请求路径首段映射）',
    `operation`      VARCHAR(128) NULL COMMENT '操作描述（接口方法名）',
    `method`         VARCHAR(8)   NULL COMMENT 'HTTP 请求方法',
    `request_uri`    VARCHAR(512) NULL COMMENT '请求路径',
    `request_params` TEXT         NULL COMMENT '请求参数（已脱敏，截断 2000 字）',
    `response_body`  TEXT         NULL COMMENT '响应内容（截断 2000 字）',
    `user_id`        BIGINT       NULL COMMENT '操作人 ID（未登录为空）',
    `user_name`      VARCHAR(64)  NULL COMMENT '操作人用户名',
    `ip`             VARCHAR(64)  NULL COMMENT '客户端 IP',
    `client`         VARCHAR(255) NULL COMMENT '客户端 User-Agent',
    `status_code`    INT          NULL COMMENT 'HTTP 状态码',
    `duration`       BIGINT       NULL COMMENT '耗时（毫秒）',
    `success`        TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功: 1是 0否',
    `error_msg`      TEXT         NULL COMMENT '异常信息',
    `create_time`    DATETIME     NULL COMMENT '创建时间',
    `update_time`    DATETIME     NULL COMMENT '更新时间',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_type_time` (`log_type`, `create_time`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user` (`user_id`),
    KEY `idx_module` (`module`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '系统日志';

-- 2. 日志中心权限（查看 / 清理），归入「系统管理」分组
INSERT INTO `permission` (`id`, `code`, `name`, `group_name`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT 14, 'log:view', '日志中心', '系统管理', 50, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'log:view');

INSERT INTO `permission` (`id`, `code`, `name`, `group_name`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT 15, 'log:manage', '日志清理', '系统管理', 60, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'log:manage');

-- 3. 仅系统管理员可查看与清理日志
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `role` r
JOIN `permission` p
  ON p.`code` IN ('log:view', 'log:manage')
WHERE r.`code` = 'admin';
