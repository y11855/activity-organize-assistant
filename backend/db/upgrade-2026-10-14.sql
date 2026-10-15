-- ======================================================================
-- 升级脚本 2026-10-14：问卷封面 / 活动人数与成员 / 站内消息 / 测试用户
-- 在 activity_assistant 库上执行（Navicat 或 mysql 客户端均可）
-- ======================================================================

USE `activity_assistant`;
SET NAMES utf8mb4;

-- 1. 问卷封面图
ALTER TABLE `questionnaire`
    ADD COLUMN `cover_image` VARCHAR(512) NULL COMMENT '封面图URL' AFTER `description`;

-- 2. 活动：预计人数 + 选择的成员
ALTER TABLE `activity`
    ADD COLUMN `expected_count` INT NULL COMMENT '预计参与人数' AFTER `location`,
    ADD COLUMN `member_ids` VARCHAR(512) NULL COMMENT '选择的成员ID（JSON数组）' AFTER `expected_count`;

-- 3. 站内消息表
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id`          BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `user_id`     BIGINT       NOT NULL COMMENT '接收人 ID',
  `title`       VARCHAR(128) NOT NULL COMMENT '消息标题',
  `content`     TEXT         NULL COMMENT '消息内容',
  `activity_id` BIGINT       NULL COMMENT '关联活动 ID',
  `read_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `create_time` DATETIME     NULL COMMENT '创建时间',
  `update_time` DATETIME     NULL COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `read_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内消息';

-- 4. 测试用户（密码均为 123456，与 admin 相同哈希）
--    id 2/3/4 与既有 task_assignment 测试数据中的 assignee_id 对齐
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `create_time`, `update_time`, `deleted`) VALUES
(2, 'zhangsan', '$2a$10$2GpwFmBnAa9WCxIvp0otQesHl39jYCAMG9YlYiuAezvH7R./dsw8a', '张三', 'member', NOW(), NOW(), 0),
(3, 'lisi',     '$2a$10$2GpwFmBnAa9WCxIvp0otQesHl39jYCAMG9YlYiuAezvH7R./dsw8a', '李四', 'member', NOW(), NOW(), 0),
(4, 'wangwu',   '$2a$10$2GpwFmBnAa9WCxIvp0otQesHl39jYCAMG9YlYiuAezvH7R./dsw8a', '王五', 'member', NOW(), NOW(), 0),
(5, 'zhaoliu',  '$2a$10$2GpwFmBnAa9WCxIvp0otQesHl39jYCAMG9YlYiuAezvH7R./dsw8a', '赵六', 'member', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`);
