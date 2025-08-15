package com.acme.im.communication.service;

import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.dto.UserEvents;
import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Token验证服务
 * 通过NATS事件调用业务服务验证Token
 * 
 * @author acme
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {

    private final AsyncEventPublisher eventPublisher;
    
    // 存储待验证的Token请求，key为requestId
    private final ConcurrentHashMap<String, CompletableFuture<UserEvents.TokenValidationResponse>> pendingValidations = new ConcurrentHashMap<>();
    
    // 验证超时时间（秒）
    private static final int VALIDATION_TIMEOUT = 10;

    /**
     * 验证Token
     * 
     * @param token JWT Token
     * @return 用户信息，验证失败返回null
     */
    public UserEvents.TokenValidationResponse validateToken(String token) {
        try {
            String requestId = generateRequestId();
            CompletableFuture<UserEvents.TokenValidationResponse> future = new CompletableFuture<>();
            
            // 存储待验证的请求
            pendingValidations.put(requestId, future);
            
            // 发布Token验证请求事件
            UserEvents.TokenValidationRequest request = UserEvents.TokenValidationRequest.builder()
                    .requestId(requestId)
                    .token(token)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            BaseEvent<UserEvents.TokenValidationRequest> event = BaseEvent.createRequest(
                    EventTopics.Security.TOKEN_VALIDATE, request)
                    .fromService("communication-service", "default")
                    .addMetadata("requestId", requestId);
            
            eventPublisher.publishEvent(EventTopics.Security.TOKEN_VALIDATE, event);
            
            log.debug("发送Token验证请求: requestId={}", requestId);
            
            // 等待验证结果，设置超时
            try {
                return future.get(VALIDATION_TIMEOUT, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Token验证超时: requestId={}", requestId);
                pendingValidations.remove(requestId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("Token验证失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 处理Token验证响应
     * 
     * @param response 验证响应
     */
    public void handleTokenValidationResponse(UserEvents.TokenValidationResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<UserEvents.TokenValidationResponse> future = pendingValidations.remove(requestId);
        
        if (future != null) {
            future.complete(response);
            log.debug("收到Token验证响应: requestId={}, valid={}", requestId, response.isValid());
        } else {
            log.warn("未找到对应的Token验证请求: requestId={}", requestId);
        }
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return "token_val_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    /**
     * 清理过期的验证请求
     */
    public void cleanupExpiredValidations() {
        // 这里可以实现定期清理逻辑
        // 比如清理超过一定时间的pendingValidations
    }
} 