package com.aiapp.activityassistant.schedule;

import com.aiapp.activityassistant.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 提醒任务调度器
 * 每分钟扫描一次到期提醒并触发
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;

    @Scheduled(cron = "0 * * * * ?")
    public void scanReminders() {
        try {
            reminderService.triggerDueReminders();
        } catch (Exception e) {
            log.error("[提醒调度] 扫描异常", e);
        }
    }
}
