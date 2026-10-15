package com.aiapp.activityassistant.common.context;

import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;

/**
 * 当前用户上下文（权限控制用）
 * Web 请求由 {@code AuthInterceptor} 填充；异步线程（如 Agent 编排）需显式 set/clear。
 */
public class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /**
     * 获取当前登录用户 ID；无上下文（未登录）时抛 401
     */
    public static Long getCurrentUserId() {
        Long id = CURRENT_USER_ID.get();
        if (id == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return id;
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
