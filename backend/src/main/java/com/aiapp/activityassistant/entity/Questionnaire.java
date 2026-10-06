package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报名问卷
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("questionnaire")
public class Questionnaire extends BaseEntity {

    private Long activityId;

    /** 问卷标题 */
    private String title;

    /** 问卷描述 */
    private String description;

    /** 题目配置（JSON 数组） */
    private String questions;

    /** 状态: 0未发布 1已发布 2已截止 */
    private Integer status;
}
