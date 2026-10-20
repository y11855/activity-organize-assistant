-- ----------------------------------------------------------------------
-- 2026-10-20 增量升级（第三批）：报名看板 + 活动结束自动通知 + AI智能复盘
--  1. 新建 activity_event_log：活动过程事件/关键节点/问题/亮点记录
--  2. activity 增加 feedback_notified / feedback_template_content
--  3. questionnaire 增加 type（1=报名问卷 2=反馈问卷）
--  4. questionnaire_response 增加 status（1=已报名 2=已取消）
-- ----------------------------------------------------------------------

-- 1. 活动事件日志（活动进行期间发起人记录的事件/亮点/问题）
CREATE TABLE IF NOT EXISTS `activity_event_log` (
  `id`          BIGINT       NOT NULL COMMENT '主键',
  `activity_id` BIGINT       NOT NULL COMMENT '活动 ID',
  `event_type`  TINYINT      NOT NULL DEFAULT 1 COMMENT '事件类型: 1普通事件 2关键节点 3问题 4亮点',
  `content`     TEXT         NOT NULL COMMENT '事件内容',
  `occurred_at` DATETIME     NOT NULL COMMENT '发生时间',
  `recorder_id` BIGINT       NULL COMMENT '记录人 ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_activity_time` (`activity_id`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动事件日志';

-- 2. activity：结束通知标记 + 自定义模板
ALTER TABLE `activity`
  ADD COLUMN `feedback_notified`        TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发送活动结束反馈通知: 0否 1是' AFTER `review_content`,
  ADD COLUMN `feedback_template_content` TEXT    NULL COMMENT '活动结束通知自定义模板内容' AFTER `feedback_notified`;

-- 3. questionnaire：区分报名/反馈
ALTER TABLE `questionnaire`
  ADD COLUMN `type` TINYINT NOT NULL DEFAULT 1 COMMENT '问卷类型: 1报名问卷 2活动反馈问卷' AFTER `activity_id`;

-- 4. questionnaire_response：报名状态
ALTER TABLE `questionnaire_response`
  ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '报名状态: 1已报名 2已取消' AFTER `answer`;
