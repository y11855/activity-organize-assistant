package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.ReminderCreateDTO;
import com.aiapp.activityassistant.service.ReminderService;
import com.aiapp.activityassistant.vo.ReminderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提醒管理
 */
@Tag(name = "提醒管理")
@RestController
@RequestMapping("/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @Operation(summary = "创建提醒")
    @PostMapping
    public Result<Long> create(@RequestBody ReminderCreateDTO dto) {
        return Result.success(reminderService.create(dto));
    }

    @Operation(summary = "取消提醒（活动前定时提醒可取消）")
    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        reminderService.cancel(id);
        return Result.success();
    }

    @Operation(summary = "获取活动下的提醒列表")
    @GetMapping
    public Result<List<ReminderVO>> listByActivity(@RequestParam Long activityId) {
        return Result.success(reminderService.listByActivity(activityId));
    }
}
