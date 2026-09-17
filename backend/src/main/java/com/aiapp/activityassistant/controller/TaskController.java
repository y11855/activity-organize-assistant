package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.TaskAssignDTO;
import com.aiapp.activityassistant.entity.TaskAssignment;
import com.aiapp.activityassistant.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 任务分工
 */
@Tag(name = "任务分工")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "派发任务")
    @PostMapping
    public Result<Long> assign(@RequestBody TaskAssignDTO dto) {
        return Result.success(taskService.assign(dto));
    }

    @Operation(summary = "批量派发任务")
    @PostMapping("/batch")
    public Result<Void> batchAssign(@RequestParam Long activityId, @RequestBody List<TaskAssignDTO> tasks) {
        taskService.batchAssign(activityId, tasks);
        return Result.success();
    }

    @Operation(summary = "标记任务完成")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        taskService.complete(id);
        return Result.success();
    }

    @Operation(summary = "获取活动下的任务列表")
    @GetMapping
    public Result<List<TaskAssignment>> listByActivity(@RequestParam Long activityId) {
        return Result.success(taskService.listByActivity(activityId));
    }
}
