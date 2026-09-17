package com.aiapp.activityassistant.agent;

import com.aiapp.activityassistant.dto.ReminderCreateDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 交付器：创建定时提醒、通知相关人员
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Deliverer {

    private final ActivityService activityService;
    private final ReminderService reminderService;

    @Value("${activity-assistant.reminder.default-advance-minutes:30}")
    private int defaultAdvanceMinutes;

    /**
     * 交付阶段：为活动创建定时提醒
     */
    public void deliver(Long activityId, PlannerResult plan) {
        Activity activity = activityService.getById(activityId);
        if (activity == null || activity.getStartTime() == null) {
            log.warn("[交付器] 活动或开始时间为空，跳过提醒创建");
            return;
        }

        // 创建活动前 N 分钟的提醒
        LocalDateTime triggerTime = activity.getStartTime().minusMinutes(defaultAdvanceMinutes);
        if (triggerTime.isBefore(LocalDateTime.now())) {
            // 若已过提醒时间，立即触发
            triggerTime = LocalDateTime.now().plusMinutes(1);
        }

        ReminderCreateDTO reminder = new ReminderCreateDTO();
        reminder.setActivityId(activityId);
        reminder.setTitle("活动提醒：" + activity.getTitle());
        reminder.setContent("活动「" + activity.getTitle() + "」将在 " + defaultAdvanceMinutes + " 分钟后开始，请做好准备。");
        reminder.setTriggerTime(triggerTime);
        reminderService.create(reminder);

        log.info("[交付器] 已创建活动提醒, activityId={}, triggerTime={}", activityId, triggerTime);
    }
}
