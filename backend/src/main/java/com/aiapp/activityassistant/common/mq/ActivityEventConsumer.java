package com.aiapp.activityassistant.common.mq;

import com.aiapp.activityassistant.service.DashboardService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 事件消费者：以消费者组方式读取 Redis Stream，异步刷新大屏缓存并记录实时动态。
 * 多实例部署时同组内消息只会被一个实例消费。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventConsumer {

    private static final String GROUP = "cg:dashboard";
    private static final int BATCH_SIZE = 20;

    private final StringRedisTemplate redisTemplate;
    private final DashboardService dashboardService;

    private final String consumerName = "consumer-" + UUID.randomUUID().toString().substring(0, 8);

    @PostConstruct
    public void init() {
        ensureGroup();
    }

    @Scheduled(fixedDelay = 2000)
    public void consume() {
        StreamOperations<String, Object, Object> ops = redisTemplate.opsForStream();
        List<MapRecord<String, Object, Object>> records;
        try {
            records = ops.read(Consumer.from(GROUP, consumerName),
                    StreamReadOptions.empty().count(BATCH_SIZE),
                    StreamOffset.create(ActivityEventPublisher.STREAM_KEY, ReadOffset.lastConsumed()));
        } catch (Exception e) {
            // Redis 重启后消费者组会丢失，重建一次后下一轮继续消费
            if (e.getMessage() != null && e.getMessage().contains("NOGROUP")) {
                ensureGroup();
            } else {
                log.warn("[事件] 读取失败", e);
            }
            return;
        }
        if (records == null) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            Map<Object, Object> value = record.getValue();
            try {
                Long ownerId = Long.valueOf(String.valueOf(value.get("ownerId")));
                dashboardService.recordEvent(ownerId,
                        String.valueOf(value.get("type")),
                        String.valueOf(value.get("content")));
            } catch (Exception e) {
                log.warn("[事件] 处理失败 id={}", record.getId(), e);
            }
            // 处理失败也确认，避免坏消息反复阻塞队列（大屏数据可由 TTL 自动恢复）
            ops.acknowledge(ActivityEventPublisher.STREAM_KEY, GROUP, record.getId());
        }
        if (!records.isEmpty()) {
            // 裁剪历史消息，防止 Stream 无限增长
            ops.trim(ActivityEventPublisher.STREAM_KEY, 1000, true);
        }
    }

    private void ensureGroup() {
        try {
            redisTemplate.opsForStream().createGroup(ActivityEventPublisher.STREAM_KEY, ReadOffset.from("0"), GROUP);
            log.info("[事件] 消费者组已创建 group={}", GROUP);
        } catch (Exception e) {
            // BUSYGROUP：组已存在，属正常情况
            log.debug("[事件] 消费者组已存在 group={}", GROUP);
        }
    }
}
