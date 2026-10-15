package com.aiapp.activityassistant.agent;

import com.aiapp.activityassistant.dto.QuestionDraftDTO;
import com.aiapp.activityassistant.dto.ReminderDraftDTO;
import com.aiapp.activityassistant.dto.TaskAssignDTO;
import com.aiapp.activityassistant.vo.MaterialVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 规划阶段产出（AI 生成的待确认内容）
 */
@Data
public class PlannerResult {

    /** 策划文案（活动流程与任务完成计划） */
    private String planContent;

    /** 物料采购清单 */
    private List<MaterialVO> materials = new ArrayList<>();

    /** 问卷题目 */
    private List<QuestionDraftDTO> questions = new ArrayList<>();

    /** 任务分工列表 */
    private List<TaskAssignDTO> tasks = new ArrayList<>();

    /** 定时提醒规则 */
    private List<ReminderDraftDTO> reminders = new ArrayList<>();
}
