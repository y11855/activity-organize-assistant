package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 生成内容配置：发起人勾选需要 AI 生成的内容项
 */
@Data
@Schema(description = "AI 生成内容配置")
public class ActivityGenerateDTO {

    @Schema(description = "是否生成任务分工明细")
    private Boolean tasks;

    @Schema(description = "是否生成物料采购清单")
    private Boolean materials;

    @Schema(description = "是否生成报名问卷配置")
    private Boolean questionnaire;

    @Schema(description = "是否生成定时提醒规则")
    private Boolean reminders;

    public boolean wantTasks() {
        return Boolean.TRUE.equals(tasks);
    }

    public boolean wantMaterials() {
        return Boolean.TRUE.equals(materials);
    }

    public boolean wantQuestionnaire() {
        return Boolean.TRUE.equals(questionnaire);
    }

    public boolean wantReminders() {
        return Boolean.TRUE.equals(reminders);
    }

    /** 是否勾选了任意一项 */
    public boolean anySelected() {
        return wantTasks() || wantMaterials() || wantQuestionnaire() || wantReminders();
    }
}
