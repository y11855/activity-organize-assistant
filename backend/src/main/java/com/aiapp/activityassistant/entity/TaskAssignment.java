package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务分工
 */
@Data
@TableName("task_assignment")
public class TaskAssignment implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
