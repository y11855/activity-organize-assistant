package com.aiapp.activityassistant.config;

import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.mapper.ActivityMapper;
import com.aiapp.activityassistant.service.DashboardService;
import com.aiapp.activityassistant.service.RbacService;
import com.aiapp.activityassistant.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动预热：把高频读取的数据提前写入 Redis（内存数据库），
 * 避免用户首次请求时集中查库。
 * <p>1. 启用用户的 RBAC 角色/权限码缓存（每个请求都会用到，30 分钟过期）<br>
 * 2. 活动创建人的大屏概览基线（供数据大屏首屏秒开）</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupWarmupRunner implements ApplicationRunner {

    private final UserService userService;
    private final RbacService rbacService;
    private final DashboardService dashboardService;
    private final ActivityMapper activityMapper;

    @Override
    public void run(ApplicationArguments args) {
        long start = System.currentTimeMillis();
        try {
            List<User> users = userService.list(new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
            for (User user : users) {
                rbacService.listRoleCodes(user.getId());
                rbacService.listPermissionCodes(user.getId());
            }

            List<Long> creatorIds = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                            .select(Activity::getCreatorId)
                            .isNotNull(Activity::getCreatorId))
                    .stream().map(Activity::getCreatorId).distinct().toList();
            creatorIds.forEach(dashboardService::overview);

            log.info("[预热] 完成：RBAC 用户 {} 个，大屏基线 {} 个，耗时 {}ms",
                    users.size(), creatorIds.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 预热失败不影响服务启动，缓存会在首次访问时按需加载
            log.warn("[预热] 启动预热失败，不影响服务启动", e);
        }
    }
}
