package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.service.QuestionnaireService;
import com.aiapp.activityassistant.vo.QuestionnaireVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 问卷管理
 */
@Tag(name = "问卷管理")
@RestController
@RequestMapping("/questionnaires")
@RequiredArgsConstructor
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    @Operation(summary = "为活动生成问卷")
    @PostMapping("/generate")
    public Result<Long> generate(@RequestParam Long activityId) {
        return Result.success(questionnaireService.generate(activityId));
    }

    @Operation(summary = "获取活动问卷详情")
    @GetMapping
    public Result<QuestionnaireVO> detailByActivity(@RequestParam Long activityId) {
        return Result.success(questionnaireService.detailByActivity(activityId));
    }

    @Operation(summary = "发布问卷")
    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        questionnaireService.publish(id);
        return Result.success();
    }

    @Operation(summary = "截止问卷")
    @PutMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        questionnaireService.close(id);
        return Result.success();
    }

    @Operation(summary = "获取活动问卷列表")
    @GetMapping("/list")
    public Result<List<QuestionnaireVO>> listByActivity(@RequestParam Long activityId) {
        return Result.success(questionnaireService.listByActivity(activityId));
    }
}
