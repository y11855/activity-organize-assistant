package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.annotation.Idempotent;
import com.aiapp.activityassistant.common.dto.PageQuery;
import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.TaskAssignDTO;
import com.aiapp.activityassistant.service.TaskService;
import com.aiapp.activityassistant.vo.MyTaskVO;
import com.aiapp.activityassistant.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
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
@RequiresPermissions("task:view")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "派发任务")
    @PostMapping
    @Idempotent(prefix = "task:assign", ttl = 3)
    public Result<Long> assign(@Valid @RequestBody TaskAssignDTO dto) {
        return Result.success(taskService.assign(dto));
    }

    @Operation(summary = "批量派发任务")
    @PostMapping("/batch")
    @Idempotent(prefix = "task:batch-assign", ttl = 3)
    public Result<Void> batchAssign(@RequestParam Long activityId, @RequestBody List<TaskAssignDTO> tasks) {
        taskService.batchAssign(activityId, tasks);
        return Result.success();
    }

    @Operation(summary = "标记任务完成")
    @PutMapping("/{id}/complete")
    @Idempotent(prefix = "task:complete", ttl = 3)
    public Result<Void> complete(@PathVariable Long id) {
        taskService.complete(id);
        return Result.success();
    }

    @Operation(summary = "获取活动下的任务列表")
    @GetMapping
    public Result<List<TaskVO>> listByActivity(@RequestParam Long activityId) {
        List<TaskVO> result = taskService.listByActivity(activityId).stream().map(task -> {
            TaskVO vo = new TaskVO();
            BeanUtils.copyProperties(task, vo);
            return vo;
        }).toList();
        return Result.success(result);
    }

    @Operation(summary = "分页获取我的任务（仅分配给自己的，支持状态过滤）")
    @GetMapping("/mine")
    public Result<PageVO<MyTaskVO>> listMine(@RequestParam(required = false) Integer status, PageQuery query) {
        return Result.success(taskService.listMine(status, query));
    }
}
