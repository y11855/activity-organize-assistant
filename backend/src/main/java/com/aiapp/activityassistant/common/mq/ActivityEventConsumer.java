package com.aiapp.activityassistant.common.mq;

import com.aiapp.activityassistant.service.DashboardService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 事件消费者：监听 Kafka 主题，异步刷新大屏缓存并记录实时动态。
 * 同一消费组内一条消息只会被一个实例消费，天然支持多实例水平扩展。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventConsumer {

    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = ActivityEventPublisher.TOPIC)
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            Map<String, String> value = objectMapper.readValue(
                    record.value(), new TypeReference<Map<String, String>>() {});
            Long ownerId = Long.valueOf(String.valueOf(value.get("ownerId")));
            dashboardService.recordEvent(ownerId,
                    String.valueOf(value.get("type")),
                    String.valueOf(value.get("content")));
        } catch (Exception e) {
            // 处理失败只记日志：ack-mode=record 仍会提交位点，避免坏消息反复阻塞（大屏数据可由 TTL 自动恢复）
            log.warn("[事件] 处理失败 partition={}, offset={}", record.partition(), record.offset(), e);
        }
    }
}
