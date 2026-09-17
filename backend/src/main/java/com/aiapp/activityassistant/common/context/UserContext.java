package com.aiapp.activityassistant.common.context;

/**
 * 当前用户上下文（权限控制用）
 * 实际项目中由鉴权拦截器填充，此处提供静态占位便于开发
 */
public class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getCurrentUserId() {
        Long id = CURRENT_USER_ID.get();
        // 未登录时返回 1 作为演示用户，实际应由拦截器抛 401
        return id != null ? id : 1L;
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
