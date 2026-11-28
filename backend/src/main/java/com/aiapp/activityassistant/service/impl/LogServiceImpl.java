package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.dto.LogQuery;
import com.aiapp.activityassistant.entity.SysLog;
import com.aiapp.activityassistant.mapper.SysLogMapper;
import com.aiapp.activityassistant.service.LogService;
import com.aiapp.activityassistant.vo.LogOverviewVO;
import com.aiapp.activityassistant.vo.SysLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 业务实现：系统日志的写入、分页查询、概览统计与清空。
 */
@Slf4j
@Service
public class LogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements LogService {

    @Override
    @Async
    public void saveAsync(SysLog sysLog) {
        try {
            save(sysLog);
        } catch (Exception e) {
            // 旁路逻辑：写日志失败只告警，绝不影响业务主流程
            log.warn("写入系统日志失败: {}", e.getMessage());
        }
    }

    @Override
    public PageVO<SysLogVO> page(LogQuery query) {
        // 条件按需拼接：logType/module 走等值，keyword 覆盖用户名、路径与操作名
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .eq(StringUtils.hasText(query.getLogType()), SysLog::getLogType, query.getLogType())
                .eq(StringUtils.hasText(query.getModule()), SysLog::getModule, query.getModule())
                .ge(query.getStartTime() != null, SysLog::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, SysLog::getCreateTime, query.getEndTime())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(SysLog::getUserName, query.getKeyword())
                        .or().like(SysLog::getRequestUri, query.getKeyword())
                        .or().like(SysLog::getOperation, query.getKeyword()))
                .orderByDesc(SysLog::getCreateTime)
                .orderByDesc(SysLog::getId);

        Page<SysLog> result = page(new Page<>(query.getPage(), query.getSize()), wrapper);
        List<SysLogVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageVO.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public LogOverviewVO overview() {
        LogOverviewVO overview = baseMapper.selectOverview();
        return overview == null ? new LogOverviewVO() : overview;
    }

    @Override
    public List<String> modules() {
        return baseMapper.selectModules();
    }

    @Override
    public void clear() {
        baseMapper.purgeAll();
    }

    /** 实体转 VO：耗时降为 Integer，避免全局 Long→String 序列化影响前端排序展示 */
    private SysLogVO toVO(SysLog log) {
        SysLogVO vo = new SysLogVO();
        vo.setId(log.getId());
        vo.setLogType(log.getLogType());
        vo.setModule(log.getModule());
        vo.setOperation(log.getOperation());
        vo.setMethod(log.getMethod());
        vo.setRequestUri(log.getRequestUri());
        vo.setRequestParams(log.getRequestParams());
        vo.setResponseBody(log.getResponseBody());
        vo.setUserId(log.getUserId());
        vo.setUserName(log.getUserName());
        vo.setIp(log.getIp());
        vo.setClient(log.getClient());
        vo.setStatusCode(log.getStatusCode());
        vo.setDuration(log.getDuration() == null ? null : log.getDuration().intValue());
        vo.setSuccess(log.getSuccess());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }
}
