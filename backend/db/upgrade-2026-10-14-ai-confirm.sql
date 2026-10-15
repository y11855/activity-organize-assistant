-- ----------------------------------------------------------------------
-- 2026-10-14 增量升级（第二批）：AI 生成内容配置 + 草稿确认流程
--  1. activity 增加 AI 生成草稿相关字段
--  2. status 语义扩展：新增 5=待确认（AI 已生成，等待发起人确认发布）
-- 执行方式：
--   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -proot123 --default-character-set=utf8mb4 activity_assistant -e "source xxx.sql"
-- ----------------------------------------------------------------------

ALTER TABLE `activity`
  ADD COLUMN `generate_options` VARCHAR(255) NULL COMMENT 'AI 生成项配置（JSON：tasks/materials/questionnaire/reminders）' AFTER `member_ids`,
  ADD COLUMN `draft_content`    LONGTEXT     NULL COMMENT 'AI 生成的待确认草稿（JSON）' AFTER `plan_content`,
  ADD COLUMN `draft_status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '草稿状态: 0无 1待确认 2已确认发布' AFTER `draft_content`;

ALTER TABLE `activity`
  MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0草稿 1策划中 2执行中 3已完成 4已取消 5待确认';
