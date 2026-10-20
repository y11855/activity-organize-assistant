package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.dto.ActivityEventLogSaveDTO;
import com.aiapp.activityassistant.vo.ActivityEventLogVO;

import java.util.List;

/**
 * 活动事件日志业务接口
 */
public interface ActivityEventLogService {

    /** 记录一条事件（仅活动创建人） */
    Long add(Long activityId, ActivityEventLogSaveDTO dto);

    /** 活动事件列表（按发生时间倒序），可按事件类型过滤 */
    List<ActivityEventLogVO> listByActivity(Long activityId, Integer eventType);

    /** 删除一条事件（仅创建人） */
    void delete(Long activityId, Long id);
}
