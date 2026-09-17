package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.dto.TaskAssignDTO;
import com.aiapp.activityassistant.entity.TaskAssignment;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 任务分工业务接口
 */
public interface TaskService extends IService<TaskAssignment> {

    /**
     * 派发任务
     */
    Long assign(TaskAssignDTO dto);

    /**
     * 批量派发任务
     */
    void batchAssign(Long activityId, List<TaskAssignDTO> tasks);

    /**
     * 完成任务
     */
    void complete(Long id);

    /**
     * 获取活动下的任务列表
     */
    List<TaskAssignment> listByActivity(Long activityId);
}
