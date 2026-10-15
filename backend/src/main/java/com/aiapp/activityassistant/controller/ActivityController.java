package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.ActivityGenerateDTO;
import com.aiapp.activityassistant.dto.ActivitySaveDTO;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.vo.ActivityBreakdownNodeVO;
import com.aiapp.activityassistant.vo.ActivityDraftVO;
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
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(summary = "获取活动分解树（活动→任务/物料/问卷/提醒）")
    @GetMapping("/breakdown-tree")
    public Result<List<ActivityBreakdownNodeVO>> breakdownTree() {
        return Result.success(activityService.breakdownTree());
    }

    @Operation(summary = "获取活动详情")
    @GetMapping("/{id}")
    public Result<ActivityVO> detail(@PathVariable Long id) {
        return Result.success(activityService.detail(id));
    }

    @Operation(summary = "取消活动（可选是否删除活动记录；问卷自动截止）")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
            @RequestParam(value = "deleteRecord", defaultValue = "false") boolean deleteRecord) {
        activityService.cancel(id, deleteRecord);
        return Result.success();
    }

    @Operation(summary = "触发 AI 生成活动内容草稿（按勾选的生成项）")
    @PostMapping("/{id}/generate")
    public Result<Void> generate(@PathVariable Long id, @RequestBody ActivityGenerateDTO options) {
        activityService.generate(id, options);
        return Result.success();
    }

    @Operation(summary = "获取 AI 生成的内容草稿（预览/编辑）")
    @GetMapping("/{id}/draft")
    public Result<ActivityDraftVO> draft(@PathVariable Long id) {
        return Result.success(activityService.draft(id));
    }

    @Operation(summary = "保存发起人对 AI 生成内容的编辑")
    @PutMapping("/{id}/draft")
    public Result<Void> updateDraft(@PathVariable Long id, @RequestBody ActivityDraftVO draft) {
        activityService.updateDraft(id, draft);
        return Result.success();
    }

    @Operation(summary = "确认发布：正式发布任务分工/物料/问卷/提醒")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        activityService.publish(id);
        return Result.success();
    }

    @Operation(summary = "生成活动复盘总结")
    @PostMapping("/{id}/review")
    public Result<String> review(@PathVariable Long id) {
        return Result.success(activityService.generateReview(id));
    }

    @Operation(summary = "确认任务分工并通知成员")
    @PostMapping("/{id}/confirm-tasks")
    public Result<Integer> confirmTasks(@PathVariable Long id) {
        return Result.success(activityService.confirmTasks(id));
    }
}
