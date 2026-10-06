package com.aiapp.activityassistant.schedule;

import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 提醒任务调度器
 * 每分钟扫描一次到期提醒并触发；Redis 锁保证多实例部署 / 上一轮未结束时不重复扫描
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final String LOCK_KEY = "lock:scheduler:reminder";
    /** 调度锁 50 秒（小于 60 秒调度间隔，避免死锁；单轮执行超时则由 TTL 自动释放） */
    private static final Duration LOCK_TTL = Duration.ofSeconds(50);

    private final ReminderService reminderService;
    private final RedisService redisService;

    @Scheduled(cron = "0 * * * * ?")
    public void scanReminders() {
        boolean locked = redisService.setIfAbsent(LOCK_KEY, "scheduler", LOCK_TTL);
        if (!locked) {
            log.debug("[提醒调度] 其他实例/上一轮正在执行，本轮跳过");
            return;
        }
        try {
            reminderService.triggerDueReminders();
        } catch (Exception e) {
            log.error("[提醒调度] 扫描异常", e);
        } finally {
            redisService.delete(LOCK_KEY);
        }
    }
}
