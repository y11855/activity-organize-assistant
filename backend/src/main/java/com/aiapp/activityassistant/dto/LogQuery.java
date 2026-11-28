package com.aiapp.activityassistant.dto;

import com.aiapp.activityassistant.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 日志分页查询参数（继承统一分页参数）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "日志分页查询参数")
public class LogQuery extends PageQuery {

    @Schema(description = "日志类型: operation/login/api/exception/audit，为空查全部")
    private String logType;

    @Schema(description = "业务模块，为空查全部")
    private String module;

    @Schema(description = "关键字：用户名 / 请求路径 / 操作名")
    private String keyword;

    @Schema(description = "开始时间，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
