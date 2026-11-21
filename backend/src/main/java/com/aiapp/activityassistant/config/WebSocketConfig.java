package com.aiapp.activityassistant.config;

import com.aiapp.activityassistant.ws.ChatHandshakeInterceptor;
import com.aiapp.activityassistant.ws.ChatSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 注册：/api/ws/chat（context-path 为 /api）
 * 同时订阅 Redis 聊天频道，让多实例部署时各节点都能收到广播。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatSocketHandler chatSocketHandler;
    private final ChatHandshakeInterceptor chatHandshakeInterceptor;
    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatSocketHandler, "/ws/chat")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }

    /** 订阅 activity:chat:channel:* 下的所有活动频道 */
    @Bean
    public RedisMessageListenerContainer chatMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(chatSocketHandler, new PatternTopic(ChatSocketHandler.CHANNEL_PREFIX + "*"));
        return container;
    }
}
