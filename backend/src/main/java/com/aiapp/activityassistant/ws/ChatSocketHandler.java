package com.aiapp.activityassistant.ws;

import com.aiapp.activityassistant.vo.ChatMessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 聊天 WebSocket 处理器：按活动分组（房间）推送消息。
 *
 * <p>广播走 Redis 发布订阅，而不是直接遍历本机连接：
 * Nginx 后面可能有多个后端实例，消息发布到 Redis 频道后，
 * 每个实例只推送自己持有的连接，保证多实例下也能收到。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSocketHandler extends TextWebSocketHandler implements MessageListener {

    public static final String ATTR_ACTIVITY_ID = "activityId";
    public static final String ATTR_USER_ID = "userId";

    /** 每个活动一个频道：activity:chat:channel:{activityId} */
    public static final String CHANNEL_PREFIX = "activity:chat:channel:";

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 活动 ID → 该实例上的在线连接 */
    private final Map<Long, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long activityId = activityId(session);
        Set<WebSocketSession> sessions = rooms.computeIfAbsent(activityId, k -> new CopyOnWriteArraySet<>());
        sessions.add(session);
        log.info("[聊天] 连接建立 activityId={}, userId={}, 本机在线 {} 人",
                activityId, session.getAttributes().get(ATTR_USER_ID), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long activityId = activityId(session);
        Set<WebSocketSession> sessions = rooms.get(activityId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            rooms.remove(activityId);
        }
    }

    /** 发布消息到 Redis 频道，由各实例的 {@link #onMessage} 推送给自己的连接 */
    public void broadcast(Long activityId, ChatMessageVO message) {
        try {
            stringRedisTemplate.convertAndSend(CHANNEL_PREFIX + activityId,
                    objectMapper.writeValueAsString(message));
        } catch (IOException e) {
            log.warn("[聊天] 消息序列化失败 activityId={}", activityId);
        }
    }

    /** 收到本实例订阅的频道消息：推给本机该活动的所有在线连接 */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        Long activityId = parseActivityId(channel);
        if (activityId != null) {
            sendToRoom(activityId, new String(message.getBody(), StandardCharsets.UTF_8));
        }
    }

    private void sendToRoom(Long activityId, String json) {
        Set<WebSocketSession> sessions = rooms.get(activityId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage text = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            try {
                // 同一连接可能被多个线程同时写，串行化避免并发写异常
                synchronized (session) {
                    session.sendMessage(text);
                }
            } catch (IOException e) {
                log.warn("[聊天] 推送失败 activityId={}, sessionId={}", activityId, session.getId());
            }
        }
    }

    private Long parseActivityId(String channel) {
        try {
            return Long.valueOf(channel.substring(CHANNEL_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long activityId(WebSocketSession session) {
        return (Long) session.getAttributes().get(ATTR_ACTIVITY_ID);
    }
}
