-- =============================================================
-- 升级脚本：活动属性标签 + 问卷点赞 + 用户兴趣推荐
-- 日期：2026-11-28
-- 说明：
--   1. activity.tag：活动属性标签（运动/艺术/学术/公益/社交/娱乐/其他），创建活动时必选
--   2. questionnaire_like：问卷点赞记录，同一用户对同一问卷只允许一条
--   3. user_recommendation：用户兴趣得分（由 Flink 作业消费 Kafka 行为流实时聚合写入）
-- =============================================================

-- 1. 活动表：属性标签
ALTER TABLE `activity`
    ADD COLUMN `tag` VARCHAR(32) NULL COMMENT '活动属性标签: sports运动 art艺术 academic学术 public_welfare公益 social社交 entertainment娱乐 other其他' AFTER `location`,
    ADD KEY `idx_tag` (`tag`);

-- 2. 问卷点赞表（物理删除，取消点赞即删记录，不参与逻辑删除）
CREATE TABLE IF NOT EXISTS `questionnaire_like`
(
    `id`               BIGINT   NOT NULL COMMENT '主键（雪花 ID）',
    `questionnaire_id` BIGINT   NOT NULL COMMENT '被点赞的问卷 ID',
    `activity_id`      BIGINT   NOT NULL COMMENT '所属活动 ID（冗余，推荐时按活动标签聚合）',
    `user_id`          BIGINT   NOT NULL COMMENT '点赞用户 ID',
    `create_time`      DATETIME NULL COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_questionnaire_user` (`questionnaire_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '问卷点赞';

-- 3. 用户兴趣得分表（Flink 作业输出，按用户 + 活动标签聚合）
CREATE TABLE IF NOT EXISTS `user_recommendation`
(
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `user_id`     BIGINT        NOT NULL COMMENT '用户 ID',
    `tag`         VARCHAR(32)   NOT NULL COMMENT '活动属性标签',
    `score`       DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '兴趣得分（停留时长 + 点赞加权）',
    `update_time` DATETIME      NULL COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tag` (`user_id`, `tag`),
    KEY `idx_user_score` (`user_id`, `score`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '用户兴趣得分（推荐依据）';
