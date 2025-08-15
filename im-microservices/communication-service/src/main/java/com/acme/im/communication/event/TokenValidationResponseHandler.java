package com.acme.im.communication.event;

import com.acme.im.common.infrastructure.nats.annotation.NatsEventHandler;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.dto.UserEvents;
import com.acme.im.communication.service.TokenValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Token验证响应处理器
 * 处理来自业务服务的Token验证响应
 * 
 * @author acme
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenValidationResponseHandler {

    private final TokenValidationService tokenValidationService;

    /**
     * 处理Token验证响应
     */
    @NatsEventHandler(value = EventTopics.Security.TOKEN_VALIDATE, priority = 100)
    public void handleTokenValidationResponse(BaseEvent<UserEvents.TokenValidationResponse> event) {
        UserEvents.TokenValidationResponse response = event.getData();
        
        log.debug("收到Token验证响应: requestId={}, valid={}", 
                response.getRequestId(), response.isValid());
        
        try {
            // 将响应传递给TokenValidationService
            tokenValidationService.handleTokenValidationResponse(response);
            
        } catch (Exception e) {
            log.error("处理Token验证响应失败: requestId={}, error={}", 
                    response.getRequestId(), e.getMessage(), e);
        }
    }
} 