-- 大学生软件实训教学 AI 检查评价系统 - 数据库初始化
-- 由 docker-compose 启动 MySQL 时自动执行

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT       NOT NULL COMMENT '主键',
  `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
  `password`    VARCHAR(128) NOT NULL COMMENT '密码（加密）',
  `nickname`    VARCHAR(64)  NULL COMMENT '昵称',
  `role`        VARCHAR(16)  NOT NULL DEFAULT 'student' COMMENT '角色: admin/teacher/student',
  `create_time` DATETIME     NULL,
  `update_time` DATETIME     NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

-- 学生表
CREATE TABLE IF NOT EXISTS `student` (
  `id`          BIGINT       NOT NULL,
  `user_id`     BIGINT       NULL COMMENT '关联用户',
  `student_no`  VARCHAR(32)  NULL COMMENT '学号',
  `class_name`  VARCHAR(64)  NULL COMMENT '班级',
  `create_time` DATETIME     NULL,
  `update_time` DATETIME     NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生';

-- 实训记录表
CREATE TABLE IF NOT EXISTS `training` (
  `id`          BIGINT       NOT NULL,
  `student_id`  BIGINT       NULL,
  `title`       VARCHAR(128) NOT NULL,
  `file_url`    VARCHAR(255) NULL COMMENT '成果文件 URL',
  `content`     LONGTEXT     NULL COMMENT '解析后的文本',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审 1AI审核中 2审核完成 3已评分',
  `ai_score`    DECIMAL(5,2) NULL COMMENT 'AI 评分',
  `teacher_score` DECIMAL(5,2) NULL COMMENT '教师评分',
  `final_score` DECIMAL(5,2) NULL COMMENT '综合评分',
  `ai_comment`  TEXT         NULL COMMENT 'AI 评价意见',
  `create_time` DATETIME     NULL,
  `update_time` DATETIME     NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '实训记录';

-- 评价指标表
CREATE TABLE IF NOT EXISTS `evaluation_metric` (
  `id`          BIGINT       NOT NULL,
  `name`        VARCHAR(64)  NOT NULL,
  `description` VARCHAR(255) NULL,
  `weight`      DECIMAL(4,2) NOT NULL DEFAULT 0.00 COMMENT '权重 0~1',
  `score_type`  VARCHAR(16)  NOT NULL DEFAULT 'ai' COMMENT '评分方式 ai/teacher',
  `create_time` DATETIME     NULL,
  `update_time` DATETIME     NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评价指标';

-- 默认评价指标
INSERT INTO `evaluation_metric` (`id`, `name`, `description`, `weight`, `score_type`) VALUES
  (1, '步骤完整性', '关键步骤是否齐全', 0.40, 'ai'),
  (2, '合规性',     '是否符合实训要求', 0.30, 'ai'),
  (3, '代码规范',   '代码风格', 0.15, 'teacher'),
  (4, '创新性',     '亮点加分', 0.15, 'teacher')
ON DUPLICATE KEY UPDATE `id` = `id`;

SET FOREIGN_KEY_CHECKS = 1;
