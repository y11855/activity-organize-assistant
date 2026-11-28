package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 实体：问卷点赞（对应表 questionnaire_like）
 * <p>取消点赞为物理删除，因此不继承 BaseEntity（无逻辑删除字段）</p>
 */
@Data
@TableName("questionnaire_like")
public class QuestionnaireLike {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 被点赞的问卷 ID */
    private Long questionnaireId;

    /** 所属活动 ID（冗余，便于按活动标签聚合兴趣） */
    private Long activityId;

    /** 点赞用户 ID */
    private Long userId;

    private LocalDateTime createTime;
}
