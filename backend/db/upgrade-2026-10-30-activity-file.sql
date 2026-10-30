-- ======================================================================
-- 升级脚本：活动资料文件 + 分片上传（2026-10-30）
-- 在已有 activity_assistant 库上执行一次即可
-- ======================================================================

USE `activity_assistant`;

CREATE TABLE IF NOT EXISTS `activity_file` (
  `id`           BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
  `activity_id`  BIGINT       NOT NULL COMMENT '所属活动 ID',
  `uploader_id`  BIGINT       NOT NULL COMMENT '上传人 ID',
  `name`         VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `ext`          VARCHAR(32)  NULL COMMENT '扩展名（小写）',
  `size`         BIGINT       NOT NULL COMMENT '文件大小（字节）',
  `sha256`       CHAR(64)     NULL COMMENT '文件整体 SHA-256（校验 / ETag）',
  `storage_path` VARCHAR(512) NOT NULL COMMENT '相对上传根目录的存储路径',
  `create_time`  DATETIME     NULL COMMENT '创建时间',
  `update_time`  DATETIME     NULL COMMENT '更新时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '活动资料文件';
