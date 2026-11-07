-- 邮箱验证码登录：user 表新增 email 字段与唯一索引
-- 幂等脚本：重复执行不会报错

SET @db := DATABASE();

-- 1. 新增 email 列
SET @has_col := (SELECT COUNT(*)
                 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = @db
                   AND TABLE_NAME = 'user'
                   AND COLUMN_NAME = 'email');
SET @sql := IF(@has_col = 0,
               'ALTER TABLE `user` ADD COLUMN `email` VARCHAR(120) NULL COMMENT ''邮箱（用于验证码登录）'' AFTER `nickname`',
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 新增唯一索引（MySQL 唯一索引允许多个 NULL，未绑定邮箱的账号不受影响）
SET @has_idx := (SELECT COUNT(*)
                 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = @db
                   AND TABLE_NAME = 'user'
                   AND INDEX_NAME = 'uk_email');
SET @sql := IF(@has_idx = 0,
               'ALTER TABLE `user` ADD UNIQUE KEY `uk_email` (`email`)',
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
