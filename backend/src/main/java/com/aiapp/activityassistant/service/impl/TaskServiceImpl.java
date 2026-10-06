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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务分工业务实现
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskAssignmentMapper, TaskAssignment>
        implements TaskService {

    /**
     * ActivityService 与本类存在构造器依赖环（ActivityService 详情聚合需要 TaskService），
     * 用字段注入 + @Lazy 打破：启动期只注入代理，首次调用时才解析真实 Bean。
     */
    @Lazy
    @Autowired
    private ActivityService activityService;

    @Override
    public Long assign(TaskAssignDTO dto) {
        activityService.checkOwnership(dto.getActivityId());
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
        activityService.checkOwnership(activityId);
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
        if (task != null) {
            task.setStatus(2);
            updateById(task);
            activityService.evictDetailCache(task.getActivityId());
        }
    }

    @Override
    public List<TaskAssignment> listByActivity(Long activityId) {
        return list(new LambdaQueryWrapper<TaskAssignment>()
                .eq(TaskAssignment::getActivityId, activityId)
                .orderByAsc(TaskAssignment::getDeadline));
    }
}
