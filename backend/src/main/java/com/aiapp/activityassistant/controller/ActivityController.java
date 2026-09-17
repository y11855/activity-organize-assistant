package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.ActivitySaveDTO;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.vo.ActivityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动管理
 */
@Tag(name = "活动管理")
@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "一句话发起活动（触发 Agent 全流程）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ActivitySaveDTO dto) {
        return Result.success(activityService.create(dto));
    }

    @Operation(summary = "获取我创建的活动列表")
    @GetMapping("/mine")
    public Result<List<ActivityVO>> listMine() {
        return Result.success(activityService.listMine());
    }

    @Operation(summary = "获取活动详情")
    @GetMapping("/{id}")
    public Result<ActivityVO> detail(@PathVariable Long id) {
        return Result.success(activityService.detail(id));
    }

    @Operation(summary = "取消活动")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        activityService.cancel(id);
        return Result.success();
    }

    @Operation(summary = "生成活动复盘总结")
    @PostMapping("/{id}/review")
    public Result<String> review(@PathVariable Long id) {
        return Result.success(activityService.generateReview(id));
    }
}
