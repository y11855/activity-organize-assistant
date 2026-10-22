package com.aiapp.activityassistant.agent;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.dto.ActivityGenerateDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.entity.ActivityEventLog;
import com.aiapp.activityassistant.entity.Questionnaire;
import com.aiapp.activityassistant.entity.QuestionnaireResponse;
import com.aiapp.activityassistant.mapper.ActivityEventLogMapper;
import com.aiapp.activityassistant.mapper.QuestionnaireMapper;
import com.aiapp.activityassistant.mapper.QuestionnaireResponseMapper;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.vo.ActivityDraftVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 活动管家 Agent：AI 生成活动内容草稿
 *
 * 新流程：发起人勾选生成项 → AI 生成草稿（本类，异步）→ 发起人预览/编辑 → 手动确认发布。
 * AI 只产出草稿，不直接写入任务/物料/问卷/提醒等业务表，避免未经确认就发布内容。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityAgent {

    private final ActivityService activityService;
    private final Planner planner;
    private final QuestionnaireMapper questionnaireMapper;
    private final QuestionnaireResponseMapper responseMapper;
    private final ActivityEventLogMapper eventLogMapper;

    /** 草稿序列化/反序列化共用 JacksonConfig 中配置的 draftObjectMapper */
    @Qualifier("draftObjectMapper")
    private final ObjectMapper draftMapper;

    /**
     * 异步生成活动内容草稿。
     * 通过独立 Bean 的代理调用，{@code @Async} 生效：接口立即返回，生成在后台执行。
     */
    @Async
    public void generateDraft(Long activityId, ActivityGenerateDTO options) {
        log.info("[Agent] 开始生成活动内容草稿, activityId={}, options={}", activityId, options);
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            log.warn("[Agent] 活动不存在, activityId={}", activityId);
            return;
        }
        // 异步线程无请求上下文，显式以活动创建人身份运行（内部权限校验依赖 UserContext）
        UserContext.setCurrentUserId(activity.getCreatorId());
        try {
            // 取消/已发布等状态变更后不再写回草稿，避免状态互斥被破坏
            if (!Objects.equals(activity.getStatus(), 1)) {
                log.warn("[Agent] 活动状态已变更，跳过草稿生成, activityId={}, status={}",
                        activityId, activity.getStatus());
                return;
            }
            PlannerResult plan = planner.plan(activity, options);

            ActivityDraftVO draft = new ActivityDraftVO();
            draft.setOptions(options);
            draft.setPlanContent(plan.getPlanContent());
            draft.setMaterials(plan.getMaterials());
            draft.setQuestions(plan.getQuestions());
            draft.setTasks(plan.getTasks());
            draft.setReminders(plan.getReminders());

            // 再次确认状态未被并发变更（如生成期间被取消）
            Activity latest = activityService.getById(activityId);
            if (latest == null || !Objects.equals(latest.getStatus(), 1)) {
                log.warn("[Agent] 生成完成时活动状态已变更，丢弃草稿, activityId={}", activityId);
                return;
            }
            latest.setPlanContent(plan.getPlanContent());
            latest.setDraftContent(draftMapper.writeValueAsString(draft));
            latest.setDraftStatus(1);
            latest.setStatus(5);
            activityService.updateById(latest);
            activityService.evictDetailCache(activityId);
            log.info("[Agent] 草稿生成完成, activityId={}，等待发起人确认", activityId);
        } catch (Exception e) {
            log.error("[Agent] 草稿生成失败, activityId={}", activityId, e);
            rollbackToDraft(activityId);
        } finally {
            UserContext.clear();
        }
    }

    /** 生成失败时回退为「待策划」，允许发起人重新生成 */
    private void rollbackToDraft(Long activityId) {
        Activity activity = activityService.getById(activityId);
        if (activity != null && activity.getStatus() != null && activity.getStatus() == 1) {
            activity.setStatus(0);
            activity.setDraftStatus(0);
            activityService.updateById(activity);
            activityService.evictDetailCache(activityId);
        }
    }

    /**
     * 生成活动复盘总结：综合策划方案 + 参与者反馈问卷 + 发起人事件记录
     */
    public String review(Long activityId) {
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return null;
        }
        // 反馈问卷（type=2）的答卷数据
        Questionnaire feedbackQ = questionnaireMapper.selectOne(new LambdaQueryWrapper<Questionnaire>()
                .eq(Questionnaire::getActivityId, activityId)
                .eq(Questionnaire::getType, 2)
                .last("LIMIT 1"));
        List<QuestionnaireResponse> feedbackResponses = (feedbackQ == null) ? Collections.emptyList()
                : responseMapper.selectList(new LambdaQueryWrapper<QuestionnaireResponse>()
                        .eq(QuestionnaireResponse::getQuestionnaireId, feedbackQ.getId()));
        // 活动事件记录
        List<ActivityEventLog> eventLogs = eventLogMapper.selectList(new LambdaQueryWrapper<ActivityEventLog>()
                .eq(ActivityEventLog::getActivityId, activityId)
                .orderByAsc(ActivityEventLog::getOccurredAt));

        String review = planner.review(activity, feedbackResponses, eventLogs);
        activity.setReviewContent(review);
        activity.setStatus(3);
        activityService.updateById(activity);
        activityService.evictDetailCache(activityId);
        return review;
    }
}
