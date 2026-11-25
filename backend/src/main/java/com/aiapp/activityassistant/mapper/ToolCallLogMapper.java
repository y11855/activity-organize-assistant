package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.ToolCallLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：工具调用日志表（tool_call_log）的数据访问接口
 */
@Mapper
public interface ToolCallLogMapper extends BaseMapper<ToolCallLog> {
}
