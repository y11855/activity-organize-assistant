package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 响应 VO：活动详情的展示数据结构
 */
@Data
@Schema(description = "活动详情")
public class ActivityVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "创建人 ID")
    private Long creatorId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "面向参与者的活动介绍")
    private String introduction;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "地点")
    private String location;

    @Schema(description = "活动属性标签编码")
    private String tag;

    @Schema(description = "活动属性标签中文名")
    private String tagName;

    @Schema(description = "预计参与人数")
    private Integer expectedCount;

    @Schema(description = "参与成员昵称列表")
    private List<String> memberNames;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "草稿状态: 0无 1待确认 2已确认发布")
    private Integer draftStatus;

    @Schema(description = "任务分工是否已通知成员: 0否 1是")
    private Integer tasksNotified;

    @Schema(description = "AI 生成的策划内容")
    private String planContent;

    @Schema(description = "AI 生成的复盘内容")
    private String reviewContent;

    @Schema(description = "物料清单")
    private List<MaterialVO> materials;

    @Schema(description = "任务列表")
    private List<TaskVO> tasks;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
