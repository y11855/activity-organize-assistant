package com.aiapp.activityassistant.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户行为采集生产者：把「问卷页停留」与「点赞」写入 Kafka，供 Flink 实时聚合兴趣得分。
 * <p>Kafka 不可用时只记日志丢弃该条行为，绝不影响报名、点赞等主流程。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBehaviorPublisher {

    /** 行为主题：Flink 作业订阅该主题计算推荐 */
    public static final String TOPIC = "user-behavior";
    public static final String TYPE_VIEW = "view";
    public static final String TYPE_LIKE = "like";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 上报一条用户行为
     *
     * @param userId          用户 ID
     * @param questionnaireId 问卷 ID
     * @param activityId      问卷所属活动 ID
     * @param tag             活动属性标签（推荐维度）
     * @param type            view / like
     * @param durationSeconds 停留秒数（仅 view 有效）
     */
    public void publish(Long userId, Long questionnaireId, Long activityId, String tag, String type,
            Integer durationSeconds) {
        if (userId == null) {
            return;
        }
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("userId", userId);
        message.put("questionnaireId", questionnaireId);
        message.put("activityId", activityId);
        message.put("tag", tag == null ? "" : tag);
        message.put("type", type);
        message.put("duration", durationSeconds == null ? 0 : durationSeconds);
        message.put("time", LocalDateTime.now().format(TIME_FORMAT));
        try {
            String payload = objectMapper.writeValueAsString(message);
            // 以 userId 作为消息 key：同一用户的行为落到同一分区，Flink 侧聚合更均衡
            kafkaTemplate.send(TOPIC, String.valueOf(userId), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("[行为] 投递失败 userId={}, type={}", userId, type, ex);
                        }
                    });
        } catch (Exception e) {
            log.warn("[行为] Kafka 不可用，丢弃本次行为 userId={}, type={}", userId, type);
        }
    }
}
