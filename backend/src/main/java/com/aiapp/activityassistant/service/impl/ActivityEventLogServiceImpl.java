package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.ActivityEventLogSaveDTO;
import com.aiapp.activityassistant.entity.ActivityEventLog;
import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.mapper.ActivityEventLogMapper;
import com.aiapp.activityassistant.service.ActivityEventLogService;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.UserService;
import com.aiapp.activityassistant.vo.ActivityEventLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityEventLogServiceImpl implements ActivityEventLogService {

    private final ActivityEventLogMapper eventLogMapper;
    private final ActivityService activityService;
    private final UserService userService;

    @Override
    public Long add(Long activityId, ActivityEventLogSaveDTO dto) {
        activityService.checkOwnership(activityId);
        ActivityEventLog log = new ActivityEventLog();
        BeanUtils.copyProperties(dto, log);
        log.setActivityId(activityId);
        log.setRecorderId(UserContext.getCurrentUserId());
        if (dto.getOccurredAt() == null) {
            log.setOccurredAt(LocalDateTime.now());
        }
        eventLogMapper.insert(log);
        return log.getId();
    }

    @Override
    public List<ActivityEventLogVO> listByActivity(Long activityId, Integer eventType) {
        activityService.checkOwnership(activityId);
        LambdaQueryWrapper<ActivityEventLog> qw = new LambdaQueryWrapper<ActivityEventLog>()
                .eq(ActivityEventLog::getActivityId, activityId)
                .orderByDesc(ActivityEventLog::getOccurredAt);
        if (eventType != null) {
            qw.eq(ActivityEventLog::getEventType, eventType);
        }
        List<ActivityEventLog> list = eventLogMapper.selectList(qw);
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量查记录人姓名，避免 N+1
        Set<Long> recorderIds = list.stream()
                .map(ActivityEventLog::getRecorderId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = recorderIds.isEmpty() ? Map.of()
                : userService.listByIds(recorderIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u.getNickname() == null ? "" : u.getNickname()));
        return list.stream().map(log -> {
            ActivityEventLogVO vo = new ActivityEventLogVO();
            BeanUtils.copyProperties(log, vo);
            vo.setRecorderName(nameMap.getOrDefault(log.getRecorderId(), ""));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void delete(Long activityId, Long id) {
        activityService.checkOwnership(activityId);
        ActivityEventLog log = eventLogMapper.selectById(id);
        if (log == null || !log.getActivityId().equals(activityId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        eventLogMapper.deleteById(id);
    }
}
