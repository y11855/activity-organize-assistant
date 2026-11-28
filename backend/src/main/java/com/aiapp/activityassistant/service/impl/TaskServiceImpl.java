package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.dto.PageQuery;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.mq.ActivityEventPublisher;
import com.aiapp.activityassistant.common.mq.ActivityEventType;
import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.TaskAssignDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.entity.TaskAssignment;
import com.aiapp.activityassistant.mapper.TaskAssignmentMapper;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.MessageService;
import com.aiapp.activityassistant.service.TaskService;
import com.aiapp.activityassistant.vo.MyTaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务实现：任务分工领域的具体逻辑（权限校验、派发、完成与缓存失效）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskAssignmentMapper, TaskAssignment>
        implements TaskService {

    /** 任务截止时间在提醒文案中的格式 */
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** 截止前提醒的提前量（小时） */
    private static final long REMIND_LEAD_HOURS = 1;

    /**
     * ActivityService 与本类存在构造器依赖环（ActivityService 详情聚合需要 TaskService），
     * 用字段注入 + @Lazy 打破：启动期只注入代理，首次调用时才解析真实 Bean。
     */
    @Lazy
    @Autowired
    private ActivityService activityService;

    private final ActivityEventPublisher eventPublisher;

    private final MessageService messageService;

    @Override
    public Long assign(TaskAssignDTO dto) {
        activityService.checkManage(dto.getActivityId(), "task:manage");
        TaskAssignment task = new TaskAssignment();
        BeanUtils.copyProperties(dto, task);
        task.setStatus(0);
        save(task);
        // 任务属于活动详情的一部分，失效详情缓存
        activityService.evictDetailCache(dto.getActivityId());
        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAssign(Long activityId, List<TaskAssignDTO> tasks) {
        activityService.checkManage(activityId, "task:manage");
        for (TaskAssignDTO dto : tasks) {
            dto.setActivityId(activityId);
            TaskAssignment task = new TaskAssignment();
            BeanUtils.copyProperties(dto, task);
            task.setStatus(0);
            save(task);
        }
        activityService.evictDetailCache(activityId);
    }

    @Override
    public void complete(Long id) {
        TaskAssignment task = getById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 权限隔离：仅任务负责人本人可标记完成；未指定负责人时仅活动创建人
        Long currentUserId = UserContext.getCurrentUserId();
        if (task.getAssigneeId() != null) {
            if (!task.getAssigneeId().equals(currentUserId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只能操作分配给自己的任务");
            }
        } else {
            activityService.checkOwnership(task.getActivityId());
        }
        task.setStatus(2);
        updateById(task);
        activityService.evictDetailCache(task.getActivityId());
        Activity activity = activityService.getById(task.getActivityId());
        if (activity != null) {
            eventPublisher.publish(ActivityEventType.TASK_COMPLETED, activity.getCreatorId(),
                    activity.getId(), "任务完成：" + task.getTitle());
        }
    }

    @Override
    public PageVO<MyTaskVO> listMine(Integer status, PageQuery query) {
        Long currentUserId = UserContext.getCurrentUserId();
        Page<TaskAssignment> page = page(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<TaskAssignment>()
                        .eq(TaskAssignment::getAssigneeId, currentUserId)
                        .eq(status != null, TaskAssignment::getStatus, status)
                        .orderByAsc(TaskAssignment::getStatus)
                        .orderByAsc(TaskAssignment::getDeadline));
        List<TaskAssignment> tasks = page.getRecords();
        if (tasks.isEmpty()) {
            return PageVO.of(new ArrayList<>(), page.getTotal(), page.getCurrent(), page.getSize());
        }
        // 批量补活动标题
        List<Long> activityIds = tasks.stream().map(TaskAssignment::getActivityId).distinct().toList();
        Map<Long, String> titleMap = activityService.listByIds(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, a -> a.getTitle() == null ? "" : a.getTitle()));
        List<MyTaskVO> records = tasks.stream().map(t -> {
            MyTaskVO vo = new MyTaskVO();
            BeanUtils.copyProperties(t, vo);
            vo.setActivityTitle(titleMap.getOrDefault(t.getActivityId(), ""));
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<TaskAssignment> listByActivity(Long activityId) {
        return list(new LambdaQueryWrapper<TaskAssignment>()
                .eq(TaskAssignment::getActivityId, activityId)
                .orderByAsc(TaskAssignment::getDeadline));
    }

    @Override
    public int remindUpcomingDeadlines() {
        LocalDateTime deadlineLine = LocalDateTime.now().plusHours(REMIND_LEAD_HOURS);
        // 未完成（待开始/进行中）且即将到期、尚未提醒过的任务
        List<TaskAssignment> upcoming = list(new LambdaQueryWrapper<TaskAssignment>()
                .in(TaskAssignment::getStatus, 0, 1)
                .isNotNull(TaskAssignment::getDeadline)
                .le(TaskAssignment::getDeadline, deadlineLine)
                .and(w -> w.isNull(TaskAssignment::getDeadlineReminded)
                        .or().eq(TaskAssignment::getDeadlineReminded, 0)));
        if (upcoming.isEmpty()) {
            return 0;
        }
        // 批量补活动标题，用于拼接提醒文案
        List<Long> activityIds = upcoming.stream().map(TaskAssignment::getActivityId).distinct().toList();
        Map<Long, String> titleMap = activityService.listByIds(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, a -> a.getTitle() == null ? "" : a.getTitle()));
        int reminded = 0;
        for (TaskAssignment task : upcoming) {
            // 未指定负责人的任务无需提醒，直接标记避免反复扫描
            if (task.getAssigneeId() == null) {
                task.setDeadlineReminded(1);
                updateById(task);
                continue;
            }
            try {
                messageService.send(task.getAssigneeId(),
                        "任务即将截止：" + task.getTitle(),
                        "你在活动「" + titleMap.getOrDefault(task.getActivityId(), "") + "」中的任务「"
                                + task.getTitle() + "」将于 " + task.getDeadline().format(DEADLINE_FORMAT)
                                + " 截止，请尽快完成。",
                        task.getActivityId());
                task.setDeadlineReminded(1);
                updateById(task);
                reminded++;
            } catch (Exception e) {
                // 发送失败不标记，下一分钟调度会自动重试
                log.error("[任务提醒] 发送失败, taskId={}, assigneeId={}", task.getId(), task.getAssigneeId(), e);
            }
        }
        if (reminded > 0) {
            log.info("[任务提醒] 本轮提醒 {} 个即将截止的任务", reminded);
        }
        return reminded;
    }
}
