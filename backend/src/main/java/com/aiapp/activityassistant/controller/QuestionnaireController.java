package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.QuestionnaireSaveDTO;
import com.aiapp.activityassistant.dto.ResponseSubmitDTO;
import com.aiapp.activityassistant.service.QuestionnaireService;
import com.aiapp.activityassistant.vo.QuestionnaireVO;
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

    @Operation(summary = "编辑问卷（含封面）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody QuestionnaireSaveDTO dto) {
        questionnaireService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "我的问卷列表（当前用户所有活动下）")
    @GetMapping("/mine")
    public Result<List<QuestionnaireVO>> listMine() {
        return Result.success(questionnaireService.listMine());
    }

    @Operation(summary = "按 ID 获取问卷详情（填写页）")
    @GetMapping("/{id}")
    public Result<QuestionnaireVO> detail(@PathVariable Long id) {
        return Result.success(questionnaireService.detail(id));
    }

    @Operation(summary = "提交答卷")
    @PostMapping("/{id}/responses")
    public Result<Void> submitResponse(@PathVariable Long id, @Valid @RequestBody ResponseSubmitDTO dto) {
        questionnaireService.submitResponse(id, dto);
        return Result.success();
    }
}
