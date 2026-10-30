-- ======================================================================
-- 升级脚本：活动文件增加「文件说明」（2026-10-30）
-- 幂等：字段已存在时跳过
-- ======================================================================

USE `activity_assistant`;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'activity_assistant'
    AND table_name = 'activity_file'
    AND column_name = 'description'
);
SET @ddl := IF(
  @col_exists = 0,
  'ALTER TABLE `activity_file` ADD COLUMN `description` VARCHAR(500) NULL COMMENT ''文件说明'' AFTER `name`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
