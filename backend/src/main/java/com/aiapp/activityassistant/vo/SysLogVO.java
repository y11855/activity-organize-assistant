package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 响应 VO：日志列表行数据（含可展开的请求参数、响应与异常信息）。
 */
@Data
@Schema(description = "系统日志")
public class SysLogVO {

    @Schema(description = "日志 ID")
    private Long id;

    @Schema(description = "日志类型: operation/login/api/exception/audit")
    private String logType;

    @Schema(description = "业务模块")
    private String module;

    @Schema(description = "操作描述")
    private String operation;

    @Schema(description = "HTTP 请求方法")
    private String method;

    @Schema(description = "请求路径")
    private String requestUri;

    @Schema(description = "请求参数（已脱敏）")
    private String requestParams;

    @Schema(description = "响应内容")
    private String responseBody;

    @Schema(description = "操作人 ID")
    private Long userId;

    @Schema(description = "操作人用户名")
    private String userName;

    @Schema(description = "客户端 IP")
    private String ip;

    @Schema(description = "客户端 User-Agent")
    private String client;

    @Schema(description = "HTTP 状态码")
    private Integer statusCode;

    @Schema(description = "耗时（毫秒）")
    private Integer duration;

    @Schema(description = "是否成功: 1是 0否")
    private Integer success;

    @Schema(description = "异常信息")
    private String errorMsg;

    @Schema(description = "日志时间")
    private LocalDateTime createTime;
}
