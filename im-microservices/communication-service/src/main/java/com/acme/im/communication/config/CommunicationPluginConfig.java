package com.acme.im.communication.config;

import com.acme.im.common.plugin.ExtensionPointManager;
import com.acme.im.common.plugin.ExtensionPointRegistry;
import com.acme.im.communication.plugin.MessageRouter;
import com.acme.im.communication.plugin.MessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 通信层插件配置类
 * 启用插件系统并注册通信层特有的扩展点
 * 专注于消息处理，不涉及业务认证
 * 
 * 扩展点说明：
 * 1. message.route - 消息路由扩展点，支持自定义消息路由逻辑
 * 2. message.process - 消息处理扩展点，支持自定义消息处理逻辑
 * 3. websocket.message.before.process - WebSocket消息前置处理扩展点
 * 4. websocket.message.after.process - WebSocket消息后置处理扩展点
 * 5. websocket.message.on.error - WebSocket消息异常处理扩展点
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class CommunicationPluginConfig {

    @Autowired
    private ExtensionPointManager extensionPointManager;

    /**
     * 注册通信层特有的扩展点
     */
    @PostConstruct
    public void registerCommunicationExtensionPoints() {
        log.info("开始注册通信层扩展点...");

        // 1. 消息路由扩展点 - 支持自定义消息路由逻辑
        extensionPointManager.registerExtensionPoint(
            "message.route",
            "消息路由扩展点，支持自定义消息路由逻辑",
            ExtensionPointRegistry.ExecutionStrategy.FIRST_SUCCESS,
            false,
            100,
            MessageRouter.class,
            "route"
        );

        // 2. 消息处理扩展点 - 支持自定义消息处理逻辑
        extensionPointManager.registerExtensionPoint(
            "message.process",
            "消息处理扩展点，支持自定义消息处理逻辑",
            ExtensionPointRegistry.ExecutionStrategy.ALL,
            false,
            150,
            MessageProcessor.class,
            "process"
        );

        // 3. WebSocket消息前置处理扩展点
        extensionPointManager.registerExtensionPoint(
            "websocket.message.before.process",
            "WebSocket消息前置处理扩展点，在消息处理前执行",
            ExtensionPointRegistry.ExecutionStrategy.ALL,
            false,
            90,
            MessageProcessor.class,
            "beforeProcess"
        );

        // 4. WebSocket消息后置处理扩展点
        extensionPointManager.registerExtensionPoint(
            "websocket.message.after.process",
            "WebSocket消息后置处理扩展点，在消息处理后执行",
            ExtensionPointRegistry.ExecutionStrategy.ALL,
            false,
            70,
            MessageProcessor.class,
            "afterProcess"
        );

        // 5. WebSocket消息异常处理扩展点
        extensionPointManager.registerExtensionPoint(
            "websocket.message.on.error",
            "WebSocket消息异常处理扩展点，在消息处理异常时执行",
            ExtensionPointRegistry.ExecutionStrategy.ALL,
            false,
            60,
            MessageProcessor.class,
            "onError"
        );

        log.info("通信层扩展点注册完成，共注册 {} 个扩展点", 5);
    }
} 