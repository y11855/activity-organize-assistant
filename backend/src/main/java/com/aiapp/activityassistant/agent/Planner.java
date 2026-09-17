package com.aiapp.activityassistant.agent;

import com.aiapp.activityassistant.ai.AiProvider;
import com.aiapp.activityassistant.ai.AiProviderRouter;
import com.aiapp.activityassistant.dto.TaskAssignDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 规划器：调用大模型生成活动策划、物料清单、问卷题目、任务分工
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Planner {

    private final AiProviderRouter aiProviderRouter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 规划活动
     */
    public PlannerResult plan(Activity activity) {
        AiProvider ai = aiProviderRouter.choose();
        String systemPrompt = """
                你是活动策划助手。根据用户的活动描述，输出 JSON，包含：
                - planContent: 策划文案
                - materials: 物料清单数组 [{name, quantity, unit, unitPrice, remark}]
                - questions: 问卷题目数组 [{title, type, required, options[]}]
                - tasks: 任务分工数组 [{title, description, assigneeName, deadlineOffsetHours}]
                只返回 JSON，不要多余文字。
                """;
        String userPrompt = String.format(
                "活动标题：%s\n描述：%s\n开始时间：%s\n地点：%s",
                activity.getTitle(), activity.getDescription(),
                activity.getStartTime(), activity.getLocation()
        );

        String response;
        try {
            response = ai.chat(systemPrompt, userPrompt);
        } catch (Exception e) {
            log.error("[规划器] AI 调用失败，使用兜底方案", e);
            return fallbackPlan(activity);
        }

        PlannerResult result = new PlannerResult();
        try {
            JsonNode root = objectMapper.readTree(extractJson(response));
            result.setPlanContent(root.path("planContent").asText(""));
            result.setMaterials(root.path("materials").toString());
            result.setQuestions(root.path("questions").toString());

            List<TaskAssignDTO> tasks = new ArrayList<>();
            JsonNode tasksNode = root.path("tasks");
            if (tasksNode.isArray()) {
                for (JsonNode t : tasksNode) {
                    TaskAssignDTO task = new TaskAssignDTO();
                    task.setTitle(t.path("title").asText(""));
                    task.setDescription(t.path("description").asText(""));
                    task.setAssigneeName(t.path("assigneeName").asText("待定"));
                    tasks.add(task);
                }
            }
            result.setTasks(tasks);
        } catch (Exception e) {
            log.warn("[规划器] AI 返回解析失败，使用兜底方案", e);
            return fallbackPlan(activity);
        }
        return result;
    }

    /**
     * 复盘总结
     */
    public String review(Activity activity) {
        AiProvider ai = aiProviderRouter.choose();
        String systemPrompt = "你是活动复盘助手。根据活动信息生成简短的复盘总结，包括：亮点、问题、改进建议。";
        String userPrompt = String.format("活动标题：%s\n策划：%s\n请生成复盘总结。",
                activity.getTitle(), activity.getPlanContent());
        try {
            return ai.chat(systemPrompt, userPrompt);
        } catch (Exception e) {
            log.error("[规划器] 复盘 AI 调用失败", e);
            return "（复盘生成失败，请手动填写）";
        }
    }

    private PlannerResult fallbackPlan(Activity activity) {
        PlannerResult result = new PlannerResult();
        result.setPlanContent("【兜底策划】活动《" + activity.getTitle() + "》的基础策划方案。");
        result.setMaterials("[]");
        result.setQuestions("[{\"title\":\"是否参加\",\"type\":\"single\",\"required\":true,\"options\":[\"参加\",\"不参加\"]}]");
        result.setTasks(List.of());
        return result;
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
