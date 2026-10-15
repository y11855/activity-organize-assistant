package com.aiapp.activityassistant.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 活动分解树节点：把活动及其任务/物料/问卷/提醒组织成树形结构
 */
@Data
public class ActivityBreakdownNodeVO {

    /** 节点唯一 ID（字符串，便于拼接） */
    private String id;

    /** 节点显示名称 */
    private String name;

    /** 节点类型：activity / category / task / material / questionnaire / reminder */
    private String type;

    /** 节点状态（活动/任务/问卷/提醒的状态码，无则为 null） */
    private Integer status;

    /** 详情面板展示用的扩展字段 */
    private Map<String, Object> meta;

    /** 子节点 */
    private List<ActivityBreakdownNodeVO> children;
}
