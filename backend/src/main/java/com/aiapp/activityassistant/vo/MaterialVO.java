package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "物料项")
public class MaterialVO {

    private Long id;

    @Schema(description = "物料名称")
    private String name;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "预估单价")
    private BigDecimal unitPrice;

    @Schema(description = "备注")
    private String remark;
}
