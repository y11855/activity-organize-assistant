package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MyBatis-Plus 实体：用户兴趣得分（对应表 user_recommendation）
 * <p>由 Flink 作业消费 Kafka 用户行为流实时写入，后端只读，用于生成推荐列表</p>
 */
@Data
@TableName("user_recommendation")
public class UserRecommendation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 活动属性标签编码，见 {@link com.aiapp.activityassistant.common.enums.ActivityTag} */
    private String tag;

    /** 兴趣得分：停留时长累加 + 点赞加权 */
    private BigDecimal score;

    private LocalDateTime updateTime;
}
