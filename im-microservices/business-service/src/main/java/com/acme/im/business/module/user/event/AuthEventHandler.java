package com.acme.im.business.module.user.event;

import com.acme.im.business.module.user.entity.User;
import com.acme.im.business.module.user.service.UserService;
import com.acme.im.common.infrastructure.nats.annotation.NatsEventHandler;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import com.acme.im.common.infrastructure.nats.constants.EventErrorCodes;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import com.acme.im.common.security.jwt.JwtTokenProvider;
import com.acme.im.common.security.encryption.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 认证事件处理器
 * 使用注解驱动的方式处理认证相关事件
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventHandler {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AsyncEventPublisher eventPublisher;
    private final EncryptionUtils encryptionUtils;

    /**
     * 处理认证验证事件
     */
    @NatsEventHandler(
        value = EventTopics.Common.Auth.VALIDATE,
        priority = 10,
        description = "处理用户认证验证请求"
    )
    public void handleAuthValidation(BaseEvent<?> event) {
        try {
            log.info("处理认证事件: eventId={}, userId={}", event.getEventId(), event.getUserId());

            // 获取认证请求数据
            Object data = event.getData();
            
            if (data == null) {
                log.warn("认证事件数据为空: eventId={}", event.getEventId());
                publishAuthFailureResponse(event, "认证数据为空", EventErrorCodes.AUTH_DATA_NULL);
                return;
            }

            // 解析认证请求
            Map<String, Object> authRequest = (Map<String, Object>) data;
            String username = (String) authRequest.get("username");
            String password = (String) authRequest.get("password");
            String deviceId = (String) authRequest.get("deviceId");

            if (username == null || password == null) {
                log.warn("认证参数不完整: eventId={}, username={}", event.getEventId(), username);
                publishAuthFailureResponse(event, "用户名或密码为空", EventErrorCodes.AUTH_PARAMS_INCOMPLETE);
                return;
            }

            // 执行用户认证 - 查找用户并验证密码
            Optional<User> userOpt = userService.findUserByUsername(username);
            if (userOpt.isEmpty()) {
                log.warn("用户不存在: eventId={}, username={}", event.getEventId(), username);
                publishAuthFailureResponse(event, "用户名或密码错误", EventErrorCodes.USER_NOT_FOUND);
                return;
            }

            User user = userOpt.get();
            
            // 检查用户状态
            if (user.getStatus() != 1) {
                log.warn("用户状态异常: eventId={}, username={}, status={}", event.getEventId(), username, user.getStatus());
                publishAuthFailureResponse(event, "用户状态异常，无法登录", EventErrorCodes.USER_STATUS_ERROR);
                return;
            }

            // 验证密码
            String inputPasswordWithSalt = password + user.getSalt();
            String inputPasswordHash = encryptionUtils.hash(inputPasswordWithSalt, "SHA-256");
            if (!user.getPasswordHash().equals(inputPasswordHash)) {
                log.warn("密码验证失败: eventId={}, username={}", event.getEventId(), username);
                publishAuthFailureResponse(event, "用户名或密码错误", EventErrorCodes.AUTH_FAILED);
                return;
            }

            // 生成JWT令牌
            String token = jwtTokenProvider.generateAccessToken(user.getUsername(), deviceId);
            
            // 构建认证成功响应
            AuthResult authResult = AuthResult.builder()
                    .success(true)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .token(token)
                    .deviceId(deviceId)
                    .message("认证成功")
                    .build();

            // 发布认证结果事件
            BaseEvent<AuthResult> responseEvent = BaseEvent.createResponse(EventTopics.Common.Auth.RESULT, authResult)
                    .fromService("business-service", "default")
                    .withUser(user.getId().toString(), deviceId, event.getSessionId())
                    .success();

            eventPublisher.publishEvent(EventTopics.Common.Auth.RESULT, responseEvent);
            
            log.info("认证事件处理成功: eventId={}, userId={}", event.getEventId(), user.getId());

        } catch (Exception e) {
            log.error("处理认证事件异常: eventId={}", event.getEventId(), e);
            publishAuthFailureResponse(event, "认证处理异常: " + e.getMessage(), EventErrorCodes.AUTH_PROCESSING_ERROR);
        }
    }

    /**
     * 发布认证失败响应
     */
    private void publishAuthFailureResponse(BaseEvent<?> originalEvent, String errorMessage, String errorCode) {
        try {
            AuthResult authResult = AuthResult.builder()
                    .success(false)
                    .message(errorMessage)
                    .errorCode(errorCode)
                    .build();

            BaseEvent<AuthResult> responseEvent = BaseEvent.createResponse(EventTopics.Common.Auth.RESULT, authResult)
                    .fromService("business-service", "default")
                    .withUser(originalEvent.getUserId(), originalEvent.getDeviceId(), originalEvent.getSessionId())
                    .failure(errorMessage, errorCode);

            eventPublisher.publishEvent(EventTopics.Common.Auth.RESULT, responseEvent);
            
        } catch (Exception e) {
            log.error("发布认证失败响应异常: eventId={}", originalEvent.getEventId(), e);
        }
    }

    /**
     * 认证结果数据类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AuthResult {
        private boolean success;
        private Long userId;
        private String username;
        private String token;
        private String deviceId;
        private String message;
        private String errorCode;
    }
} 