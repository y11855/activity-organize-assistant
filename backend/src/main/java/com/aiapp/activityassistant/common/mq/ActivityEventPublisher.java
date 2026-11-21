package com.aiapp.activityassistant.common.mq;

import com.aiapp.activityassistant.service.DashboardService;
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
 * 事件发布者：业务动作完成后向 Kafka 投递消息。
 * 消息只驱动大屏缓存刷新与实时动态，不承载核心业务写入，避免 MQ 带来一致性问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventPublisher {

    public static final String TOPIC = "activity-events";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DashboardService dashboardService;

    /** ownerId：大屏数据归属人（活动创建人），为空表示无需推送 */
    public void publish(ActivityEventType type, Long ownerId, Long activityId, String content) {
        if (ownerId == null) {
            return;
        }
        Map<String, String> message = new LinkedHashMap<>();
        // 存中文描述，大屏实时动态可直接展示
        message.put("type", type.getDescription());
        message.put("ownerId", String.valueOf(ownerId));
        message.put("activityId", activityId == null ? "" : String.valueOf(activityId));
        message.put("content", content == null ? "" : content);
        message.put("time", LocalDateTime.now().format(TIME_FORMAT));
        try {
            String payload = objectMapper.writeValueAsString(message);
            // 以 ownerId 作为消息 key：同一归属人的事件落到同一分区，保证大屏动态顺序
            kafkaTemplate.send(TOPIC, String.valueOf(ownerId), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("[事件] 投递失败 type={}, ownerId={}", type, ownerId, ex);
                        }
                    });
        } catch (Exception e) {
            // Kafka 不可用（如本地未启动 broker）时直接本地刷新大屏，功能不受影响
            log.warn("[事件] Kafka 不可用，改为本地刷新大屏 type={}, ownerId={}", type, ownerId);
            dashboardService.recordEvent(ownerId, type.getDescription(), content);
        }
    }
}
