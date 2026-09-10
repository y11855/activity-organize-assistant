package com.aiapp.trainingeval.controller;

import com.aiapp.trainingeval.common.result.Result;
import com.aiapp.trainingeval.dto.TrainingSaveDTO;
import com.aiapp.trainingeval.service.TrainingService;
import com.aiapp.trainingeval.vo.TrainingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 实训管理
 */
@Tag(name = "实训管理")
@RestController
@RequestMapping("/trainings")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @Operation(summary = "提交实训成果")
    @PostMapping
    public Result<Long> submit(@Valid @RequestBody TrainingSaveDTO dto) {
        return Result.success(trainingService.submit(dto));
    }

    @Operation(summary = "获取实训详情")
    @GetMapping("/{id}")
    public Result<TrainingVO> detail(@PathVariable Long id) {
        return Result.success(trainingService.detail(id));
    }

    @Operation(summary = "触发 AI 校验")
    @PostMapping("/{id}/evaluate")
    public Result<Void> evaluate(@PathVariable Long id) {
        trainingService.triggerAiEvaluate(id);
        return Result.success();
    }
}
