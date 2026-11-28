package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 响应 VO：日志中心概览统计。
 *
 * <p>数值统一用 Integer（Long 会被全局序列化成字符串，前端统计卡需要数字）。</p>
 */
@Data
@Schema(description = "日志概览统计")
public class LogOverviewVO {

    @Schema(description = "接口调用总量")
    private Integer totalCalls = 0;

    @Schema(description = "今日接口调用量")
    private Integer todayCalls = 0;

    @Schema(description = "异常日志数")
    private Integer exceptionCount = 0;

    @Schema(description = "慢请求数（耗时 ≥ 1000ms）")
    private Integer slowCount = 0;

    @Schema(description = "登录失败次数")
    private Integer loginFailCount = 0;

    @Schema(description = "当前在线用户（近 30 分钟去重用户数）")
    private Integer onlineUsers = 0;
}
