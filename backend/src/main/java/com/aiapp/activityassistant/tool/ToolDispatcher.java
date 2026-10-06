package com.aiapp.activityassistant.tool;

import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.common.utils.IdempotentKeyGenerator;
import com.aiapp.activityassistant.entity.ToolCallLog;
import com.aiapp.activityassistant.mapper.ToolCallLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具调度器：统一管理工具调用，提供幂等保障与失败重试
 *
 * 幂等策略（两层）：
 * 1. Redis：成功结果缓存直接返回；SET NX 原子抢占执行权，防止并发重复执行
 * 2. DB：tool_call_log 记录留底，Redis 丢失后仍可按幂等键 + 成功状态兜底
 *
 * 重试策略：失败后按配置间隔重试，达到最大次数后抛出异常。
 */
@Slf4j
@Component
public class ToolDispatcher {

    /** 工具成功结果缓存 key */
    private static final String RESULT_KEY_PREFIX = "idem:tool:result:";
    /** 工具执行抢占锁 key */
    private static final String LOCK_KEY_PREFIX = "idem:tool:lock:";
    /** 成功结果保留 24 小时 */
    private static final Duration RESULT_TTL = Duration.ofHours(24);
    /** 执行锁最长持有 5 分钟（兜底进程崩溃未释放） */
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    private final ToolCallLogMapper toolCallLogMapper;
    private final IdempotentKeyGenerator idempotentKeyGenerator;
    private final RedisService redisService;
    private final Map<String, Tool> toolRegistry = new ConcurrentHashMap<>();

    @Value("${activity-assistant.tool.retry-count:3}")
    private int retryCount;

    @Value("${activity-assistant.tool.retry-interval:1000}")
    private long retryInterval;

    public ToolDispatcher(ToolCallLogMapper toolCallLogMapper,
                            IdempotentKeyGenerator idempotentKeyGenerator,
                            RedisService redisService,
                            List<Tool> tools) {
        this.toolCallLogMapper = toolCallLogMapper;
        this.idempotentKeyGenerator = idempotentKeyGenerator;
        this.redisService = redisService;
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

        // 1. Redis 幂等检查：已成功则直接返回缓存结果
        Object cachedResult = redisService.get(RESULT_KEY_PREFIX + idempotentKey);
        if (cachedResult != null) {
            log.info("[工具幂等] {} Redis 命中成功记录，直接返回", toolName);
            return ToolResult.ok(cachedResult);
        }

        // 2. DB 兜底幂等检查（Redis 被清空等场景）
        ToolCallLog existing = toolCallLogMapper.selectOne(
                new LambdaQueryWrapper<ToolCallLog>()
                        .eq(ToolCallLog::getIdempotentKey, idempotentKey)
                        .eq(ToolCallLog::getStatus, 1)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            log.info("[工具幂等] {} DB 命中成功记录，直接返回并回填 Redis", toolName);
            redisService.set(RESULT_KEY_PREFIX + idempotentKey, existing.getResult(), RESULT_TTL);
            return ToolResult.ok(existing.getResult());
        }

        Tool tool = toolRegistry.get(toolName);
        if (tool == null) {
            throw new BusinessException(ResultCode.TOOL_CALL_ERROR, "未知工具: " + toolName);
        }

        // 3. 原子抢占执行权，防止同一时刻并发重复执行
        boolean locked = redisService.setIfAbsent(
                LOCK_KEY_PREFIX + idempotentKey, toolName, LOCK_TTL);
        if (!locked) {
            log.warn("[工具幂等] {} 已有相同调用执行中，本次跳过", toolName);
            return ToolResult.ok("任务正在处理中，请勿重复触发");
        }

        try {
            return doDispatch(activityId, toolName, params, idempotentKey, tool);
        } finally {
            // 主动释放执行锁（崩溃场景由锁 TTL 兜底）
            redisService.delete(LOCK_KEY_PREFIX + idempotentKey);
        }
    }

    /**
     * 落日志 + 重试执行（调用方已持有执行锁）
     */
    private ToolResult doDispatch(Long activityId, String toolName, Map<String, Object> params,
                                String idempotentKey, Tool tool) {
        // 创建调用日志
        ToolCallLog callLog = new ToolCallLog();
        callLog.setActivityId(activityId);
        callLog.setToolName(toolName);
        callLog.setIdempotentKey(idempotentKey);
        callLog.setParams(params.toString());
        callLog.setStatus(0);
        callLog.setRetryCount(0);
        toolCallLogMapper.insert(callLog);

        // 带重试执行
        Exception lastException = null;
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                ToolResult result = tool.execute(params);
                if (result.isSuccess()) {
                    String resultData = String.valueOf(result.getData());
                    callLog.setStatus(1);
                    callLog.setResult(resultData);
                    callLog.setRetryCount(attempt - 1);
                    toolCallLogMapper.updateById(callLog);
                    // 成功结果写入 Redis，后续重复调用直接返回
                    redisService.set(RESULT_KEY_PREFIX + idempotentKey, resultData, RESULT_TTL);
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

        // 全部失败，记录日志并抛出
        callLog.setStatus(2);
        callLog.setRetryCount(retryCount);
        callLog.setErrorMessage(lastException != null ? lastException.getMessage() : "未知错误");
        toolCallLogMapper.updateById(callLog);

        throw new BusinessException(ResultCode.TOOL_RETRY_EXHAUSTED,
                toolName + " 调用失败，已重试 " + retryCount + " 次");
    }
}
