package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.dto.LogQuery;
import com.aiapp.activityassistant.entity.SysLog;
import com.aiapp.activityassistant.vo.LogOverviewVO;
import com.aiapp.activityassistant.vo.SysLogVO;

import java.util.List;

/**
 * 业务接口：系统日志的采集、查询与维护。
 */
public interface LogService {

    /** 分页查询日志（按时间倒序） */
    PageVO<SysLogVO> page(LogQuery query);

    /** 概览统计：调用量、异常、慢请求、登录失败与在线用户 */
    LogOverviewVO overview();

    /** 已产生日志的业务模块列表（筛选项） */
    List<String> modules();

    /** 清空全部日志 */
    void clear();

    /**
     * 异步写入一条日志。
     *
     * <p>日志采集属于旁路逻辑，必须在独立线程执行并吞掉异常，
     * 不能因为写日志失败影响正常接口响应。</p>
     */
    void saveAsync(SysLog sysLog);
}
