-- 社团/班级活动组织 AI 助手 - 数据库初始化
-- 由 docker-compose 启动 MySQL 时自动执行

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT       NOT NULL COMMENT '主键',
  `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
  `password`    VARCHAR(128) NOT NULL COMMENT '密码（加密）',
  `nickname`    VARCHAR(64)  NULL COMMENT '昵称',
  `role`        VARCHAR(16)  NOT NULL DEFAULT 'member' COMMENT '角色: admin/leader/member',
  `create_time` DATETIME     NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

-- 活动表
CREATE TABLE IF NOT EXISTS `activity` (
  `id`             BIGINT       NOT NULL,
  `creator_id`     BIGINT       NOT NULL COMMENT '创建人 ID（权限控制）',
  `title`          VARCHAR(128) NOT NULL,
  `description`    TEXT         NULL,
  `start_time`     DATETIME     NULL,
  `end_time`       DATETIME     NULL,
  `location`       VARCHAR(128) NULL,
  `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1策划中 2执行中 3已完成 4已取消',
  `plan_content`   LONGTEXT     NULL COMMENT 'AI 生成的策划',
  `review_content` LONGTEXT     NULL COMMENT 'AI 生成的复盘',
  `create_time`    DATETIME     NULL,
  `update_time`    DATETIME     NULL,
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`creator_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '活动';

-- 物料清单表
CREATE TABLE IF NOT EXISTS `material` (
  `id`          BIGINT       NOT NULL,
  `activity_id` BIGINT       NOT NULL,
  `name`        VARCHAR(64)  NOT NULL,
  `quantity`    INT          NULL,
  `unit`        VARCHAR(16)  NULL,
  `unit_price`  DECIMAL(8,2) NULL,
  `remark`      VARCHAR(255) NULL,
  `create_time` DATETIME     NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料清单';

-- 问卷表
CREATE TABLE IF NOT EXISTS `questionnaire` (
  `id`          BIGINT       NOT NULL,
  `activity_id` BIGINT       NOT NULL,
  `title`       VARCHAR(128) NOT NULL,
  `description` VARCHAR(255) NULL,
  `questions`   TEXT         NULL COMMENT '题目 JSON',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0未发布 1已发布 2已截止',
  `create_time` DATETIME     NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '报名问卷';

-- 问卷回答表
CREATE TABLE IF NOT EXISTS `questionnaire_response` (
  `id`               BIGINT   NOT NULL,
  `questionnaire_id` BIGINT   NOT NULL,
  `activity_id`      BIGINT   NOT NULL,
  `respondent_id`    BIGINT   NULL,
  `respondent_name`  VARCHAR(64) NULL,
  `answer`           TEXT     NULL COMMENT '回答 JSON',
  `submit_time`      DATETIME NULL,
  `deleted`          TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_questionnaire` (`questionnaire_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '问卷回答';

-- 任务分工表
CREATE TABLE IF NOT EXISTS `task_assignment` (
  `id`            BIGINT       NOT NULL,
  `activity_id`   BIGINT       NOT NULL,
  `title`         VARCHAR(128) NOT NULL,
  `description`   VARCHAR(255) NULL,
  `assignee_id`   BIGINT       NULL,
  `assignee_name` VARCHAR(64)  NULL,
  `deadline`      DATETIME     NULL,
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待开始 1进行中 2已完成 3已逾期',
  `create_time`   DATETIME     NULL,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务分工';

-- 定时提醒表
CREATE TABLE IF NOT EXISTS `reminder` (
  `id`              BIGINT       NOT NULL,
  `activity_id`     BIGINT       NOT NULL,
  `title`           VARCHAR(128) NOT NULL,
  `content`         VARCHAR(512) NULL,
  `target_user_id`  BIGINT       NULL,
  `target_user_name` VARCHAR(64) NULL,
  `trigger_time`    DATETIME     NOT NULL,
  `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0待触发 1已发送 2已取消 3发送失败',
  `idempotent_key`  VARCHAR(64)  NOT NULL COMMENT '幂等键，避免重复发送',
  `retry_count`     INT          NOT NULL DEFAULT 0,
  `create_time`     DATETIME     NULL,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_status_trigger` (`status`, `trigger_time`),
  UNIQUE KEY `uk_idempotent` (`idempotent_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时提醒';

-- 工具调用日志表（幂等 + 重试追踪）
CREATE TABLE IF NOT EXISTS `tool_call_log` (
  `id`             BIGINT       NOT NULL,
  `activity_id`    BIGINT       NULL,
  `tool_name`      VARCHAR(32)  NOT NULL COMMENT 'calendar/form/message',
  `idempotent_key` VARCHAR(64)  NOT NULL,
  `params`         TEXT         NULL,
  `result`         TEXT         NULL,
  `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0进行中 1成功 2失败',
  `retry_count`    INT          NOT NULL DEFAULT 0,
  `error_message`  VARCHAR(512) NULL,
  `create_time`    DATETIME     NULL,
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_idempotent_status` (`idempotent_key`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工具调用日志';

SET FOREIGN_KEY_CHECKS = 1;
