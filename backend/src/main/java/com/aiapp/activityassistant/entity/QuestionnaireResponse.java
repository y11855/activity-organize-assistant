package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 问卷回答（报名记录）
 */
@Data
@TableName("questionnaire_response")
public class QuestionnaireResponse implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long questionnaireId;

    private Long activityId;

    /** 填写人 ID */
    private Long respondentId;

    /** 填写人姓名 */
    private String respondentName;

    /** 回答内容（JSON） */
    private String answer;

    private LocalDateTime submitTime;

    @TableLogic
    private Integer deleted;
}
