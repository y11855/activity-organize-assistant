package com.aiapp.activityassistant.agent;

import com.aiapp.activityassistant.dto.TaskAssignDTO;
import lombok.Data;

import java.util.List;

/**
 * Agent 规划阶段产出
 */
@Data
public class PlannerResult {

    /** 策划文案 */
    private String planContent;

    /** 物料清单（JSON 描述） */
    private String materials;

    /** 问卷题目（JSON 数组） */
    private String questions;

    /** 任务分工列表 */
    private List<TaskAssignDTO> tasks;
}
