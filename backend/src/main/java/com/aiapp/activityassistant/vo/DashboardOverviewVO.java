package com.aiapp.activityassistant.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据大屏概览（当前登录账号维度）
 */
@Data
public class DashboardOverviewVO {

    /** 我创建的活动总数 */
    private Long activityTotal;
    /** 进行中（status=2） */
    private Long runningCount;
    /** 已完成（status=3） */
    private Long completedCount;
    /** 待处理：待策划 + 策划中 + 待确认 */
    private Long pendingCount;
    private Long cancelledCount;

    /** 我创建活动下的有效报名总数 */
    private Long registrationTotal;
    /** 我创建活动下的任务总数 / 已完成数 */
    private Long taskTotal;
    private Long taskCompleted;
    /** 任务完成率（百分比整数） */
    private Integer taskCompletionRate;
    /** 分配给我且未完成的任务数 */
    private Long myTodoCount;
    /** 发给我的提醒数 */
    private Long reminderCount;
    /** 我的未读消息数 */
    private Long unreadMessages;

    /** 活动状态分布（图表数据） */
    private List<StatusItem> statusDistribution;
    /** 近 7 天报名趋势 */
    private List<TrendItem> registrationTrend;
    /** 实时动态（来自消息队列） */
    private List<EventItem> recentEvents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusItem {
        private String name;
        private Long value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        private String date;
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventItem {
        private String type;
        private String content;
        private String time;
    }
}
