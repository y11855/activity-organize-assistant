package com.aiapp.activityassistant.agent;

import cn.hutool.json.JSONUtil;
import com.aiapp.activityassistant.ai.AiProvider;
import com.aiapp.activityassistant.ai.AiProviderRouter;
import com.aiapp.activityassistant.dto.ActivityGenerateDTO;
import com.aiapp.activityassistant.dto.QuestionDraftDTO;
import com.aiapp.activityassistant.dto.ReminderDraftDTO;
import com.aiapp.activityassistant.dto.TaskAssignDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.service.UserService;
import com.aiapp.activityassistant.vo.MaterialVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 规划器：调用大模型按发起人勾选的生成项，产出策划、物料、问卷、任务分工与提醒规则
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Planner {

    /** 活动开始前默认提醒提前量（分钟） */
    private static final long REMIND_BEFORE_START_MINUTES = 30;
    /** 活动开始前一天的提醒提前量（小时） */
    private static final long REMIND_BEFORE_START_HOURS = 24;

    private final AiProviderRouter aiProviderRouter;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 按全部生成项规划（兼容旧调用）
     */
    public PlannerResult plan(Activity activity) {
        ActivityGenerateDTO all = new ActivityGenerateDTO();
        all.setTasks(true);
        all.setMaterials(true);
        all.setQuestionnaire(true);
        all.setReminders(true);
        return plan(activity, all);
    }

    /**
     * 按发起人勾选的生成项规划活动
     */
    public PlannerResult plan(Activity activity, ActivityGenerateDTO options) {
        // 解析活动选择的成员，供 AI 在成员间分配任务
        Map<Long, String> memberMap = resolveMembers(activity.getMemberIds());

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("活动标题：").append(activity.getTitle())
                .append("\n描述：").append(activity.getDescription())
                .append("\n开始时间：").append(activity.getStartTime())
                .append("\n结束时间：").append(activity.getEndTime())
                .append("\n地点：").append(activity.getLocation());
        if (activity.getExpectedCount() != null) {
            userPrompt.append("\n预计参与人数：").append(activity.getExpectedCount());
        }
        if (!memberMap.isEmpty()) {
            userPrompt.append("\n成员名单（id:昵称）：");
            memberMap.forEach((id, name) -> userPrompt.append(id).append(":").append(name).append("，"));
            userPrompt.setLength(userPrompt.length() - 1);
        }

        String response;
        try {
            response = aiProviderRouter.choose().chat(buildSystemPrompt(options, memberMap), userPrompt.toString());
        } catch (Exception e) {
            log.error("[规划器] AI 调用失败，使用兜底方案", e);
            return fallbackPlan(activity, options);
        }

        PlannerResult result = new PlannerResult();
        try {
            JsonNode root = objectMapper.readTree(extractJson(response));
            result.setPlanContent(root.path("planContent").asText(""));
            if (options.wantMaterials()) {
                result.setMaterials(parseMaterials(root.path("materials")));
            }
            if (options.wantQuestionnaire()) {
                result.setQuestions(parseQuestions(root.path("questions")));
            }
            if (options.wantTasks()) {
                result.setTasks(parseTasks(root.path("tasks"), memberMap));
            }
            if (options.wantReminders()) {
                result.setReminders(buildReminders(activity, options));
            }
        } catch (Exception e) {
            log.warn("[规划器] AI 返回解析失败，使用兜底方案", e);
            return fallbackPlan(activity, options);
        }
        return result;
    }

    /**
     * 按勾选项动态拼装 system prompt，要求 AI 只输出需要的内容
     */
    private String buildSystemPrompt(ActivityGenerateDTO options, Map<Long, String> memberMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是活动策划助手。根据用户的活动描述，输出 JSON，包含：\n")
                .append("- planContent: 策划文案（包含活动流程与任务完成计划）\n");
        if (options.wantTasks()) {
            sb.append("- tasks: 任务分工数组 [{title, description, assigneeId, assigneeName, deadlineOffsetHours}]\n");
        }
        if (options.wantMaterials()) {
            sb.append("- materials: 物料清单数组 [{name, quantity, unit, unitPrice, remark}]\n");
        }
        if (options.wantQuestionnaire()) {
            sb.append("- questions: 问卷题目数组 [{title, type, required, options[]}]\n");
        }
        sb.append("要求：\n");
        if (options.wantTasks()) {
            if (!memberMap.isEmpty()) {
                sb.append("- tasks 必须且只能在成员名单间分配，assigneeId 与 assigneeName 必须来自名单，尽量均衡分摊\n");
            }
            sb.append("- deadlineOffsetHours 为任务截止距当前的小时数（整数，24~168 之间）\n");
        }
        if (options.wantQuestionnaire()) {
            sb.append("- questions 的 type 只能是 single（单选）、multiple（多选）、text（填空）\n");
        }
        sb.append("- 若提供了预计人数，物料数量与任务粒度按人数合理规划\n");
        sb.append("只返回 JSON，不要多余文字。");
        return sb.toString();
    }

    /**
     * 解析物料清单
     */
    private List<MaterialVO> parseMaterials(JsonNode node) {
        List<MaterialVO> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode m : node) {
                MaterialVO vo = new MaterialVO();
                vo.setName(m.path("name").asText(""));
                vo.setQuantity(m.path("quantity").asInt(1));
                vo.setUnit(m.path("unit").asText(""));
                String price = m.path("unitPrice").asText("");
                if (!price.isBlank() && !"null".equals(price)) {
                    try {
                        vo.setUnitPrice(new java.math.BigDecimal(price));
                    } catch (NumberFormatException ignored) {
                        // 单价格式非法则忽略，保留为空
                    }
                }
                vo.setRemark(m.path("remark").asText(""));
                list.add(vo);
            }
        }
        return list;
    }

    /**
     * 解析问卷题目
     */
    private List<QuestionDraftDTO> parseQuestions(JsonNode node) {
        List<QuestionDraftDTO> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode q : node) {
                QuestionDraftDTO dto = new QuestionDraftDTO();
                dto.setTitle(q.path("title").asText(""));
                dto.setType(q.path("type").asText("text"));
                dto.setRequired(q.path("required").asBoolean(false));
                List<String> opts = new ArrayList<>();
                JsonNode optionNode = q.path("options");
                if (optionNode.isArray()) {
                    for (JsonNode o : optionNode) {
                        opts.add(o.asText(""));
                    }
                }
                dto.setOptions(opts);
                list.add(dto);
            }
        }
        return list;
    }

    /**
     * 解析任务数组：映射负责人 ID，并把 deadlineOffsetHours 换算为绝对截止时间
     */
    private List<TaskAssignDTO> parseTasks(JsonNode tasksNode, Map<Long, String> memberMap) {
        List<TaskAssignDTO> tasks = new ArrayList<>();
        // 昵称 → ID 反查表，兜底 AI 只返回名字的情况
        Map<String, Long> nameToId = new HashMap<>();
        memberMap.forEach((id, name) -> nameToId.put(name, id));

        if (tasksNode.isArray()) {
            for (JsonNode t : tasksNode) {
                TaskAssignDTO task = new TaskAssignDTO();
                task.setTitle(t.path("title").asText(""));
                task.setDescription(t.path("description").asText(""));

                long assigneeId = t.path("assigneeId").asLong(0);
                String assigneeName = t.path("assigneeName").asText("");
                if (assigneeId > 0) {
                    task.setAssigneeId(assigneeId);
                    // 以名单中的昵称为准
                    task.setAssigneeName(memberMap.getOrDefault(assigneeId,
                            assigneeName.isEmpty() ? "待定" : assigneeName));
                } else if (!assigneeName.isEmpty() && nameToId.containsKey(assigneeName)) {
                    task.setAssigneeId(nameToId.get(assigneeName));
                    task.setAssigneeName(assigneeName);
                } else {
                    task.setAssigneeName(assigneeName.isEmpty() ? "待定" : assigneeName);
                }

                int offsetHours = t.path("deadlineOffsetHours").asInt(0);
                if (offsetHours > 0) {
                    task.setDeadline(LocalDateTime.now().plusHours(offsetHours));
                }
                tasks.add(task);
            }
        }
        return tasks;
    }

    /**
     * 依据活动时间生成提醒规则草稿（发起人可在确认前调整触发时间）
     */
    private List<ReminderDraftDTO> buildReminders(Activity activity, ActivityGenerateDTO options) {
        List<ReminderDraftDTO> list = new ArrayList<>();
        LocalDateTime start = activity.getStartTime();
        if (start == null) {
            log.info("[规划器] 活动未设置开始时间，跳过提醒规则生成, activityId={}", activity.getId());
            return list;
        }
        User creator = activity.getCreatorId() == null ? null : userService.getById(activity.getCreatorId());
        String creatorName = creator == null ? null
                : (creator.getNickname() == null ? creator.getUsername() : creator.getNickname());
        String location = activity.getLocation() == null ? "" : "，地点：" + activity.getLocation();

        LocalDateTime dayBefore = start.minusHours(REMIND_BEFORE_START_HOURS);
        if (dayBefore.isAfter(LocalDateTime.now())) {
            ReminderDraftDTO r = new ReminderDraftDTO();
            r.setTitle("活动预告：" + activity.getTitle());
            r.setContent("活动「" + activity.getTitle() + "」将于明天 " + start + " 开始" + location + "，请提前做好准备。");
            r.setTargetUserId(activity.getCreatorId());
            r.setTargetUserName(creatorName);
            r.setTriggerTime(dayBefore);
            list.add(r);
        }

        LocalDateTime beforeStart = start.minusMinutes(REMIND_BEFORE_START_MINUTES);
        ReminderDraftDTO soon = new ReminderDraftDTO();
        soon.setTitle("活动提醒：" + activity.getTitle());
        soon.setContent("活动「" + activity.getTitle() + "」将在 " + REMIND_BEFORE_START_MINUTES + " 分钟后开始"
                + location + "，请做好准备。");
        soon.setTargetUserId(activity.getCreatorId());
        soon.setTargetUserName(creatorName);
        soon.setTriggerTime(beforeStart.isAfter(LocalDateTime.now()) ? beforeStart
                : LocalDateTime.now().plusMinutes(1));
        list.add(soon);
        return list;
    }

    /**
     * 解析活动成员ID JSON → {id: 昵称}
     */
    private Map<Long, String> resolveMembers(String memberIdsJson) {
        Map<Long, String> map = new HashMap<>();
        if (memberIdsJson == null || memberIdsJson.isBlank()) {
            return map;
        }
        try {
            List<Long> ids = JSONUtil.toList(memberIdsJson, Long.class);
            if (ids.isEmpty()) {
                return map;
            }
            for (User u : userService.listByIds(ids)) {
                map.put(u.getId(), u.getNickname() == null ? u.getUsername() : u.getNickname());
            }
        } catch (Exception e) {
            log.warn("[规划器] 解析成员名单失败: {}", memberIdsJson);
        }
        return map;
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

    private PlannerResult fallbackPlan(Activity activity, ActivityGenerateDTO options) {
        PlannerResult result = new PlannerResult();
        result.setPlanContent("【兜底策划】活动《" + activity.getTitle() + "》的基础策划方案。");
        if (options.wantQuestionnaire()) {
            QuestionDraftDTO q = new QuestionDraftDTO();
            q.setTitle("是否参加");
            q.setType("single");
            q.setRequired(true);
            q.setOptions(List.of("参加", "不参加"));
            result.setQuestions(new ArrayList<>(List.of(q)));
        }
        if (options.wantReminders()) {
            result.setReminders(buildReminders(activity, options));
        }
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
