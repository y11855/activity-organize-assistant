package com.aiapp.activityassistant.schedule;

import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 定时任务（Spring Scheduling）：任务截止前自动提醒。
 *
 * <p>
 * 每分钟扫描一次：未完成且距离截止不足 1 小时的任务，给负责人发送站内提醒。
 * 同一条任务只提醒一次（由 task_assignment.deadline_reminded 标记去重）。
 * Redis 锁保证多实例部署 / 上一轮未结束时不重复扫描。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskDeadlineScheduler {

    private static final String LOCK_KEY = "lock:scheduler:task-deadline";
    /** 调度锁 50 秒（小于 60 秒调度间隔，避免死锁；单轮执行超时则由 TTL 自动释放） */
    private static final Duration LOCK_TTL = Duration.ofSeconds(50);

    private final TaskService taskService;
    private final RedisService redisService;

    @Scheduled(cron = "0 * * * * ?")
    public void remindUpcomingTasks() {
        boolean locked = redisService.setIfAbsent(LOCK_KEY, "scheduler", LOCK_TTL);
        if (!locked) {
            log.debug("[任务提醒] 其他实例/上一轮正在执行，本轮跳过");
            return;
        }
        try {
            taskService.remindUpcomingDeadlines();
        } catch (Exception e) {
            log.error("[任务提醒] 扫描异常", e);
        } finally {
            redisService.delete(LOCK_KEY);
        }
    }
}
