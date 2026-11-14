package com.aiapp.activityassistant.common.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 事件发布者：业务动作完成后向 Redis Stream 投递消息。
 * 消息只驱动大屏缓存刷新与实时动态，不承载核心业务写入，避免 MQ 带来一致性问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventPublisher {

    public static final String STREAM_KEY = "activity:stream:events";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringRedisTemplate redisTemplate;

    /** ownerId：大屏数据归属人（活动创建人），为空表示无需推送 */
    public void publish(ActivityEventType type, Long ownerId, Long activityId, String content) {
        if (ownerId == null) {
            return;
        }
        Map<String, String> message = new HashMap<>();
        // 存中文描述，大屏实时动态可直接展示
        message.put("type", type.getDescription());
        message.put("ownerId", String.valueOf(ownerId));
        message.put("activityId", activityId == null ? "" : String.valueOf(activityId));
        message.put("content", content == null ? "" : content);
        message.put("time", LocalDateTime.now().format(TIME_FORMAT));
        try {
            redisTemplate.opsForStream().add(StreamRecords.newRecord().in(STREAM_KEY).ofMap(message));
        } catch (Exception e) {
            // 消息只影响大屏，投递失败不阻断业务
            log.warn("[事件] 投递失败 type={}, ownerId={}", type, ownerId, e);
        }
    }
}
