package com.aiapp.activityassistant.tool;

import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.common.utils.IdempotentKeyGenerator;
import com.aiapp.activityassistant.entity.ToolCallLog;
import com.aiapp.activityassistant.mapper.ToolCallLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具调度器：统一管理工具调用，提供幂等保障与失败重试
 *
 * 幂等策略：根据 toolName + params 生成幂等键，
 * 若存在成功记录则直接返回缓存结果，避免重复发消息等副作用。
 *
 * 重试策略：失败后按配置间隔重试，达到最大次数后抛出异常。
 */
@Slf4j
@Component
public class ToolDispatcher {

    private final ToolCallLogMapper toolCallLogMapper;
    private final IdempotentKeyGenerator idempotentKeyGenerator;
    private final Map<String, Tool> toolRegistry = new ConcurrentHashMap<>();

    @Value("${activity-assistant.tool.retry-count:3}")
    private int retryCount;

    @Value("${activity-assistant.tool.retry-interval:1000}")
    private long retryInterval;

    public ToolDispatcher(ToolCallLogMapper toolCallLogMapper,
                          IdempotentKeyGenerator idempotentKeyGenerator,
                          List<Tool> tools) {
        this.toolCallLogMapper = toolCallLogMapper;
        this.idempotentKeyGenerator = idempotentKeyGenerator;
        tools.forEach(t -> toolRegistry.put(t.name(), t));
    }

    /**
     * 调用工具（带幂等 + 重试）
     *
     * @param activityId 活动 ID
     * @param toolName   工具名称
     * @param params     参数
     * @return 工具执行结果
     */
    public ToolResult dispatch(Long activityId, String toolName, Map<String, Object> params) {
        String idempotentKey = idempotentKeyGenerator.generate(toolName, params.toString());

        // 1. 幂等检查：已成功则直接返回
        ToolCallLog existing = toolCallLogMapper.selectOne(
                new LambdaQueryWrapper<ToolCallLog>()
                        .eq(ToolCallLog::getIdempotentKey, idempotentKey)
                        .eq(ToolCallLog::getStatus, 1)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            log.info("[工具幂等] {} 已成功调用，直接返回缓存结果", toolName);
            return ToolResult.ok(existing.getResult());
        }

        Tool tool = toolRegistry.get(toolName);
        if (tool == null) {
            throw new BusinessException(ResultCode.TOOL_CALL_ERROR, "未知工具: " + toolName);
        }

        // 2. 创建调用日志
        ToolCallLog callLog = new ToolCallLog();
        callLog.setActivityId(activityId);
        callLog.setToolName(toolName);
        callLog.setIdempotentKey(idempotentKey);
        callLog.setParams(params.toString());
        callLog.setStatus(0);
        callLog.setRetryCount(0);
        callLog.setCreateTime(LocalDateTime.now());
        toolCallLogMapper.insert(callLog);

        // 3. 带重试执行
        Exception lastException = null;
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                ToolResult result = tool.execute(params);
                if (result.isSuccess()) {
                    callLog.setStatus(1);
                    callLog.setResult(String.valueOf(result.getData()));
                    callLog.setRetryCount(attempt - 1);
                    toolCallLogMapper.updateById(callLog);
                    return result;
                }
                lastException = new RuntimeException(result.getErrorMessage());
                log.warn("[工具调用] {} 第 {} 次返回失败: {}", toolName, attempt, result.getErrorMessage());
            } catch (Exception e) {
                lastException = e;
                log.warn("[工具调用] {} 第 {} 次异常: {}", toolName, attempt, e.getMessage());
            }

            // 重试等待
            if (attempt < retryCount) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 4. 全部失败，记录日志并抛出
        callLog.setStatus(2);
        callLog.setRetryCount(retryCount);
        callLog.setErrorMessage(lastException != null ? lastException.getMessage() : "未知错误");
        toolCallLogMapper.updateById(callLog);

        throw new BusinessException(ResultCode.TOOL_RETRY_EXHAUSTED,
                toolName + " 调用失败，已重试 " + retryCount + " 次");
    }
}
