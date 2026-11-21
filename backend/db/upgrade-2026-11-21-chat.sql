-- ======================================================================
-- 活动群聊消息表（2026-11-21）
-- 已有数据库执行本脚本；全新初始化直接执行 init.sql 即可（已包含该表）
-- ======================================================================

USE `activity_assistant`;

DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id`          BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `activity_id` BIGINT       NOT NULL COMMENT '活动 ID',
  `sender_id`   BIGINT       NOT NULL COMMENT '发送人 ID',
  `sender_name` VARCHAR(64)  NOT NULL COMMENT '发送人昵称（冗余存储）',
  `content`     VARCHAR(500) NOT NULL COMMENT '消息内容',
  `create_time` DATETIME     NULL COMMENT '创建时间',
  `update_time` DATETIME     NULL COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_activity_time` (`activity_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '活动群聊消息';
