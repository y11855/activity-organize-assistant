package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 响应 VO：报名统计看板的展示数据结构
 */
@Data
@Schema(description = "报名统计看板")
public class ResponseStatisticsVO {

    @Schema(description = "报名总人数")
    private Long total;

    @Schema(description = "按状态分组的人数: status(1已报名 2已取消) -> 人数")
    private Map<Integer, Long> byStatus;

    @Schema(description = "按日期分组的报名时间线: yyyy-MM-dd -> 人数")
    private List<Map<String, Object>> timeline;
}
