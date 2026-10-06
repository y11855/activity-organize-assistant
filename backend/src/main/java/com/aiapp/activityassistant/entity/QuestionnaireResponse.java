package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 问卷回答（报名记录）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("questionnaire_response")
public class QuestionnaireResponse extends BaseEntity {

    private Long questionnaireId;

    private Long activityId;

    /** 填写人 ID */
    private Long respondentId;

    /** 填写人姓名 */
    private String respondentName;

    /** 回答内容（JSON） */
    private String answer;

    /** 提交时间 */
    private LocalDateTime submitTime;
}
