package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.BehaviorReportDTO;
import com.aiapp.activityassistant.service.QuestionnaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring Web 控制器：用户行为采集（问卷页停留时长、点赞），数据经 Kafka 交给 Flink 计算推荐。
 */
@Tag(name = "用户行为采集")
@RestController
@RequestMapping("/behaviors")
@RequiredArgsConstructor
public class BehaviorController {

    private final QuestionnaireService questionnaireService;

    @Operation(summary = "上报用户行为（停留时长 / 点赞）")
    @PostMapping
    public Result<Void> report(@Valid @RequestBody BehaviorReportDTO dto) {
        questionnaireService.reportBehavior(dto);
        return Result.success();
    }
}
