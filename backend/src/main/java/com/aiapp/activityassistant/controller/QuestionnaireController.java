package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.annotation.Idempotent;
import com.aiapp.activityassistant.common.dto.PageQuery;
import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.QuestionnaireSaveDTO;
import com.aiapp.activityassistant.dto.ResponseSubmitDTO;
import com.aiapp.activityassistant.service.QuestionnaireService;
import com.aiapp.activityassistant.vo.QuestionnaireResponseVO;
import com.aiapp.activityassistant.vo.QuestionnaireVO;
import com.aiapp.activityassistant.vo.ResponseStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 问卷管理
 */
@Tag(name = "问卷管理")
@RestController
@RequestMapping("/questionnaires")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('questionnaire:view')")
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    @Operation(summary = "为活动生成问卷")
    @PostMapping("/generate")
    @Idempotent(prefix = "questionnaire:generate", ttl = 5)
    public Result<Long> generate(@RequestParam Long activityId) {
        return Result.success(questionnaireService.generate(activityId));
    }

    @Operation(summary = "按活动查询问卷列表")
    @GetMapping
    public Result<List<QuestionnaireVO>> listByActivity(@RequestParam Long activityId) {
        return Result.success(questionnaireService.listByActivity(activityId));
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

    @Operation(summary = "编辑问卷（含封面）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody QuestionnaireSaveDTO dto) {
        questionnaireService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "分页获取我的问卷列表（当前用户所有活动下）")
    @GetMapping("/mine")
    public Result<PageVO<QuestionnaireVO>> listMine(PageQuery query) {
        return Result.success(questionnaireService.listMine(query));
    }

    @Operation(summary = "分页获取全部已发布问卷（所有登录用户可查看与填写）")
    @GetMapping("/published")
    public Result<PageVO<QuestionnaireVO>> listPublished(PageQuery query) {
        return Result.success(questionnaireService.listPublished(query));
    }

    @Operation(summary = "按 ID 获取问卷详情（填写页）")
    @GetMapping("/{id}")
    public Result<QuestionnaireVO> detail(@PathVariable Long id) {
        return Result.success(questionnaireService.detail(id));
    }

    @Operation(summary = "提交答卷")
    @PostMapping("/{id}/responses")
    @Idempotent(prefix = "questionnaire:submit", ttl = 3)
    public Result<Void> submitResponse(@PathVariable Long id, @Valid @RequestBody ResponseSubmitDTO dto) {
        questionnaireService.submitResponse(id, dto);
        return Result.success();
    }

    @Operation(summary = "报名人员明细列表（可按状态筛选）")
    @GetMapping("/{id}/responses")
    public Result<List<QuestionnaireResponseVO>> listResponses(@PathVariable Long id,
            @RequestParam(required = false) Integer status) {
        return Result.success(questionnaireService.listResponses(id, status));
    }

    @Operation(summary = "报名统计看板（总人数/状态分组/按日时间线）")
    @GetMapping("/{id}/statistics")
    public Result<ResponseStatisticsVO> statistics(@PathVariable Long id) {
        return Result.success(questionnaireService.statistics(id));
    }

    @Operation(summary = "导出报名人员 Excel")
    @GetMapping("/{id}/responses/export")
    public ResponseEntity<byte[]> exportResponses(@PathVariable Long id) {
        byte[] data = questionnaireService.exportResponses(id);
        String filename = URLEncoder.encode("报名人员名单.xlsx", StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
