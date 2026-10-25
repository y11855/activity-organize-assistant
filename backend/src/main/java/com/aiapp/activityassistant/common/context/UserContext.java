package com.aiapp.activityassistant.common.context;

import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 当前用户上下文（权限控制用）
 * Web 请求由 {@code AuthInterceptor} 填充；异步线程（如 Agent 编排）需显式 set/clear。
 */
public class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> ROLES = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> PERMISSIONS = new ThreadLocal<>();

    /**
     * 仅设置用户 ID（异步线程复用场景：只需身份，无需权限判断）
     */
    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /**
     * 设置完整上下文（Web 请求由拦截器调用）
     */
    public static void setCurrentUser(Long userId, List<String> roles, List<String> permissions) {
        CURRENT_USER_ID.set(userId);
        ROLES.set(roles == null ? Collections.emptyList() : roles);
        PERMISSIONS.set(permissions == null ? Collections.emptySet()
                : new java.util.HashSet<>(permissions));
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

    /** 当前用户角色编码；无上下文时返回空列表 */
    public static List<String> getRoles() {
        List<String> roles = ROLES.get();
        return roles == null ? Collections.emptyList() : roles;
    }

    /** 当前用户权限编码；无上下文时返回空集合 */
    public static Set<String> getPermissions() {
        Set<String> permissions = PERMISSIONS.get();
        return permissions == null ? Collections.emptySet() : permissions;
    }

    /** 是否拥有指定权限 */
    public static boolean hasPermission(String code) {
        return getPermissions().contains(code);
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        ROLES.remove();
        PERMISSIONS.remove();
    }
}
