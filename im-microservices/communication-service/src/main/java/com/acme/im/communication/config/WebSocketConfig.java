package com.acme.im.communication.config;

import com.acme.im.communication.realtime.ImWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket配置类
 * 配置WebSocket路由和处理器
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final ImWebSocketHandler webSocketHandler;

    /**
     * WebSocket路由映射
     */
    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, org.springframework.web.reactive.socket.WebSocketHandler> urlMap = new HashMap<>();
        urlMap.put("/ws", webSocketHandler);
        
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(urlMap);
        mapping.setOrder(1);
        
        return mapping;
    }

    /**
     * WebSocket处理器适配器
     */
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
} 