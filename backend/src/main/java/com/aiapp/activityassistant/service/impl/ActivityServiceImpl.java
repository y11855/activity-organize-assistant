package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.agent.ActivityAgent;
import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.common.utils.IdempotentKeyGenerator;
import com.aiapp.activityassistant.dto.ActivityGenerateDTO;
import com.aiapp.activityassistant.dto.ActivitySaveDTO;
import com.aiapp.activityassistant.dto.ReminderDraftDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.entity.Material;
import com.aiapp.activityassistant.entity.Message;
import com.aiapp.activityassistant.entity.Questionnaire;
import com.aiapp.activityassistant.entity.QuestionnaireResponse;
import com.aiapp.activityassistant.entity.Reminder;
import com.aiapp.activityassistant.entity.TaskAssignment;
import com.aiapp.activityassistant.mapper.ActivityMapper;
import com.aiapp.activityassistant.mapper.MaterialMapper;
import com.aiapp.activityassistant.mapper.QuestionnaireMapper;
import com.aiapp.activityassistant.mapper.QuestionnaireResponseMapper;
import com.aiapp.activityassistant.mapper.ReminderMapper;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.MessageService;
import com.aiapp.activityassistant.service.TaskService;
import com.aiapp.activityassistant.service.UserService;
import com.aiapp.activityassistant.vo.ActivityBreakdownNodeVO;
import com.aiapp.activityassistant.vo.ActivityDraftVO;
import com.aiapp.activityassistant.vo.ActivityVO;
import com.aiapp.activityassistant.vo.MaterialVO;
import com.aiapp.activityassistant.vo.TaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 活动业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity>
        implements ActivityService {

    /** 活动详情缓存 key 前缀（RedisService 会再补全局前缀 activity:） */
    private static final String DETAIL_CACHE_KEY = "cache:activity:detail:";
    /** 详情缓存有效期 2 分钟，兜底 Agent 异步编排期间的短暂不一致 */
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(2);

    private final MaterialMapper materialMapper;
    private final TaskService taskService;
    private final RedisService redisService;
    private final QuestionnaireMapper questionnaireMapper;
    private final QuestionnaireResponseMapper questionnaireResponseMapper;
    private final ReminderMapper reminderMapper;
    private final MessageService messageService;
    private final UserService userService;
    private final IdempotentKeyGenerator idempotentKeyGenerator;

    /** 草稿 JSON 序列化/反序列化共用 JacksonConfig 中配置的 draftObjectMapper（支持 LocalDateTime） */
    @Qualifier("draftObjectMapper")
    private final ObjectMapper draftMapper;

    /**
     * Agent 又依赖 ActivityService，构成构造器循环，用字段注入 + @Lazy 打破
     */
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private ActivityAgent activityAgent;

    @Override
    public Long create(ActivitySaveDTO dto) {
        Long currentUserId = UserContext.getCurrentUserId();
        Activity activity = new Activity();
        BeanUtils.copyProperties(dto, activity, "memberIds");
        // memberIds 是 List<Long>，落库为 JSON 字符串
        if (dto.getMemberIds() != null && !dto.getMemberIds().isEmpty()) {
            activity.setMemberIds(cn.hutool.json.JSONUtil.toJsonStr(dto.getMemberIds()));
        }
        activity.setCreatorId(currentUserId);
        activity.setStatus(0);
        activity.setDraftStatus(0);
        save(activity);
        // AI 生成需发起人勾选生成项后另行触发（POST /activities/{id}/generate）
        return activity.getId();
    }

    @Override
    public void generate(Long id, ActivityGenerateDTO options) {
        checkOwnership(id);
        Activity activity = getById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Integer status = activity.getStatus();
        // 状态互斥：仅「草稿」或「待确认」允许触发 AI 生成
        if (status == null || (status != 0 && status != 5)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "当前状态（" + activityStatusLabel(status) + "）不可生成内容");
        }
        if (options == null || !options.anySelected()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请至少勾选一项需要 AI 生成的内容");
        }
        activity.setGenerateOptions(cn.hutool.json.JSONUtil.toJsonStr(options));
        activity.setStatus(1);
        activity.setDraftStatus(0);
        activity.setDraftContent(null);
        updateById(activity);
        evictDetailCache(id);
        // 异步生成草稿，前端轮询详情状态
        activityAgent.generateDraft(id, options);
    }

    @Override
    public ActivityDraftVO draft(Long id) {
        checkOwnership(id);
        Activity activity = getById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (activity.getDraftContent() == null || activity.getDraftContent().isBlank()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "暂无待确认的 AI 生成内容");
        }
        return readDraft(activity.getDraftContent());
    }

    @Override
    public void updateDraft(Long id, ActivityDraftVO draft) {
        checkOwnership(id);
        Activity activity = getById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 状态互斥：仅「待确认」状态的内容可编辑
        if (activity.getStatus() == null || activity.getStatus() != 5
                || !Integer.valueOf(1).equals(activity.getDraftStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "仅待确认状态的内容可以编辑");
        }
        activity.setDraftContent(writeDraft(draft));
        activity.setPlanContent(draft.getPlanContent());
        updateById(activity);
        evictDetailCache(id);
        log.info("[活动] 草稿已更新, activityId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        checkOwnership(id);
        Activity activity = getById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 状态互斥：必须处于「待确认」且草稿就绪，防止未经确认直接发布
        if (activity.getStatus() == null || activity.getStatus() != 5
                || !Integer.valueOf(1).equals(activity.getDraftStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "当前状态不可确认发布，请先完成 AI 生成");
        }
        ActivityDraftVO draft = readDraft(activity.getDraftContent());
        ActivityGenerateDTO options = draft.getOptions() == null ? new ActivityGenerateDTO() : draft.getOptions();

        // 1. 物料清单：先清空旧的，再按草稿落库
        materialMapper.delete(new LambdaQueryWrapper<Material>().eq(Material::getActivityId, id));
        if (draft.getMaterials() != null) {
            for (MaterialVO m : draft.getMaterials()) {
                Material entity = new Material();
                BeanUtils.copyProperties(m, entity, "id");
                entity.setActivityId(id);
                materialMapper.insert(entity);
            }
        }

        // 2. 任务分工：先清空旧的，再按草稿落库（此时才正式发布任务）
        taskService.remove(new LambdaQueryWrapper<TaskAssignment>().eq(TaskAssignment::getActivityId, id));
        if (draft.getTasks() != null) {
            for (com.aiapp.activityassistant.dto.TaskAssignDTO task : draft.getTasks()) {
                task.setActivityId(id);
                taskService.assign(task);
            }
        }

        // 3. 报名问卷：按草稿题目发布
        if (options.wantQuestionnaire() && draft.getQuestions() != null && !draft.getQuestions().isEmpty()) {
            String questionsJson = writeJson(draft.getQuestions());
            Questionnaire existing = questionnaireMapper.selectOne(new LambdaQueryWrapper<Questionnaire>()
                    .eq(Questionnaire::getActivityId, id));
            if (existing == null) {
                Questionnaire q = new Questionnaire();
                q.setActivityId(id);
                q.setTitle("活动报名问卷");
                q.setDescription("请填写报名信息");
                q.setQuestions(questionsJson);
                q.setStatus(1);
                questionnaireMapper.insert(q);
            } else {
                existing.setQuestions(questionsJson);
                existing.setStatus(1);
                questionnaireMapper.updateById(existing);
            }
        }

        // 4. 定时提醒
        createReminders(id, draft.getReminders());

        // 5. 状态流转：待确认 → 执行中，草稿标记已确认
        activity.setStatus(2);
        activity.setDraftStatus(2);
        activity.setDraftContent(null);
        updateById(activity);
        evictDetailCache(id);
        log.info("[活动] 内容已确认发布, activityId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, boolean deleteRecord) {
        checkOwnership(id);
        Activity activity = getById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Integer status = activity.getStatus();
        // 状态互斥：AI 生成中不允许取消，避免生成回写与取消状态冲突
        if (Integer.valueOf(1).equals(status)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 正在生成内容，请等待生成完成后再取消活动");
        }
        if (Integer.valueOf(4).equals(status) && !deleteRecord) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "活动已取消");
        }

        // 1. 所有关联报名问卷自动截止，禁止新用户继续提交报名
        List<Questionnaire> questionnaires = questionnaireMapper.selectList(
                new LambdaQueryWrapper<Questionnaire>().eq(Questionnaire::getActivityId, id));
        for (Questionnaire q : questionnaires) {
            if (!Integer.valueOf(2).equals(q.getStatus())) {
                q.setStatus(2);
                questionnaireMapper.updateById(q);
            }
        }
        // 2. 待触发的提醒一并取消，避免活动取消后仍发送提醒
        List<Reminder> reminders = reminderMapper.selectList(new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getActivityId, id)
                .eq(Reminder::getStatus, 0));
        for (Reminder r : reminders) {
            r.setStatus(2);
            reminderMapper.updateById(r);
        }

        if (deleteRecord) {
            // 级联删除活动及其全部关联数据，避免残留脏数据
            questionnaireResponseMapper.delete(new LambdaQueryWrapper<QuestionnaireResponse>()
                    .eq(QuestionnaireResponse::getActivityId, id));
            questionnaireMapper.delete(new LambdaQueryWrapper<Questionnaire>()
                    .eq(Questionnaire::getActivityId, id));
            taskService.remove(new LambdaQueryWrapper<TaskAssignment>()
                    .eq(TaskAssignment::getActivityId, id));
            materialMapper.delete(new LambdaQueryWrapper<Material>().eq(Material::getActivityId, id));
            reminderMapper.delete(new LambdaQueryWrapper<Reminder>().eq(Reminder::getActivityId, id));
            messageService.remove(new LambdaQueryWrapper<Message>().eq(Message::getActivityId, id));
            removeById(id);
            log.info("[活动] 已取消并删除活动记录, activityId={}, 级联截止问卷={}", id, questionnaires.size());
        } else {
            activity.setStatus(4);
            activity.setDraftStatus(0);
            activity.setDraftContent(null);
            updateById(activity);
            log.info("[活动] 已取消（保留记录）, activityId={}, 级联截止问卷={}", id, questionnaires.size());
        }
        evictDetailCache(id);
    }

    @Override
    public void createReminders(Long activityId, List<ReminderDraftDTO> reminders) {
        if (reminders == null || reminders.isEmpty()) {
            return;
        }
        for (ReminderDraftDTO dto : reminders) {
            Reminder reminder = new Reminder();
            BeanUtils.copyProperties(dto, reminder);
            reminder.setActivityId(activityId);
            reminder.setStatus(0);
            reminder.setRetryCount(0);
            reminder.setIdempotentKey(idempotentKeyGenerator.generate(
                    "reminder", String.valueOf(activityId),
                    String.valueOf(dto.getTargetUserId()),
                    String.valueOf(dto.getTriggerTime()),
                    dto.getTitle()));
            reminderMapper.insert(reminder);
        }
    }

    /** 读取草稿 JSON */
    private ActivityDraftVO readDraft(String json) {
        try {
            return draftMapper.readValue(json, ActivityDraftVO.class);
        } catch (Exception e) {
            log.error("[活动] 草稿解析失败", e);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "草稿数据解析失败，请重新生成");
        }
    }

    /** 写入草稿 JSON */
    private String writeDraft(ActivityDraftVO draft) {
        return writeJson(draft);
    }

    private String writeJson(Object value) {
        try {
            return draftMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.error("[活动] 草稿序列化失败", e);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "草稿数据保存失败");
        }
    }

    @Override
    public ActivityVO detail(Long id) {
        // 1. 先查 Redis 缓存
        Object cached = redisService.get(DETAIL_CACHE_KEY + id);
        if (cached instanceof ActivityVO vo) {
            return vo;
        }

        // 2. 缓存未命中，查库
        checkOwnership(id);
        Activity activity = getById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ActivityVO vo = new ActivityVO();
        BeanUtils.copyProperties(activity, vo);
        vo.setMemberNames(resolveMemberNames(activity.getMemberIds()));

        // 物料清单
        List<Material> materials = materialMapper.selectList(
                new LambdaQueryWrapper<Material>().eq(Material::getActivityId, id));
        vo.setMaterials(materials.stream().map(m -> {
            MaterialVO mvo = new MaterialVO();
            BeanUtils.copyProperties(m, mvo);
            return mvo;
        }).collect(Collectors.toList()));

        // 任务列表
        List<TaskAssignment> tasks = taskService.listByActivity(id);
        vo.setTasks(tasks.stream().map(t -> {
            TaskVO tvo = new TaskVO();
            BeanUtils.copyProperties(t, tvo);
            return tvo;
        }).collect(Collectors.toList()));

        // 3. 回填缓存
        redisService.set(DETAIL_CACHE_KEY + id, vo, DETAIL_CACHE_TTL);
        return vo;
    }

    @Override
    public List<ActivityVO> listMine() {
        Long currentUserId = UserContext.getCurrentUserId();
        List<Activity> list = list(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCreatorId, currentUserId)
                .orderByDesc(Activity::getCreateTime));
        return list.stream().map(a -> {
            ActivityVO vo = new ActivityVO();
            BeanUtils.copyProperties(a, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public String generateReview(Long id) {
        // 同步执行：控制器需直接返回复盘内容，且权限校验依赖请求线程的 UserContext（异步线程无法继承 ThreadLocal）
        checkOwnership(id);
        String review = activityAgent.review(id);
        evictDetailCache(id);
        return review;
    }

    @Override
    public void checkOwnership(Long activityId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (!currentUserId.equals(activity.getCreatorId())) {
            throw new BusinessException(ResultCode.ACTIVITY_NOT_OWNED);
        }
    }

    @Override
    public void evictDetailCache(Long activityId) {
        redisService.delete(DETAIL_CACHE_KEY + activityId);
    }

    @Override
    public List<ActivityBreakdownNodeVO> breakdownTree() {
        Long currentUserId = UserContext.getCurrentUserId();
        // 1. 查出当前用户的所有活动
        List<Activity> activities = list(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCreatorId, currentUserId)
                .orderByDesc(Activity::getCreateTime));
        if (activities.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> activityIds = activities.stream().map(Activity::getId).toList();

        // 2. 一次性查出所有活动的子资源，按 activityId 分组
        Map<Long, List<TaskAssignment>> taskMap = groupByActivity(
                taskService.list(new LambdaQueryWrapper<TaskAssignment>()
                        .in(TaskAssignment::getActivityId, activityIds)));
        Map<Long, List<Material>> materialMap = groupByActivity(
                materialMapper.selectList(new LambdaQueryWrapper<Material>()
                        .in(Material::getActivityId, activityIds)));
        Map<Long, List<Questionnaire>> qnMap = groupByActivity(
                questionnaireMapper.selectList(new LambdaQueryWrapper<Questionnaire>()
                        .in(Questionnaire::getActivityId, activityIds)));
        Map<Long, List<Reminder>> reminderMap = groupByActivity(
                reminderMapper.selectList(new LambdaQueryWrapper<Reminder>()
                        .in(Reminder::getActivityId, activityIds)));

        // 3. 组装树
        List<ActivityBreakdownNodeVO> roots = new ArrayList<>();
        for (Activity a : activities) {
            ActivityBreakdownNodeVO actNode = new ActivityBreakdownNodeVO();
            actNode.setId("act-" + a.getId());
            actNode.setName(a.getTitle());
            actNode.setType("activity");
            actNode.setStatus(a.getStatus());
            Map<String, Object> actMeta = new LinkedHashMap<>();
            actMeta.put("描述", a.getDescription() == null ? "—" : a.getDescription());
            actMeta.put("状态", activityStatusLabel(a.getStatus()));
            actMeta.put("开始时间", a.getStartTime() == null ? "—" : a.getStartTime().toString());
            actMeta.put("结束时间", a.getEndTime() == null ? "—" : a.getEndTime().toString());
            actNode.setMeta(actMeta);

            List<ActivityBreakdownNodeVO> children = new ArrayList<>();
            children.add(buildCategory("任务分工", "task", taskMap.get(a.getId()),
                    t -> t.getTitle() + (t.getAssigneeName() == null ? "" : " · " + t.getAssigneeName()),
                    t -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("标题", t.getTitle());
                        m.put("负责人", t.getAssigneeName() == null ? "—" : t.getAssigneeName());
                        m.put("截止时间", t.getDeadline() == null ? "—" : t.getDeadline().toString());
                        m.put("状态", taskStatusLabel(t.getStatus()));
                        m.put("描述", t.getDescription() == null ? "—" : t.getDescription());
                        return m;
                    },
                    TaskAssignment::getStatus));
            children.add(buildCategory("物料清单", "material", materialMap.get(a.getId()),
                    m -> m.getName() + " · " + m.getQuantity() + (m.getUnit() == null ? "" : m.getUnit()),
                    m -> {
                        Map<String, Object> meta = new LinkedHashMap<>();
                        meta.put("名称", m.getName());
                        meta.put("数量", m.getQuantity());
                        meta.put("单位", m.getUnit() == null ? "—" : m.getUnit());
                        meta.put("单价", m.getUnitPrice() == null ? "—" : m.getUnitPrice().toPlainString());
                        meta.put("备注", m.getRemark() == null ? "—" : m.getRemark());
                        return meta;
                    },
                    m -> null));
            children.add(buildCategory("报名问卷", "questionnaire", qnMap.get(a.getId()),
                    q -> q.getTitle(),
                    q -> {
                        Map<String, Object> meta = new LinkedHashMap<>();
                        meta.put("标题", q.getTitle());
                        meta.put("描述", q.getDescription() == null ? "—" : q.getDescription());
                        meta.put("状态", questionnaireStatusLabel(q.getStatus()));
                        return meta;
                    },
                    Questionnaire::getStatus));
            children.add(buildCategory("定时提醒", "reminder", reminderMap.get(a.getId()),
                    r -> r.getTitle() + (r.getTriggerTime() == null ? "" : " · " + r.getTriggerTime()),
                    r -> {
                        Map<String, Object> meta = new LinkedHashMap<>();
                        meta.put("标题", r.getTitle());
                        meta.put("内容", r.getContent() == null ? "—" : r.getContent());
                        meta.put("提醒对象", r.getTargetUserName() == null ? "—" : r.getTargetUserName());
                        meta.put("触发时间", r.getTriggerTime() == null ? "—" : r.getTriggerTime().toString());
                        meta.put("状态", reminderStatusLabel(r.getStatus()));
                        return meta;
                    },
                    Reminder::getStatus));
            actNode.setChildren(children);
            roots.add(actNode);
        }
        return roots;
    }

    @Override
    public int confirmTasks(Long id) {
        checkOwnership(id);
        Activity activity = getById(id);
        // 状态互斥：任务必须先经发起人确认发布，才能通知成员
        if (activity.getStatus() == null || activity.getStatus() != 2) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "请先确认并发布 AI 生成的内容，再通知成员");
        }
        List<TaskAssignment> tasks = taskService.listByActivity(id);
        if (tasks.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "当前活动还没有任务分工，无法确认");
        }
        // 按负责人分组（跳过未指定负责人的任务）
        Map<Long, List<TaskAssignment>> byAssignee = tasks.stream()
                .filter(t -> t.getAssigneeId() != null)
                .collect(Collectors.groupingBy(TaskAssignment::getAssigneeId));
        if (byAssignee.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "所有任务都未指定负责人，无法通知");
        }
        String activityTitle = activity.getTitle();
        byAssignee.forEach((assigneeId, myTasks) -> {
            StringBuilder content = new StringBuilder();
            content.append("你在活动「").append(activityTitle).append("」中被分配了 ")
                    .append(myTasks.size()).append(" 项任务：\n");
            int index = 1;
            for (TaskAssignment t : myTasks) {
                content.append(index++).append(". ").append(t.getTitle());
                if (t.getDeadline() != null) {
                    content.append("（截止：").append(t.getDeadline()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("）");
                }
                content.append("\n");
                if (t.getDescription() != null && !t.getDescription().isBlank()) {
                    content.append("   交付要求：").append(t.getDescription()).append("\n");
                }
            }
            content.append("请及时查看「任务分工」并按时完成。");
            messageService.send(assigneeId, "活动任务通知：" + activityTitle, content.toString(), id);
        });
        log.info("[活动] 分工已确认并通知, activityId={}, notified={}", id, byAssignee.size());
        return byAssignee.size();
    }

    /** 解析成员ID JSON → 昵称列表 */
    private List<String> resolveMemberNames(String memberIdsJson) {
        if (memberIdsJson == null || memberIdsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Long> ids = cn.hutool.json.JSONUtil.toList(memberIdsJson, Long.class);
            if (ids.isEmpty()) {
                return new ArrayList<>();
            }
            return userService.listByIds(ids).stream()
                    .map(u -> u.getNickname() == null ? u.getUsername() : u.getNickname())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[活动] 解析成员ID失败: {}", memberIdsJson);
            return new ArrayList<>();
        }
    }

    /** 把列表按 activityId 分组 */
    private <T> Map<Long, List<T>> groupByActivity(List<T> list) {
        if (list == null)
            return new HashMap<>();
        return list.stream().collect(Collectors.groupingBy(item -> {
            try {
                return (Long) item.getClass().getMethod("getActivityId").invoke(item);
            } catch (Exception e) {
                return 0L;
            }
        }));
    }

    /** 构建一个分类节点（任务分工/物料/问卷/提醒）及其子项 */
    private <T> ActivityBreakdownNodeVO buildCategory(String name, String type,
            List<T> items,
            java.util.function.Function<T, String> labelFn,
            java.util.function.Function<T, Map<String, Object>> metaFn,
            java.util.function.Function<T, Integer> statusFn) {
        ActivityBreakdownNodeVO cat = new ActivityBreakdownNodeVO();
        cat.setId("cat-" + type);
        cat.setName(name + (items == null ? " (0)" : " (" + items.size() + ")"));
        cat.setType("category");
        cat.setMeta(new LinkedHashMap<>());
        List<ActivityBreakdownNodeVO> kids = new ArrayList<>();
        if (items != null) {
            for (T item : items) {
                ActivityBreakdownNodeVO kid = new ActivityBreakdownNodeVO();
                kid.setId(type + "-" + extractId(item));
                kid.setName(labelFn.apply(item));
                kid.setType(type);
                kid.setStatus(statusFn.apply(item));
                kid.setMeta(metaFn.apply(item));
                kids.add(kid);
            }
        }
        cat.setChildren(kids);
        return cat;
    }

    private Long extractId(Object item) {
        try {
            return (Long) item.getClass().getMethod("getId").invoke(item);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String activityStatusLabel(Integer s) {
        if (s == null)
            return "未知";
        return switch (s) {
            case 0 -> "待策划";
            case 1 -> "策划中";
            case 2 -> "进行中";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case 5 -> "待确认";
            default -> "未知";
        };
    }

    private String taskStatusLabel(Integer s) {
        if (s == null)
            return "未知";
        return switch (s) {
            case 0 -> "待开始";
            case 1 -> "进行中";
            case 2 -> "已完成";
            case 3 -> "已逾期";
            default -> "未知";
        };
    }

    private String questionnaireStatusLabel(Integer s) {
        if (s == null)
            return "未知";
        return switch (s) {
            case 0 -> "未发布";
            case 1 -> "已发布";
            case 2 -> "已截止";
            default -> "未知";
        };
    }

    private String reminderStatusLabel(Integer s) {
        if (s == null)
            return "未知";
        return switch (s) {
            case 0 -> "待触发";
            case 1 -> "已发送";
            case 2 -> "已取消";
            case 3 -> "发送失败";
            default -> "未知";
        };
    }
}
