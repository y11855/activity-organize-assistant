package com.aiapp.activityassistant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 请求 DTO：活动创建/编辑
 */
@Data
@Schema(description = "活动创建请求")
public class ActivitySaveDTO {

    @Schema(description = "活动标题")
    @NotBlank(message = "活动标题不能为空")
    private String title;

    @Schema(description = "活动描述（一句话描述，用于 AI 生成策划）")
    private String description;

    @Schema(description = "面向参与者的活动介绍（可手写，留空则可由 AI 生成）")
    private String introduction;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "活动地点")
    private String location;

    @Schema(description = "活动属性标签: sports运动 art艺术 academic学术 public_welfare公益 social社交 entertainment娱乐 other其他")
    @NotBlank(message = "请选择活动属性标签")
    private String tag;

    @Schema(description = "预计参与人数")
    private Integer expectedCount;

    @Schema(description = "参与成员 ID 列表（AI 分工在这些成员间进行）")
    private List<Long> memberIds;
}
