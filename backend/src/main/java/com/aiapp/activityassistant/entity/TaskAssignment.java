package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务分工
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_assignment")
public class TaskAssignment extends BaseEntity {

    private Long activityId;

    /** 任务标题 */
    private String title;

    /** 任务描述 */
    private String description;

    /** 负责人 ID */
    private Long assigneeId;

    /** 负责人姓名 */
    private String assigneeName;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 状态: 0待开始 1进行中 2已完成 3已逾期 */
    private Integer status;
}
