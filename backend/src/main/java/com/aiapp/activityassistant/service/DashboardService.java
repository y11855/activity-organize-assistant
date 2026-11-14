package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.vo.DashboardOverviewVO;

/**
 * 数据大屏业务接口（数据范围限定为传入账号本人）
 */
public interface DashboardService {

    /** 大屏概览：Redis 缓存 60 秒，消息队列事件会主动失效 */
    DashboardOverviewVO overview(Long userId);

    /** 记录实时动态并失效大屏缓存（由消息队列消费者调用） */
    void recordEvent(Long ownerId, String type, String content);
}
