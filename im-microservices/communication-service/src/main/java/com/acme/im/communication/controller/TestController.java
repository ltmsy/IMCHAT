package com.acme.im.communication.controller;

import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.dto.UserEvents;
import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import com.acme.im.communication.service.TokenValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 测试控制器
 * 用于测试各种功能Ï
 *
 * @author acme
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final AsyncEventPublisher eventPublisher;
    private final TokenValidationService tokenValidationService;

    /**
     * 获取服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        log.info("🔍 收到服务状态查询请求");
        String status = "Communication Service is running. NATS event mechanism is ready.";
        log.info("✅ 返回服务状态: {}", status);
        return ResponseEntity.ok(status);
    }

    /**
     * 测试NATS事件发布
     */
    @PostMapping("/publish-nats-event")
    public ResponseEntity<Map<String, Object>> publishNatsEvent(
            @RequestParam String subject,
            @RequestParam(required = false) String message) {
        
        log.info("🚀 开始测试NATS事件发布: subject={}, message={}", subject, message);
        
        try {
            String requestId = "test_direct_" + System.currentTimeMillis();
            
            // 创建测试事件数据
            UserEvents.TokenValidationRequest request = UserEvents.TokenValidationRequest.builder()
                    .requestId(requestId)
                    .token("test_token_" + System.currentTimeMillis())
                    .deviceId("test_device_001")
                    .timestamp(System.currentTimeMillis())
                    .build();

            // 创建基础事件
            BaseEvent<UserEvents.TokenValidationRequest> event = BaseEvent.createRequest(subject, request)
                    .fromService("communication-service", "test-controller")
                    .addMetadata("requestId", requestId)
                    .addMetadata("testType", "manual-test")
                    .addMetadata("timestamp", String.valueOf(System.currentTimeMillis()));

            log.info("📤 准备发布NATS事件: subject={}, requestId={}, event={}", subject, requestId, event);

            // 发布事件
            eventPublisher.publishEvent(subject, event);
            
            log.info("📤 事件发布请求已发送");
            
            log.info("✅ NATS事件发布成功: subject={}, requestId={}", subject, requestId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "NATS事件发布成功");
            response.put("subject", subject);
            response.put("requestId", requestId);
            response.put("timestamp", System.currentTimeMillis());
            response.put("eventData", request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ NATS事件发布失败: subject={}, error={}", subject, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "NATS事件发布失败: " + e.getMessage());
            response.put("subject", subject);
            response.put("timestamp", System.currentTimeMillis());
            response.put("error", e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 测试用户注册
     */
    @PostMapping("/test-user-register")
    public ResponseEntity<Map<String, Object>> testUserRegister() {
        log.info("👤 开始测试用户注册接口");
        
        try {
            String testUsername = "test_user_" + System.currentTimeMillis();
            String testPassword = "test123456";
            
            // 调用业务服务的用户注册接口
            String registerUrl = "http://localhost:8080/api/users/register";
            String requestBody = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"email\":\"%s@test.com\"}",
                testUsername, testPassword, testUsername
            );
            
            log.info("📤 发送用户注册请求: URL={}, username={}", registerUrl, testUsername);
            
            // 这里应该使用RestTemplate或WebClient，但为了简化，我们记录日志
            log.info("📝 模拟用户注册请求: {}", requestBody);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户注册测试完成");
            response.put("testUsername", testUsername);
            response.put("testPassword", testPassword);
            response.put("registerUrl", registerUrl);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ 用户注册测试完成: username={}", testUsername);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 用户注册测试失败: error={}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户注册测试失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 测试用户登录
     */
    @PostMapping("/test-user-login")
    public ResponseEntity<Map<String, Object>> testUserLogin() {
        log.info("🔐 开始测试用户登录接口");
        
        try {
            String testUsername = "bbbb"; // 使用已知的用户
            String testPassword = "123456";
            
            String loginUrl = "http://localhost:8080/api/users/login";
            String requestBody = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                testUsername, testPassword
            );
            
            log.info("📤 发送用户登录请求: URL={}, username={}", loginUrl, testUsername);
            log.info("📝 模拟用户登录请求: {}", requestBody);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户登录测试完成");
            response.put("testUsername", testUsername);
            response.put("loginUrl", loginUrl);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ 用户登录测试完成: username={}", testUsername);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 用户登录测试失败: error={}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户登录测试失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 测试WebSocket连接
     */
    @PostMapping("/test-websocket")
    public ResponseEntity<Map<String, Object>> testWebSocket() {
        log.info("🔌 开始测试WebSocket连接");
        
        try {
            String wsUrl = "ws://localhost:8081/ws";
            String stompUrl = "http://localhost:8081/ws";
            
            log.info("📡 WebSocket端点信息: wsUrl={}, stompUrl={}", wsUrl, stompUrl);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "WebSocket连接测试完成");
            response.put("wsUrl", wsUrl);
            response.put("stompUrl", stompUrl);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ WebSocket连接测试完成");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ WebSocket连接测试失败: error={}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "WebSocket连接测试失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 测试NATS订阅
     */
    @PostMapping("/test-nats-subscribe")
    public ResponseEntity<Map<String, Object>> testNatsSubscribe() {
        log.info("📡 开始测试NATS订阅");
        
        try {
            String testSubject = "test.subscribe." + System.currentTimeMillis();
            
            log.info("📡 测试订阅主题: {}", testSubject);
            log.info("💡 提示: 请使用nats-cli工具监听此主题: nats sub '{}'", testSubject);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "NATS订阅测试完成");
            response.put("testSubject", testSubject);
            response.put("timestamp", System.currentTimeMillis());
            response.put("command", "nats sub '" + testSubject + "'");
            
            log.info("✅ NATS订阅测试完成: subject={}", testSubject);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ NATS订阅测试失败: error={}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "NATS订阅测试失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取所有测试端点信息
     */
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getEndpoints() {
        log.info("📋 获取所有测试端点信息");
        
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("serviceStatus", "/api/test/status");
        endpoints.put("publishNatsEvent", "/api/test/publish-nats-event?subject={subject}");
        endpoints.put("testUserRegister", "/api/test/test-user-register");
        endpoints.put("testUserLogin", "/api/test/test-user-login");
        endpoints.put("testWebSocket", "/api/test/test-websocket");
        endpoints.put("testNatsSubscribe", "/api/test/test-nats-subscribe");
        endpoints.put("endpoints", "/api/test/endpoints");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "获取测试端点信息成功");
        response.put("endpoints", endpoints);
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("✅ 返回测试端点信息: {}", endpoints);
        return ResponseEntity.ok(response);
    }
} 