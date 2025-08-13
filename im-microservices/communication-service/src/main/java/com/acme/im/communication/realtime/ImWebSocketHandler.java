package com.acme.im.communication.realtime;

import com.acme.im.communication.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * IM WebSocket处理器
 * 处理WebSocket连接建立、消息接收和发送
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImWebSocketHandler implements WebSocketHandler {

    private final WebSocketConnectionManager connectionManager;
    private final WebSocketMessageHandler messageHandler;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("WebSocket连接建立: sessionId={}", session.getId());
        
        return session.receive()
                .doOnNext(message -> {
                    try {
                        messageHandler.handleIncomingMessage(session, message);
                    } catch (Exception e) {
                        log.error("处理WebSocket消息异常: sessionId={}", session.getId(), e);
                        messageHandler.handleError(session, e);
                    }
                })
                .doOnError(error -> {
                    log.error("WebSocket连接错误: sessionId={}", session.getId(), error);
                    messageHandler.handleError(session, error);
                })
                .doFinally(signal -> {
                    log.info("WebSocket连接关闭: sessionId={}, signal={}", session.getId(), signal);
                    messageHandler.handleConnectionClose(session);
                })
                .then();
    }
} 