package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.service.DashboardService;
import com.aiapp.activityassistant.vo.DashboardOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据大屏：仅返回当前登录账号相关的数据
 */
@Tag(name = "数据大屏")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@RequiresPermissions("activity:view")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "大屏概览（当前账号维度）")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(dashboardService.overview(UserContext.getCurrentUserId()));
    }
}
