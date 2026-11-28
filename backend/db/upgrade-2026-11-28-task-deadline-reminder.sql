-- =============================================================
-- 升级脚本：任务截止前自动提醒 + 分工通知全局标志
-- 日期：2026-11-28
-- 说明：
--   1. task_assignment.deadline_reminded：任务截止前 1 小时提醒是否已发送，避免重复提醒
--   2. activity.tasks_notified：任务分工是否已通知成员（0否 1是），全局置灰防重复通知
-- =============================================================

-- 1. 任务表：截止前提醒标记
ALTER TABLE `task_assignment`
    ADD COLUMN `deadline_reminded` TINYINT NOT NULL DEFAULT 0 COMMENT '截止前1小时是否已提醒: 0否 1是' AFTER `status`;

-- 2. 活动表：分工通知全局标志
ALTER TABLE `activity`
    ADD COLUMN `tasks_notified` TINYINT NOT NULL DEFAULT 0 COMMENT '任务分工是否已通知成员: 0否 1是' AFTER `draft_status`;
