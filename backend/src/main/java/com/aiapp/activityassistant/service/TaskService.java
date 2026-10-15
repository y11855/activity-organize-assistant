package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.dto.TaskAssignDTO;
import com.aiapp.activityassistant.entity.TaskAssignment;
import com.aiapp.activityassistant.vo.MyTaskVO;
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
     * 完成任务（仅任务负责人本人；未指定负责人时仅活动创建人）
     */
    void complete(Long id);

    /**
     * 我的任务列表（仅当前用户被分配的任务，权限隔离）
     *
     * @param status 可选状态过滤
     */
    List<MyTaskVO> listMine(Integer status);

    /**
     * 获取活动下的任务列表
     */
    List<TaskAssignment> listByActivity(Long activityId);
}
