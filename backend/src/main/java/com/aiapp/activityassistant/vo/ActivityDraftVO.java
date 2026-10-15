package com.aiapp.activityassistant.vo;

import com.aiapp.activityassistant.dto.ActivityGenerateDTO;
import com.aiapp.activityassistant.dto.QuestionDraftDTO;
import com.aiapp.activityassistant.dto.ReminderDraftDTO;
import com.aiapp.activityassistant.dto.TaskAssignDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 生成内容草稿（待发起人确认/编辑，确认后正式发布）
 */
@Data
@Schema(description = "AI 生成内容草稿")
public class ActivityDraftVO {

    @Schema(description = "生成项配置")
    private ActivityGenerateDTO options;

    @Schema(description = "策划方案（活动流程与任务完成计划）")
    private String planContent;

    @Schema(description = "物料采购清单")
    private List<MaterialVO> materials;

    @Schema(description = "报名问卷题目")
    private List<QuestionDraftDTO> questions;

    @Schema(description = "任务分工明细")
    private List<TaskAssignDTO> tasks;

    @Schema(description = "定时提醒规则")
    private List<ReminderDraftDTO> reminders;
}
