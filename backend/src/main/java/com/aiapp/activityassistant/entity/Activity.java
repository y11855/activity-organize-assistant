package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动实体
 */
@Data
@TableName("activity")
public class Activity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 创建人 ID（权限控制用，只能操作自己的活动） */
    private Long creatorId;

    private String title;

    private String description;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动地点 */
    private String location;

    /** 状态: 0草稿 1策划中 2执行中 3已完成 4已取消 */
    private Integer status;

    /** 活动策划内容（AI 生成） */
    private String planContent;

    /** 复盘总结内容（AI 生成） */
    private String reviewContent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
