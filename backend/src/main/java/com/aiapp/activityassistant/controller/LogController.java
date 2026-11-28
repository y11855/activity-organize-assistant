package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.annotation.Idempotent;
import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.LogQuery;
import com.aiapp.activityassistant.service.LogService;
import com.aiapp.activityassistant.vo.LogOverviewVO;
import com.aiapp.activityassistant.vo.SysLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Spring Web 控制器：日志中心（概览统计、日志分页、模块筛选与清空）。
 */
@Tag(name = "日志中心")
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@RequiresPermissions("log:view")
public class LogController {

    private final LogService logService;

    @Operation(summary = "分页查询日志")
    @GetMapping("/page")
    public Result<PageVO<SysLogVO>> page(LogQuery query) {
        return Result.success(logService.page(query));
    }

    @Operation(summary = "日志概览统计")
    @GetMapping("/overview")
    public Result<LogOverviewVO> overview() {
        return Result.success(logService.overview());
    }

    @Operation(summary = "日志模块列表（筛选项）")
    @GetMapping("/modules")
    public Result<List<String>> modules() {
        return Result.success(logService.modules());
    }

    @Operation(summary = "清空全部日志")
    @DeleteMapping
    @RequiresPermissions("log:manage")
    @Idempotent(prefix = "log:clear", ttl = 3)
    public Result<Void> clear() {
        logService.clear();
        return Result.success();
    }
}
