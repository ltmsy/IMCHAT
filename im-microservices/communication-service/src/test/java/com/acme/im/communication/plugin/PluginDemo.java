package com.acme.im.communication.plugin;

import com.acme.im.communication.plugin.impl.DefaultMessageProcessor;
import com.acme.im.communication.plugin.impl.DefaultMessageRouter;

/**
 * 插件演示程序
 * 展示改造后的消息处理插件系统功能
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class PluginDemo {

    public static void main(String[] args) {
        System.out.println("=== IM通信层插件系统演示 ===\n");

        // 创建插件实例
        DefaultMessageProcessor messageProcessor = new DefaultMessageProcessor();
        DefaultMessageRouter messageRouter = new DefaultMessageRouter();

        // 演示消息处理器
        System.out.println("1. 消息处理器演示:");
        MessageProcessor.ProcessingContext context = new MessageProcessor.ProcessingContext("session123", "CHAT");
        context.setAttribute("userId", "user456");
        
        Object processedMessage = messageProcessor.processMessage("Hello World", context);
        System.out.println("   处理前: Hello World");
        System.out.println("   处理后: " + processedMessage);
        System.out.println("   处理时间: " + context.getAttribute("processingTime"));
        System.out.println("   是否已处理: " + context.getAttribute("processed"));
        System.out.println();

        // 演示消息路由器
        System.out.println("2. 消息路由器演示:");
        MessageRouter.RoutingContext routingContext = new MessageRouter.RoutingContext("session123", "FILE", "user789", "conv001");
        routingContext.setAttribute("priority", "high");
        
        MessageRouter.RoutingResult routingResult = messageRouter.routeMessage("document.pdf", routingContext);
        System.out.println("   路由成功: " + routingResult.isSuccess());
        System.out.println("   目标服务: " + routingResult.getTargetService());
        System.out.println("   目标实例: " + routingResult.getTargetInstance());
        System.out.println("   消息类型: " + routingResult.getMetadata().get("messageType"));
        System.out.println("   优先级: " + routingResult.getMetadata().get("priority"));
        System.out.println("   文件处理: " + routingResult.getMetadata().get("fileProcessing"));
        System.out.println();

        // 演示插件信息
        System.out.println("3. 插件信息:");
        System.out.println("   消息处理器名称: " + messageProcessor.getProcessorName());
        System.out.println("   消息处理器优先级: " + messageProcessor.getPriority());
        System.out.println("   消息路由器名称: " + messageRouter.getRouterName());
        System.out.println("   消息路由器优先级: " + messageRouter.getPriority());
        System.out.println();

        // 演示支持的消息类型
        System.out.println("4. 支持的消息类型:");
        System.out.println("   消息处理器支持: " + messageProcessor.getSupportedMessageTypes());
        System.out.println("   消息路由器支持所有类型: " + messageRouter.supportsMessageType("CHAT"));
        System.out.println();

        System.out.println("=== 演示完成 ===");
    }
} 