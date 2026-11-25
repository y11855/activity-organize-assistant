package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.TaskAssignment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：任务分工表（task_assignment）的数据访问接口
 */
@Mapper
public interface TaskAssignmentMapper extends BaseMapper<TaskAssignment> {
}
