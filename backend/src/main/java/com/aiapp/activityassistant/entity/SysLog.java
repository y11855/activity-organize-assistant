package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MyBatis-Plus 实体：系统日志（对应表 sys_log）
 *
 * <p>所有日志统一落一张表，通过 {@link #logType} 区分操作、登录、接口、异常与审计，
 * 由日志拦截器异步写入，管理端按类型分 Tab 查询。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log")
public class SysLog extends BaseEntity {

    /** 日志类型：operation操作 login登录 api接口 exception异常 audit审计 */
    private String logType;

    /** 业务模块中文名（按请求路径首段映射） */
    private String module;

    /** 操作描述（接口方法名） */
    private String operation;

    /** HTTP 请求方法 */
    private String method;

    /** 请求路径 */
    private String requestUri;

    /** 请求参数（已脱敏并截断） */
    private String requestParams;

    /** 响应内容（截断） */
    private String responseBody;

    /** 操作人 ID，未登录为空 */
    private Long userId;

    /** 操作人用户名 */
    private String userName;

    /** 客户端 IP */
    private String ip;

    /** 客户端 User-Agent */
    private String client;

    /** HTTP 状态码 */
    private Integer statusCode;

    /** 耗时（毫秒） */
    private Long duration;

    /** 是否成功：1 是 0 否 */
    private Integer success;

    /** 异常信息 */
    private String errorMsg;
}
