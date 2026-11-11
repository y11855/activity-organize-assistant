package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.common.utils.IdempotentKeyGenerator;
import com.aiapp.activityassistant.dto.ReminderCreateDTO;
import com.aiapp.activityassistant.entity.Reminder;
import com.aiapp.activityassistant.mapper.ReminderMapper;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.ReminderService;
import com.aiapp.activityassistant.tool.ToolDispatcher;
import com.aiapp.activityassistant.vo.ReminderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提醒业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl extends ServiceImpl<ReminderMapper, Reminder>
        implements ReminderService {

    private final ActivityService activityService;
    private final ToolDispatcher toolDispatcher;
    private final IdempotentKeyGenerator idempotentKeyGenerator;

    /** 提醒失败后的最大重试次数（每次调度最多再尝试 1 次，配合 ToolDispatcher 内部同步重试） */
    private static final int MAX_RETRY = 3;

    @Override
    public Long create(ReminderCreateDTO dto) {
        // 活动成员即可创建提醒（不限制只有负责人）；默认目标为自己
        activityService.checkAccess(dto.getActivityId());
        Reminder reminder = new Reminder();
        BeanUtils.copyProperties(dto, reminder);
        if (reminder.getTargetUserId() == null) {
            reminder.setTargetUserId(UserContext.getCurrentUserId());
        }
        reminder.setStatus(0);
        reminder.setRetryCount(0);
        // 幂等键：活动 + 目标用户 + 触发时间 + 标题
        reminder.setIdempotentKey(idempotentKeyGenerator.generate(
                "reminder", String.valueOf(dto.getActivityId()),
                String.valueOf(reminder.getTargetUserId()),
                String.valueOf(dto.getTriggerTime()),
                dto.getTitle()
        ));
        save(reminder);
        return reminder.getId();
    }

    @Override
    public void cancel(Long id) {
        Reminder reminder = getById(id);
        if (reminder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 权限：活动负责人可取消，或提醒的目标用户可取消发给自己的提醒
        boolean isTarget = reminder.getTargetUserId() != null
                && reminder.getTargetUserId().equals(UserContext.getCurrentUserId());
        if (!isTarget) {
            activityService.checkManage(reminder.getActivityId(), "reminder:manage");
        }
        // 状态：0待触发 1已发送 2已取消 3发送失败；只有「待触发」才允许取消
        Integer status = reminder.getStatus();
        if (status != null && status == 2) {
            throw new BusinessException(ResultCode.REMINDER_ALREADY_CANCELLED);
        }
        if (status == null || status != 0) {
            throw new BusinessException(ResultCode.REMINDER_NOT_CANCELLABLE);
        }
        reminder.setStatus(2);
        updateById(reminder);
        log.info("[提醒] 已取消提醒, id={}", id);
    }

    @Override
    public List<ReminderVO> listByActivity(Long activityId) {
        activityService.checkAccess(activityId);
        List<Reminder> list = list(new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getActivityId, activityId)
                .orderByAsc(Reminder::getTriggerTime));
        return list.stream().map(r -> {
            ReminderVO vo = new ReminderVO();
            BeanUtils.copyProperties(r, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void triggerDueReminders() {
        // 两类提醒需要处理：
        // 1. 待触发且已到时间（status=0 AND trigger_time <= now）
        // 2. 发送失败且未超过最大重试次数（status=3 AND retry_count < MAX_RETRY）
        List<Reminder> dueList = list(new LambdaQueryWrapper<Reminder>()
                .and(w -> w.eq(Reminder::getStatus, 0)
                        .le(Reminder::getTriggerTime, LocalDateTime.now()))
                .or(w -> w.eq(Reminder::getStatus, 3)
                        .lt(Reminder::getRetryCount, MAX_RETRY)));

        for (Reminder reminder : dueList) {
            try {
                // 通过消息工具发送（落站内信），工具调度器保证幂等 + 同步重试
                toolDispatcher.dispatch(reminder.getActivityId(), "message", Map.of(
                        "targetUserId", reminder.getTargetUserId(),
                        "targetUserName", reminder.getTargetUserName() == null ? "" : reminder.getTargetUserName(),
                        "title", reminder.getTitle(),
                        "content", reminder.getContent() == null ? "" : reminder.getContent(),
                        "activityId", reminder.getActivityId()
                ));
                reminder.setStatus(1);
                reminder.setRetryCount(0);
            } catch (Exception e) {
                log.error("[提醒] 发送失败, reminderId={}, retryCount={}", reminder.getId(), reminder.getRetryCount(), e);
                reminder.setRetryCount(reminder.getRetryCount() + 1);
                // status 保持为失败；retry_count < MAX 时下一轮调度会自动重试，达到上限后不再被捞起
                reminder.setStatus(3);
            }
            updateById(reminder);
        }
        if (!dueList.isEmpty()) {
            log.info("[提醒] 本轮处理 {} 条提醒", dueList.size());
        }
    }

    @Override
    public List<ReminderVO> listMine() {
        Long userId = UserContext.getCurrentUserId();
        List<Reminder> list = list(new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getTargetUserId, userId)
                .orderByDesc(Reminder::getTriggerTime));
        return list.stream().map(r -> {
            ReminderVO vo = new ReminderVO();
            BeanUtils.copyProperties(r, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
