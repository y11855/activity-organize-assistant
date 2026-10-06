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

    /** 状态: 0草稿 1策划中 2执行中 3已完成 4已取消 */
    private Integer status;

    /** 活动策划内容（AI 生成） */
    private String planContent;

    /** 复盘总结内容（AI 生成） */
    private String reviewContent;
}
