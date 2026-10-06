-- ======================================================================
-- 社团/班级活动组织 AI 助手 - 数据库初始化
-- 可直接在 Navicat 中打开并执行；docker-compose 首次启动 MySQL 时也会自动执行
-- 字符集：utf8mb4；存储引擎：InnoDB
-- ======================================================================

CREATE DATABASE IF NOT EXISTS `activity_assistant`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `activity_assistant`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------
-- 用户表
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `username`    VARCHAR(64)  NOT NULL COMMENT '用户名（登录名）',
  `password`    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt 加密）',
  `nickname`    VARCHAR(64)  NULL COMMENT '昵称',
  `role`        VARCHAR(16)  NOT NULL DEFAULT 'member' COMMENT '角色: admin/leader/member',
  `create_time` DATETIME     NULL COMMENT '创建时间',
  `update_time` DATETIME     NULL COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

-- ----------------------------------------------------------------------
-- 活动表（聚合根）
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS `activity`;
CREATE TABLE `activity` (
  `id`             BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `creator_id`     BIGINT       NOT NULL COMMENT '创建人 ID（数据归属/权限控制）',
  `title`          VARCHAR(128) NOT NULL COMMENT '活动标题',
  `description`    TEXT         NULL COMMENT '活动描述',
  `start_time`     DATETIME     NULL COMMENT '活动开始时间',
  `end_time`       DATETIME     NULL COMMENT '活动结束时间',
  `location`       VARCHAR(128) NULL COMMENT '活动地点',
  `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0草稿 1策划中 2执行中 3已完成 4已取消',
  `plan_content`   LONGTEXT     NULL COMMENT 'AI 生成的策划方案',
  `review_content` LONGTEXT     NULL COMMENT 'AI 生成的复盘总结',
  `create_time`    DATETIME     NULL COMMENT '创建时间',
  `update_time`    DATETIME     NULL COMMENT '更新时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_status_time` (`status`, `start_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '活动';

-- ----------------------------------------------------------------------
-- 物料清单表（活动 1:N）
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS `material`;
CREATE TABLE `material` (
  `id`          BIGINT        NOT NULL COMMENT '主键（雪花 ID）',
  `activity_id` BIGINT        NOT NULL COMMENT '所属活动 ID',
  `name`        VARCHAR(64)   NOT NULL COMMENT '物料名称',
  `quantity`    INT           NULL COMMENT '数量',
  `unit`        VARCHAR(16)   NULL COMMENT '单位',
  `unit_price`  DECIMAL(10,2) NULL COMMENT '预估单价',
  `remark`      VARCHAR(255)  NULL COMMENT '备注',
  `create_time` DATETIME      NULL COMMENT '创建时间',
  `update_time` DATETIME      NULL COMMENT '更新时间',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料清单';

-- ----------------------------------------------------------------------
-- 问卷表（活动 1:N）
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS `questionnaire`;
CREATE TABLE `questionnaire` (
  `id`          BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `activity_id` BIGINT       NOT NULL COMMENT '所属活动 ID',
  `title`       VARCHAR(128) NOT NULL COMMENT '问卷标题',
  `description` VARCHAR(255) NULL COMMENT '问卷描述',
  `questions`   TEXT         NULL COMMENT '题目配置（JSON 数组）',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0未发布 1已发布 2已截止',
  `create_time` DATETIME     NULL COMMENT '创建时间',
  `update_time` DATETIME     NULL COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '报名问卷';

-- ----------------------------------------------------------------------
-- 问卷回答表（问卷 1:N，冗余 activity_id 便于按活动统计）
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS `questionnaire_response`;
CREATE TABLE `questionnaire_response` (
  `id`               BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `questionnaire_id` BIGINT       NOT NULL COMMENT '所属问卷 ID',
  `activity_id`      BIGINT       NOT NULL COMMENT '所属活动 ID（冗余，加速统计）',
  `respondent_id`    BIGINT       NULL COMMENT '填写人 ID',
  `respondent_name`  VARCHAR(64)  NULL COMMENT '填写人姓名',
  `answer`           TEXT         NULL COMMENT '回答内容（JSON）',
  `submit_time`      DATETIME     NULL COMMENT '提交时间',
  `create_time`      DATETIME     NULL COMMENT '创建时间',
  `update_time`      DATETIME     NULL COMMENT '更新时间',
  `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_questionnaire` (`questionnaire_id`),
  KEY `idx_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '问卷回答（报名记录）';

-- ----------------------------------------------------------------------
-- 任务分工表（活动 1:N）
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS `task_assignment`;
CREATE TABLE `task_assignment` (
  `id`            BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `activity_id`   BIGINT       NOT NULL COMMENT '所属活动 ID',
  `title`         VARCHAR(128) NOT NULL COMMENT '任务标题',
  `description`   VARCHAR(255) NULL COMMENT '任务描述',
  `assignee_id`   BIGINT       NULL COMMENT '负责人 ID',
  `assignee_name` VARCHAR(64)  NULL COMMENT '负责人姓名',
  `deadline`      DATETIME     NULL COMMENT '截止时间',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0待开始 1进行中 2已完成 3已逾期',
  `create_time`   DATETIME     NULL COMMENT '创建时间',
  `update_time`  DATETIME     NULL COMMENT '更新时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_activity` (`activity_id`),
  KEY `idx_assignee_status` (`assignee_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务分工';

-- ----------------------------------------------------------------------
-- 定时提醒表（活动 1:N）
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS `reminder`;
CREATE TABLE `reminder` (
  `id`              BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `activity_id`     BIGINT       NOT NULL COMMENT '所属活动 ID',
  `title`           VARCHAR(128) NOT NULL COMMENT '提醒标题',
  `content`         VARCHAR(512) NULL COMMENT '提醒内容',
  `target_user_id`  BIGINT       NULL COMMENT '目标用户 ID',
  `target_user_name` VARCHAR(64)  NULL COMMENT '目标用户姓名',
  `trigger_time`    DATETIME     NOT NULL COMMENT '触发时间',
  `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0待触发 1已发送 2已取消 3发送失败',
  `idempotent_key`  VARCHAR(64)  NOT NULL COMMENT '幂等键，避免重复发送',
  `retry_count`     INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `create_time`     DATETIME     NULL COMMENT '创建时间',
  `update_time`     DATETIME     NULL COMMENT '更新时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_trigger` (`status`, `trigger_time`),
  KEY `idx_activity` (`activity_id`),
  UNIQUE KEY `uk_idempotent` (`idempotent_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时提醒';

-- ----------------------------------------------------------------------
-- 工具调用日志表（幂等 + 重试追踪；同一幂等键可能有多条不同状态记录，故用普通索引）
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS `tool_call_log`;
CREATE TABLE `tool_call_log` (
  `id`             BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `activity_id`    BIGINT       NULL COMMENT '关联活动 ID',
  `tool_name`      VARCHAR(32)  NOT NULL COMMENT '工具名称: calendar/form/message',
  `idempotent_key` VARCHAR(64)  NOT NULL COMMENT '幂等键',
  `params`         TEXT         NULL COMMENT '调用参数（JSON）',
  `result`         TEXT         NULL COMMENT '调用结果（JSON）',
  `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0进行中 1成功 2失败',
  `retry_count`    INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `error_message` VARCHAR(512) NULL COMMENT '失败原因',
  `create_time`    DATETIME     NULL COMMENT '创建时间',
  `update_time`    DATETIME     NULL COMMENT '更新时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_idempotent_status` (`idempotent_key`, `status`),
  KEY `idx_activity` (`activity_id`),
  KEY `idx_status_create` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工具调用日志';

-- ----------------------------------------------------------------------
-- 初始化数据：默认管理员（admin / 123456，BCrypt 加密）
-- 固定 ID = 1，与 UserContext 未登录时的默认用户保持一致
-- ----------------------------------------------------------------------
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `create_time`, `update_time`, `deleted`)
VALUES (1, 'admin', '$2a$10$2GpwFmBnAa9WCxIvp0otQesHl39jYCAMG9YlYiuAezvH7R./dsw8a',
        '系统管理员', 'admin', NOW(), NOW(), 0);

SET FOREIGN_KEY_CHECKS = 1;
