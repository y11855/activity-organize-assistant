package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.dto.TaskAssignDTO;
import com.aiapp.activityassistant.entity.TaskAssignment;
import com.aiapp.activityassistant.mapper.TaskAssignmentMapper;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务分工业务实现
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskAssignmentMapper, TaskAssignment>
        implements TaskService {

    private final ActivityService activityService;

    @Override
    public Long assign(TaskAssignDTO dto) {
        activityService.checkOwnership(dto.getActivityId());
        TaskAssignment task = new TaskAssignment();
        BeanUtils.copyProperties(dto, task);
        task.setStatus(0);
        task.setCreateTime(LocalDateTime.now());
        save(task);
        return task.getId();
    }

    @Override
    public void batchAssign(Long activityId, List<TaskAssignDTO> tasks) {
        activityService.checkOwnership(activityId);
        for (TaskAssignDTO dto : tasks) {
            dto.setActivityId(activityId);
            assign(dto);
        }
    }

    @Override
    public void complete(Long id) {
        TaskAssignment task = getById(id);
        if (task != null) {
            task.setStatus(2);
            updateById(task);
        }
    }

    @Override
    public List<TaskAssignment> listByActivity(Long activityId) {
        return list(new LambdaQueryWrapper<TaskAssignment>()
                .eq(TaskAssignment::getActivityId, activityId)
                .orderByAsc(TaskAssignment::getDeadline));
    }
}
