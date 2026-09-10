package com.aiapp.trainingeval.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实训详情返回对象
 */
@Data
@Schema(description = "实训详情")
public class TrainingVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "文件地址")
    private String fileUrl;

    @Schema(description = "AI 评分")
    private Double aiScore;

    @Schema(description = "教师评分")
    private Double teacherScore;

    @Schema(description = "综合评分")
    private Double finalScore;

    @Schema(description = "AI 评价意见")
    private String aiComment;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
