package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工具调用日志（幂等与重试追踪）
 */
@Data
@TableName("tool_call_log")
public class ToolCallLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long activityId;

    /** 工具名称: calendar / form / message */
    private String toolName;

    /** 幂等键 */
    private String idempotentKey;

    /** 调用参数（JSON） */
    private String params;

    /** 调用结果（JSON） */
    private String result;

    /** 状态: 0进行中 1成功 2失败 */
    private Integer status;

    /** 重试次数 */
    private Integer retryCount;

    /** 错误信息 */
    private String errorMessage;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
