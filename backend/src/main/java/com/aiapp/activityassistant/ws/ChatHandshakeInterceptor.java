package com.aiapp.activityassistant.ws;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.RbacService;
import com.aiapp.activityassistant.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 握手鉴权：浏览器无法自定义 Header，token 通过查询参数传递。
 * 连接地址：ws://host/api/ws/chat?token=xxx&activityId=1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final TokenService tokenService;
    private final ActivityService activityService;
    private final RbacService rbacService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(request.getURI())
                .build().getQueryParams();

        Long userId = tokenService.resolveUserId(params.getFirst("token"));
        if (userId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long activityId = parseId(params.getFirst("activityId"));
        if (activityId == null) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        // 复用活动详情的数据权限：创建人、参与成员或有管理权限的人才能加入聊天
        UserContext.setCurrentUser(userId, null, rbacService.listPermissionCodes(userId));
        try {
            activityService.checkAccess(activityId);
        } catch (Exception e) {
            log.warn("[聊天] 无权加入 activityId={}, userId={}", activityId, userId);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        } finally {
            UserContext.clear();
        }

        attributes.put(ChatSocketHandler.ATTR_ACTIVITY_ID, activityId);
        attributes.put(ChatSocketHandler.ATTR_USER_ID, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
    }

    private Long parseId(String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
