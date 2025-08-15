package com.acme.im.communication.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 * 使用传统Spring WebSocket，支持STOMP协议
 * 
 * @author acme
 * @since 1.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     * 
     * @param config 消息代理配置
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的内存消息代理，用于向客户端广播消息
        // 客户端订阅以"/topic"开头的目标
        config.enableSimpleBroker("/topic", "/queue");
        
        // 设置应用程序目标的前缀
        // 客户端发送消息到以"/app"开头的目标
        config.setApplicationDestinationPrefixes("/app");
        
        // 设置用户目标的前缀
        // 客户端可以发送消息到以"/user"开头的目标，用于点对点通信
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 注册STOMP端点
     * 
     * @param registry STOMP端点注册表
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端通过这个端点建立WebSocket连接
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 允许所有来源，生产环境应该限制
                .withSockJS(); // 启用SockJS支持，提供降级方案
        
        // 注册原生WebSocket端点
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }
} 