package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.entity.Message;
import com.aiapp.activityassistant.entity.QuestionnaireResponse;
import com.aiapp.activityassistant.entity.Reminder;
import com.aiapp.activityassistant.entity.TaskAssignment;
import com.aiapp.activityassistant.mapper.ActivityMapper;
import com.aiapp.activityassistant.mapper.MessageMapper;
import com.aiapp.activityassistant.mapper.QuestionnaireResponseMapper;
import com.aiapp.activityassistant.mapper.ReminderMapper;
import com.aiapp.activityassistant.mapper.TaskAssignmentMapper;
import com.aiapp.activityassistant.service.DashboardService;
import com.aiapp.activityassistant.vo.DashboardOverviewVO;
import com.aiapp.activityassistant.vo.DashboardOverviewVO.EventItem;
import com.aiapp.activityassistant.vo.DashboardOverviewVO.StatusItem;
import com.aiapp.activityassistant.vo.DashboardOverviewVO.TrendItem;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据大屏业务实现：所有查询都以传入 userId 为归属人，不返回他人数据。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    /** 概览缓存 60 秒；消息队列事件会立即失效，保证大屏近实时 */
    private static final String CACHE_KEY = "dashboard:overview:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    /** 实时动态：每人一个 Redis List，只保留最近 20 条 */
    private static final String FEED_KEY = "dashboard:feed:";
    private static final int FEED_SIZE = 20;
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");

    private final ActivityMapper activityMapper;
    private final QuestionnaireResponseMapper responseMapper;
    private final TaskAssignmentMapper taskMapper;
    private final ReminderMapper reminderMapper;
    private final MessageMapper messageMapper;
    private final RedisService redisService;

    @Override
    public DashboardOverviewVO overview(Long userId) {
        Object cached = redisService.get(CACHE_KEY + userId);
        if (cached instanceof DashboardOverviewVO vo) {
            return vo;
        }
        DashboardOverviewVO vo = build(userId);
        redisService.set(CACHE_KEY + userId, vo, CACHE_TTL);
        return vo;
    }

    @Override
    public void recordEvent(Long ownerId, String type, String content) {
        Map<String, String> item = new HashMap<>();
        item.put("type", type);
        item.put("content", content);
        item.put("time", LocalDateTime.now().format(TIME_FORMAT));
        redisService.pushCapped(FEED_KEY + ownerId, item, FEED_SIZE);
        redisService.delete(CACHE_KEY + ownerId);
    }

    private DashboardOverviewVO build(Long userId) {
        // 1. 我创建的活动：一次查询后内存统计，避免逐状态查库
        List<Activity> activities = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCreatorId, userId));
        List<Long> activityIds = activities.stream().map(Activity::getId).toList();
        Map<Integer, Long> byStatus = activities.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatus() == null ? 0 : a.getStatus(), Collectors.counting()));

        DashboardOverviewVO vo = new DashboardOverviewVO();
        vo.setActivityTotal((long) activities.size());
        vo.setRunningCount(byStatus.getOrDefault(2, 0L));
        vo.setCompletedCount(byStatus.getOrDefault(3, 0L));
        vo.setPendingCount(byStatus.getOrDefault(0, 0L)
                + byStatus.getOrDefault(1, 0L) + byStatus.getOrDefault(5, 0L));
        vo.setCancelledCount(byStatus.getOrDefault(4, 0L));
        vo.setStatusDistribution(List.of(
                new StatusItem("待策划", byStatus.getOrDefault(0, 0L)),
                new StatusItem("策划中", byStatus.getOrDefault(1, 0L)),
                new StatusItem("待确认", byStatus.getOrDefault(5, 0L)),
                new StatusItem("进行中", byStatus.getOrDefault(2, 0L)),
                new StatusItem("已完成", byStatus.getOrDefault(3, 0L)),
                new StatusItem("已取消", byStatus.getOrDefault(4, 0L))));

        // 2. 我活动下的有效报名（status=1）
        List<QuestionnaireResponse> responses = activityIds.isEmpty() ? List.of()
                : responseMapper.selectList(new LambdaQueryWrapper<QuestionnaireResponse>()
                        .in(QuestionnaireResponse::getActivityId, activityIds)
                        .eq(QuestionnaireResponse::getStatus, 1));
        vo.setRegistrationTotal((long) responses.size());

        // 3. 我活动下的任务完成率
        List<TaskAssignment> tasks = activityIds.isEmpty() ? List.of()
                : taskMapper.selectList(new LambdaQueryWrapper<TaskAssignment>()
                        .in(TaskAssignment::getActivityId, activityIds));
        long completed = tasks.stream().filter(t -> Integer.valueOf(2).equals(t.getStatus())).count();
        vo.setTaskTotal((long) tasks.size());
        vo.setTaskCompleted(completed);
        vo.setTaskCompletionRate(tasks.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / tasks.size()));

        // 4. 与我个人直接相关的待办 / 提醒 / 未读消息
        vo.setMyTodoCount(taskMapper.selectCount(new LambdaQueryWrapper<TaskAssignment>()
                .eq(TaskAssignment::getAssigneeId, userId)
                .ne(TaskAssignment::getStatus, 2)));
        vo.setReminderCount(reminderMapper.selectCount(new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getTargetUserId, userId)));
        vo.setUnreadMessages(messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getReadStatus, 0)));

        vo.setRegistrationTrend(buildTrend(responses));
        vo.setRecentEvents(readFeed(userId));
        return vo;
    }

    /** 近 7 天报名趋势，缺失日期补 0 */
    private List<TrendItem> buildTrend(List<QuestionnaireResponse> responses) {
        Map<LocalDate, Long> byDay = responses.stream()
                .filter(r -> r.getSubmitTime() != null)
                .collect(Collectors.groupingBy(r -> r.getSubmitTime().toLocalDate(), Collectors.counting()));
        List<TrendItem> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            trend.add(new TrendItem(day.format(DAY_FORMAT), byDay.getOrDefault(day, 0L)));
        }
        return trend;
    }

    private List<EventItem> readFeed(Long userId) {
        return redisService.range(FEED_KEY + userId, 0, FEED_SIZE - 1).stream()
                .filter(Map.class::isInstance)
                .map(value -> {
                    Map<?, ?> item = (Map<?, ?>) value;
                    return new EventItem(String.valueOf(item.get("type")),
                            String.valueOf(item.get("content")),
                            String.valueOf(item.get("time")));
                })
                .toList();
    }
}
