package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报名问卷
 */
@Data
@TableName("questionnaire")
public class Questionnaire implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long activityId;

    /** 问卷标题 */
    private String title;

    /** 问卷描述 */
    private String description;

    /** 题目配置（JSON 数组） */
    private String questions;

    /** 状态: 0未发布 1已发布 2已截止 */
    private Integer status;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
