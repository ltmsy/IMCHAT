package com.acme.im.communication.plugin;

import com.acme.im.communication.plugin.impl.DefaultMessageProcessor;
import com.acme.im.communication.plugin.impl.DefaultMessageRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 插件集成测试
 * 测试改造后的消息处理插件系统
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class PluginIntegrationTest {

    private DefaultMessageProcessor messageProcessor;
    private DefaultMessageRouter messageRouter;

    @BeforeEach
    public void setUp() {
        messageProcessor = new DefaultMessageProcessor();
        messageRouter = new DefaultMessageRouter();
    }

    @Test
    public void testMessageProcessorBasicFunctionality() {
        // 测试消息处理器的基本功能
        MessageProcessor.ProcessingContext context = new MessageProcessor.ProcessingContext("session123", "CHAT");
        context.setAttribute("userId", "user456");
        
        Object result = messageProcessor.processMessage("Hello World", context);
        
        assertNotNull(result);
        assertEquals("Hello World", result);
        assertTrue(context.getAttributes().containsKey("processed"));
        assertTrue(context.getAttributes().containsKey("processingTime"));
    }

    @Test
    public void testMessageRouterBasicFunctionality() {
        // 测试消息路由器的基本功能
        MessageRouter.RoutingContext context = new MessageRouter.RoutingContext("session123", "CHAT", "user789", "conv001");
        context.setAttribute("priority", "high");
        
        MessageRouter.RoutingResult result = messageRouter.routeMessage("Hello World", context);
        
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("message-service", result.getTargetService());
        assertEquals("default", result.getTargetInstance());
        assertNotNull(result.getMetadata());
    }

    @Test
    public void testMessageProcessorSupportsAllMessageTypes() {
        // 测试消息处理器支持所有消息类型
        assertTrue(messageProcessor.supportsMessageType("CHAT"));
        assertTrue(messageProcessor.supportsMessageType("FILE"));
        assertTrue(messageProcessor.supportsMessageType("IMAGE"));
        assertTrue(messageProcessor.supportsMessageType("AUDIO"));
        assertTrue(messageProcessor.supportsMessageType("VIDEO"));
        assertTrue(messageProcessor.supportsMessageType("SYSTEM"));
    }

    @Test
    public void testMessageRouterSupportsAllMessageTypes() {
        // 测试消息路由器支持所有消息类型
        assertTrue(messageRouter.supportsMessageType("CHAT"));
        assertTrue(messageRouter.supportsMessageType("FILE"));
        assertTrue(messageRouter.supportsMessageType("IMAGE"));
        assertTrue(messageRouter.supportsMessageType("AUDIO"));
        assertTrue(messageRouter.supportsMessageType("VIDEO"));
        assertTrue(messageRouter.supportsMessageType("SYSTEM"));
    }

    @Test
    public void testMessageProcessorPriority() {
        // 测试消息处理器优先级
        assertEquals(100, messageProcessor.getPriority());
    }

    @Test
    public void testMessageRouterPriority() {
        // 测试消息路由器优先级
        assertEquals(100, messageRouter.getPriority());
    }

    @Test
    public void testMessageProcessorName() {
        // 测试消息处理器名称
        assertEquals("DefaultMessageProcessor", messageProcessor.getProcessorName());
    }

    @Test
    public void testMessageRouterName() {
        // 测试消息路由器名称
        assertEquals("DefaultMessageRouter", messageRouter.getRouterName());
    }

    @Test
    public void testMessageProcessorSupportedTypes() {
        // 测试消息处理器支持的消息类型列表
        var supportedTypes = messageProcessor.getSupportedMessageTypes();
        assertNotNull(supportedTypes);
        assertEquals(6, supportedTypes.size());
        assertTrue(supportedTypes.contains("CHAT"));
        assertTrue(supportedTypes.contains("FILE"));
        assertTrue(supportedTypes.contains("IMAGE"));
        assertTrue(supportedTypes.contains("AUDIO"));
        assertTrue(supportedTypes.contains("VIDEO"));
        assertTrue(supportedTypes.contains("SYSTEM"));
    }

    @Test
    public void testMessageRouterChatMessageRouting() {
        // 测试聊天消息路由
        MessageRouter.RoutingContext context = new MessageRouter.RoutingContext("session123", "CHAT", "user789", "conv001");
        MessageRouter.RoutingResult result = messageRouter.routeMessage("Hello", context);
        
        assertTrue(result.isSuccess());
        assertEquals("message-service", result.getTargetService());
        assertEquals("default", result.getTargetInstance());
        assertEquals("CHAT", result.getMetadata().get("messageType"));
        assertEquals("normal", result.getMetadata().get("priority"));
        assertEquals(true, result.getMetadata().get("requiresAck"));
    }

    @Test
    public void testMessageRouterFileMessageRouting() {
        // 测试文件消息路由
        MessageRouter.RoutingContext context = new MessageRouter.RoutingContext("session123", "FILE", "user789", "conv001");
        MessageRouter.RoutingResult result = messageRouter.routeMessage("document.pdf", context);
        
        assertTrue(result.isSuccess());
        assertEquals("file-service", result.getTargetService());
        assertEquals("default", result.getTargetInstance());
        assertEquals("FILE", result.getMetadata().get("messageType"));
        assertEquals("high", result.getMetadata().get("priority"));
        assertEquals(true, result.getMetadata().get("fileProcessing"));
    }

    @Test
    public void testMessageProcessorContextAttributes() {
        // 测试消息处理器上下文属性设置
        MessageProcessor.ProcessingContext context = new MessageProcessor.ProcessingContext("session123", "IMAGE");
        context.setAttribute("customAttr", "customValue");
        
        messageProcessor.processMessage("image.jpg", context);
        
        assertTrue(context.getAttributes().containsKey("processed"));
        assertTrue(context.getAttributes().containsKey("processingTime"));
        assertTrue(context.getAttributes().containsKey("imageType"));
        assertEquals("customValue", context.getAttribute("customAttr"));
        assertEquals("jpeg", context.getAttribute("imageType"));
    }

    @Test
    public void testMessageRouterContextAttributes() {
        // 测试消息路由器上下文属性设置
        MessageRouter.RoutingContext context = new MessageRouter.RoutingContext("session123", "VIDEO", "user789", "conv001");
        context.setAttribute("customAttr", "customValue");
        
        MessageRouter.RoutingResult result = messageRouter.routeMessage("video.mp4", context);
        
        assertTrue(result.isSuccess());
        assertEquals("video-service", result.getTargetService());
        assertEquals("customValue", context.getAttribute("customAttr"));
        assertEquals("mp4", result.getMetadata().get("videoType"));
    }
} 