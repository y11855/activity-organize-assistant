package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 活动实体（聚合根）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity")
public class Activity extends BaseEntity {

    /** 创建人 ID（权限控制用，只能操作自己的活动） */
    private Long creatorId;

    private String title;

    private String description;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动地点 */
    private String location;

    /** 预计参与人数 */
    private Integer expectedCount;

    /** 选择的成员ID（JSON数组字符串） */
    private String memberIds;

    /** AI 生成项配置（JSON：tasks/materials/questionnaire/reminders） */
    private String generateOptions;

    /** 状态: 0草稿 1策划中 2执行中 3已完成 4已取消 5待确认 */
    private Integer status;

    /** 活动策划内容（AI 生成） */
    private String planContent;

    /** AI 生成的待确认草稿（JSON），确认发布后清空 */
    private String draftContent;

    /** 草稿状态: 0无 1待确认 2已确认发布 */
    private Integer draftStatus;

    /** 复盘总结内容（AI 生成） */
    private String reviewContent;
}
