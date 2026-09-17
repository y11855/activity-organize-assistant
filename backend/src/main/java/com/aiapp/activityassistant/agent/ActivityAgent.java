package com.aiapp.activityassistant.agent;

import com.aiapp.activityassistant.dto.TaskAssignDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.QuestionnaireService;
import com.aiapp.activityassistant.service.ReminderService;
import com.aiapp.activityassistant.service.TaskService;
import com.aiapp.activityassistant.tool.ToolDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 活动管家 Agent：一句话发起活动，全流程自动跑
 *
 * 编排流程：规划(Plan) → 执行(Execute) → 交付(Deliver)
 *  - 规划：调用大模型生成策划方案、物料清单、问卷、任务分工
 *  - 执行：通过工具调度器调用日历/表单/消息工具，幂等 + 重试
 *  - 交付：创建定时提醒、通知相关人员
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityAgent {

    private final ActivityService activityService;
    private final TaskService taskService;
    private final QuestionnaireService questionnaireService;
    private final ReminderService reminderService;
    private final ToolDispatcher toolDispatcher;
    private final Planner planner;
    private final Deliverer deliverer;

    /**
     * 运行全流程编排
     */
    public void run(Long activityId) {
        log.info("[Agent] 开始编排活动, activityId={}", activityId);
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            log.warn("[Agent] 活动不存在, activityId={}", activityId);
            return;
        }

        // ========== 阶段一：规划 ==========
        log.info("[Agent] 阶段一：规划");
        PlannerResult plan = planner.plan(activity);

        // 保存策划内容
        activity.setPlanContent(plan.getPlanContent());
        activity.setStatus(1);
        activityService.updateById(activity);

        // ========== 阶段二：执行 ==========
        log.info("[Agent] 阶段二：执行");
        // 1. 写日历
        toolDispatcher.dispatch(activityId, "calendar", Map.of(
                "title", activity.getTitle(),
                "startTime", activity.getStartTime(),
                "endTime", activity.getEndTime(),
                "location", activity.getLocation()
        ));

        // 2. 生成问卷
        questionnaireService.generate(activityId);

        // 3. 派发任务
        for (TaskAssignDTO task : plan.getTasks()) {
            task.setActivityId(activityId);
            taskService.assign(task);
        }

        // ========== 阶段三：交付 ==========
        log.info("[Agent] 阶段三：交付");
        deliverer.deliver(activityId, plan);

        activity.setStatus(2);
        activityService.updateById(activity);
        log.info("[Agent] 活动编排完成, activityId={}", activityId);
    }

    /**
     * 生成活动复盘总结
     */
    public String review(Long activityId) {
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return null;
        }
        String review = planner.review(activity);
        activity.setReviewContent(review);
        activity.setStatus(3);
        activityService.updateById(activity);
        return review;
    }
}
