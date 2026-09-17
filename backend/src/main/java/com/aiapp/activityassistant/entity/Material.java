package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物料清单项
 */
@Data
@TableName("material")
public class Material implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
