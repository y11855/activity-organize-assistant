package com.aiapp.activityassistant.schedule;

import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.entity.Questionnaire;
import com.aiapp.activityassistant.entity.QuestionnaireResponse;
import com.aiapp.activityassistant.mapper.QuestionnaireMapper;
import com.aiapp.activityassistant.mapper.QuestionnaireResponseMapper;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.MessageService;
import com.aiapp.activityassistant.service.QuestionnaireService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动结束反馈通知调度器
 * 每分钟扫描已到结束时间且未通知的「进行中」活动：
 * - 确保反馈问卷存在
 * - 给所有报名参与者发站内消息引导填写反馈
 * - 标记已通知并把活动置为「已完成」
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackNotifyScheduler {

    private static final String LOCK_KEY = "lock:scheduler:feedback-notify";
    private static final Duration LOCK_TTL = Duration.ofSeconds(50);

    private final ActivityService activityService;
    private final QuestionnaireService questionnaireService;
    private final QuestionnaireMapper questionnaireMapper;
    private final QuestionnaireResponseMapper responseMapper;
    private final MessageService messageService;
    private final RedisService redisService;

    @Scheduled(cron = "0 * * * * ?")
    public void scanAndNotify() {
        boolean locked = redisService.setIfAbsent(LOCK_KEY, "scheduler", LOCK_TTL);
        if (!locked) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Activity> dueActivities = activityService.list(
                    new LambdaQueryWrapper<Activity>()
                            .eq(Activity::getStatus, 2)
                            .lt(Activity::getEndTime, now)
                            .and(w -> w.isNull(Activity::getFeedbackNotified)
                                    .or().eq(Activity::getFeedbackNotified, 0)));
            for (Activity activity : dueActivities) {
                try {
                    notifyFeedback(activity);
                } catch (Exception e) {
                    log.error("[结束通知] 活动 {} 处理失败", activity.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("[结束通知] 扫描异常", e);
        } finally {
            redisService.delete(LOCK_KEY);
        }
    }

    private void notifyFeedback(Activity activity) {
        Long activityId = activity.getId();
        // 1. 报名问卷（type=1）
        Questionnaire regQ = questionnaireMapper.selectOne(new LambdaQueryWrapper<Questionnaire>()
                .eq(Questionnaire::getActivityId, activityId)
                .and(w -> w.isNull(Questionnaire::getType).or().eq(Questionnaire::getType, 1))
                .last("LIMIT 1"));
        if (regQ == null) {
            log.info("[结束通知] 活动 {} 无报名问卷，跳过", activityId);
            markNotified(activity);
            return;
        }
        // 2. 报名参与者（排除已取消）
        List<QuestionnaireResponse> responses = responseMapper.selectList(
                new LambdaQueryWrapper<QuestionnaireResponse>()
                        .eq(QuestionnaireResponse::getQuestionnaireId, regQ.getId())
                        .ne(QuestionnaireResponse::getStatus, 2));
        if (responses.isEmpty()) {
            log.info("[结束通知] 活动 {} 无有效报名者，跳过", activityId);
            markNotified(activity);
            return;
        }
        // 3. 确保反馈问卷
        Long feedbackQId = questionnaireService.ensureFeedbackQuestionnaire(activityId);

        // 4. 通知内容（支持自定义模板，占位符 {activityName} {feedbackUrl}）
        String template = activity.getFeedbackTemplateContent();
        if (template == null || template.isBlank()) {
            template = "您参与的活动【{activityName}】已结束，诚邀您花 1 分钟填写反馈问卷，帮助我们持续改进：{feedbackUrl}";
        }
        String content = template
                .replace("{activityName}", activity.getTitle() == null ? "活动" : activity.getTitle())
                .replace("{feedbackUrl}", "/questionnaires/" + feedbackQId);
        String title = "【活动反馈】" + (activity.getTitle() == null ? "活动已结束" : activity.getTitle());

        // 5. 逐个发消息（去重 respondentId）
        List<Long> respondentIds = responses.stream()
                .map(QuestionnaireResponse::getRespondentId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        for (Long userId : respondentIds) {
            try {
                messageService.send(userId, title, content, activityId);
            } catch (Exception e) {
                log.error("[结束通知] 发送给用户 {} 失败", userId, e);
            }
        }
        log.info("[结束通知] 活动 {} 已通知 {} 名参与者", activityId, respondentIds.size());

        markNotified(activity);
    }

    private void markNotified(Activity activity) {
        activity.setFeedbackNotified(1);
        activity.setStatus(3); // 已完成
        activityService.updateById(activity);
    }
}
