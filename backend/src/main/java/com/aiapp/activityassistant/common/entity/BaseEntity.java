package com.aiapp.activityassistant.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：统一主键、审计字段、逻辑删除
 * <ul>
 *   <li>id：雪花算法生成的 64 位 Long，全局唯一、趋势递增</li>
 *   <li>createTime / updateTime：由 MyMetaObjectHandler 自动填充</li>
 *   <li>deleted：逻辑删除标记，MyBatis-Plus 查询自动追加 deleted = 0</li>
 * </ul>
 */
@Data
public abstract class BaseEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
