package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.ActivityEventLogSaveDTO;
import com.aiapp.activityassistant.service.ActivityEventLogService;
import com.aiapp.activityassistant.vo.ActivityEventLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * 活动事件日志（活动进行期间记录事件/亮点/问题，用于 AI 复盘）
 */
@Tag(name = "活动事件日志")
@RestController
@RequestMapping("/activities/{activityId}/event-logs")
@RequiredArgsConstructor
public class ActivityEventLogController {

    private final ActivityEventLogService eventLogService;

    @Operation(summary = "记录一条活动事件")
    @PostMapping
    public Result<Long> add(@PathVariable Long activityId, @Valid @RequestBody ActivityEventLogSaveDTO dto) {
        return Result.success(eventLogService.add(activityId, dto));
    }

    @Operation(summary = "活动事件列表（按发生时间倒序）")
    @GetMapping
    public Result<List<ActivityEventLogVO>> list(@PathVariable Long activityId,
            @RequestParam(required = false) Integer eventType) {
        return Result.success(eventLogService.listByActivity(activityId, eventType));
    }

    @Operation(summary = "删除一条活动事件")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long activityId, @PathVariable Long id) {
        eventLogService.delete(activityId, id);
        return Result.success();
    }
}
