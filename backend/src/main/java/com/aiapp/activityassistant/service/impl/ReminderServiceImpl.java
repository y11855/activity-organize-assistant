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

    @Override
    public Long create(ReminderCreateDTO dto) {
        activityService.checkOwnership(dto.getActivityId());
        Reminder reminder = new Reminder();
        BeanUtils.copyProperties(dto, reminder);
        reminder.setStatus(0);
        reminder.setRetryCount(0);
        // 幂等键：活动 + 目标用户 + 触发时间 + 标题
        reminder.setIdempotentKey(idempotentKeyGenerator.generate(
                "reminder", String.valueOf(dto.getActivityId()),
                String.valueOf(dto.getTargetUserId()),
                String.valueOf(dto.getTriggerTime()),
                dto.getTitle()
        ));
        reminder.setCreateTime(LocalDateTime.now());
        save(reminder);
        return reminder.getId();
    }

    @Override
    public void cancel(Long id) {
        Reminder reminder = getById(id);
        if (reminder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 权限校验：只能取消自己活动的提醒
        activityService.checkOwnership(reminder.getActivityId());
        if (reminder.getStatus() != 0) {
            throw new BusinessException(ResultCode.REMINDER_ALREADY_CANCELLED);
        }
        reminder.setStatus(2);
        updateById(reminder);
        log.info("[提醒] 已取消提醒, id={}", id);
    }

    @Override
    public List<ReminderVO> listByActivity(Long activityId) {
        activityService.checkOwnership(activityId);
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
        // 查询所有待触发且已到触发时间的提醒
        List<Reminder> dueList = list(new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getStatus, 0)
                .le(Reminder::getTriggerTime, LocalDateTime.now()));

        for (Reminder reminder : dueList) {
            try {
                // 通过消息工具发送，工具调度器保证幂等 + 重试
                toolDispatcher.dispatch(reminder.getActivityId(), "message", Map.of(
                        "targetUserId", reminder.getTargetUserId(),
                        "targetUserName", reminder.getTargetUserName(),
                        "title", reminder.getTitle(),
                        "content", reminder.getContent()
                ));
                reminder.setStatus(1);
            } catch (Exception e) {
                log.error("[提醒] 发送失败, reminderId={}", reminder.getId(), e);
                reminder.setStatus(3);
                reminder.setRetryCount(reminder.getRetryCount() + 1);
            }
            updateById(reminder);
        }
        if (!dueList.isEmpty()) {
            log.info("[提醒] 本轮处理 {} 条到期提醒", dueList.size());
        }
    }
}
