package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "提醒")
public class ReminderVO {

    private Long id;

    @Schema(description = "活动 ID")
    private Long activityId;

    @Schema(description = "提醒标题")
    private String title;

    @Schema(description = "提醒内容")
    private String content;

    @Schema(description = "目标用户")
    private String targetUserName;

    @Schema(description = "触发时间")
    private LocalDateTime triggerTime;

    @Schema(description = "状态: 0待触发 1已发送 2已取消 3发送失败")
    private Integer status;

    private LocalDateTime createTime;
}
