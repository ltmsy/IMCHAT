package com.acme.im.business.module.user.event;

import com.acme.im.common.infrastructure.nats.annotation.NatsEventHandler;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.dto.UserEvents;
import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import com.acme.im.common.security.jwt.JwtTokenProvider;
import com.acme.im.business.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Token验证事件处理器
 * 处理来自通信服务的Token验证请求
 * 
 * @author acme
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenValidationEventHandler {

    private final AsyncEventPublisher eventPublisher;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    /**
     * 处理Token验证请求
     */
    @NatsEventHandler(value = EventTopics.Security.TOKEN_VALIDATE, priority = 100)
    public void handleTokenValidationRequest(BaseEvent<UserEvents.TokenValidationRequest> event) {
        UserEvents.TokenValidationRequest request = event.getData();
        String requestId = (String) event.getMetadata().get("requestId");
        
        log.info("收到Token验证请求: requestId={}", requestId);
        
        try {
            // 验证Token
            String token = request.getToken();
            if (!jwtTokenProvider.validateToken(token)) {
                sendValidationResponse(requestId, false, null, null, null, "Token无效或已过期");
                return;
            }
            
            // 从Token中获取用户信息
            String username = jwtTokenProvider.getUsernameFromToken(token);
            String deviceId = jwtTokenProvider.getDeviceIdFromToken(token);
            
            // 查找用户
            Optional<com.acme.im.business.module.user.entity.User> userOpt = userService.findUserByUsername(username);
            if (userOpt.isEmpty()) {
                sendValidationResponse(requestId, false, null, null, null, "用户不存在");
                return;
            }
            
            com.acme.im.business.module.user.entity.User user = userOpt.get();
            
            // 检查用户状态
            if (user.getStatus() != 1) { // 1表示正常状态
                sendValidationResponse(requestId, false, null, null, null, "用户账户已被禁用");
                return;
            }
            
            // 发送验证成功响应
            sendValidationResponse(requestId, true, user.getId(), username, deviceId, null);
            
            log.info("Token验证成功: requestId={}, userId={}, username={}", requestId, user.getId(), username);
            
        } catch (Exception e) {
            log.error("Token验证处理失败: requestId={}, error={}", requestId, e.getMessage(), e);
            sendValidationResponse(requestId, false, null, null, null, "Token验证失败: " + e.getMessage());
        }
    }

    /**
     * 发送Token验证响应
     */
    private void sendValidationResponse(String requestId, boolean valid, Long userId, String username, 
                                     String deviceId, String errorMessage) {
        try {
            UserEvents.TokenValidationResponse response = UserEvents.TokenValidationResponse.builder()
                    .requestId(requestId)
                    .valid(valid)
                    .userId(userId)
                    .username(username)
                    .deviceId(deviceId)
                    .errorMessage(errorMessage)
                    .timestamp(System.currentTimeMillis())
                    .build();

            BaseEvent<UserEvents.TokenValidationResponse> responseEvent = BaseEvent.createResponse(
                    EventTopics.Security.TOKEN_VALIDATE, response)
                    .fromService("business-service", "default")
                    .addMetadata("requestId", requestId);

            eventPublisher.publishEvent(EventTopics.Security.TOKEN_VALIDATE, responseEvent);
            
            log.debug("发送Token验证响应: requestId={}, valid={}", requestId, valid);
            
        } catch (Exception e) {
            log.error("发送Token验证响应失败: requestId={}, error={}", requestId, e.getMessage(), e);
        }
    }
} 