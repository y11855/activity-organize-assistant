package com.aiapp.activityassistant.schedule;

import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.service.QuestionnaireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 定时任务（Spring Scheduling）：活动截止后自动关闭报名问卷。
 *
 * <p>
 * 每分钟扫描一次：活动结束时间已到的报名问卷（type=1）自动置为已截止，
 * 禁止参与者继续报名。与提交接口的活动截止时间校验形成双保险。
 * Redis 锁保证多实例部署 / 上一轮未结束时不重复扫描。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityDeadlineScheduler {

    private static final String LOCK_KEY = "lock:scheduler:activity-deadline";
    /** 调度锁 50 秒（小于 60 秒调度间隔，避免死锁；单轮执行超时则由 TTL 自动释放） */
    private static final Duration LOCK_TTL = Duration.ofSeconds(50);

    private final QuestionnaireService questionnaireService;
    private final RedisService redisService;

    @Scheduled(cron = "0 * * * * ?")
    public void closeExpiredRegistrations() {
        boolean locked = redisService.setIfAbsent(LOCK_KEY, "scheduler", LOCK_TTL);
        if (!locked) {
            log.debug("[截止调度] 其他实例/上一轮正在执行，本轮跳过");
            return;
        }
        try {
            int closed = questionnaireService.closeExpiredQuestionnaires();
            if (closed > 0) {
                log.info("[截止调度] 自动关闭已截止报名问卷 {} 个", closed);
            }
        } catch (Exception e) {
            log.error("[截止调度] 扫描异常", e);
        } finally {
            redisService.delete(LOCK_KEY);
        }
    }
}
