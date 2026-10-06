package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物料清单项
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material")
public class Material extends BaseEntity {

    private Long activityId;

    /** 物料名称 */
    private String name;

    /** 数量 */
    private Integer quantity;

    /** 单位 */
    private String unit;

    /** 预估单价 */
    private BigDecimal unitPrice;

    /** 备注 */
    private String remark;
}
