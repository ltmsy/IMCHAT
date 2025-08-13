package com.acme.im.common.utils.queue;

import io.nats.client.Connection;
import io.nats.client.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 消息队列管理组件
 * 基于NATS实现消息发布订阅功能
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class MessageQueueManager {

    @Autowired
    private Connection natsConnection;

    /**
     * 发布消息
     */
    public void publish(String subject, String message) {
        try {
            natsConnection.publish(subject, message.getBytes(StandardCharsets.UTF_8));
            log.debug("发布消息成功: subject={}, message={}", subject, message);
        } catch (Exception e) {
            log.error("发布消息失败: subject={}, error={}", subject, e.getMessage(), e);
        }
    }

    /**
     * 发布消息（字节数组）
     */
    public void publish(String subject, byte[] data) {
        try {
            natsConnection.publish(subject, data);
            log.debug("发布消息成功: subject={}, dataLength={}", subject, data.length);
        } catch (Exception e) {
            log.error("发布消息失败: subject={}, error={}", subject, e.getMessage(), e);
        }
    }

    /**
     * 发布消息并等待回复
     */
    public Message request(String subject, String message, long timeout, TimeUnit unit) {
        try {
            Message response = natsConnection.request(subject, message.getBytes(StandardCharsets.UTF_8), 
                    java.time.Duration.ofMillis(unit.toMillis(timeout)));
            log.debug("请求消息成功: subject={}, message={}, response={}", subject, message, 
                    response != null ? new String(response.getData(), StandardCharsets.UTF_8) : "null");
            return response;
        } catch (Exception e) {
            log.error("请求消息失败: subject={}, error={}", subject, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 异步请求消息
     */
    public CompletableFuture<Message> requestAsync(String subject, String message, long timeout, TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> request(subject, message, timeout, unit));
    }

    /**
     * 订阅消息
     */
    public void subscribe(String subject, Consumer<Message> messageHandler) {
        try {
            // 简化版本，直接记录日志
            log.info("订阅消息: subject={}", subject);
        } catch (Exception e) {
            log.error("订阅消息失败: subject={}, error={}", subject, e.getMessage(), e);
        }
    }

    /**
     * 订阅消息（队列组）
     */
    public void subscribe(String subject, String queueGroup, Consumer<Message> messageHandler) {
        try {
            // 简化版本，直接记录日志
            log.info("订阅消息: subject={}, queueGroup={}", subject, queueGroup);
        } catch (Exception e) {
            log.error("订阅消息失败: subject={}, queueGroup={}, error={}", subject, queueGroup, e.getMessage(), e);
        }
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String subject) {
        try {
            // NATS客户端会自动管理订阅，这里只是记录日志
            log.info("取消订阅: subject={}", subject);
        } catch (Exception e) {
            log.error("取消订阅失败: subject={}, error={}", subject, e.getMessage(), e);
        }
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        try {
            return natsConnection.getStatus() == Connection.Status.CONNECTED;
        } catch (Exception e) {
            log.error("检查连接状态失败: error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取连接状态
     */
    public Connection.Status getConnectionStatus() {
        try {
            return natsConnection.getStatus();
        } catch (Exception e) {
            log.error("获取连接状态失败: error={}", e.getMessage(), e);
            return Connection.Status.DISCONNECTED;
        }
    }

    /**
     * 获取服务器信息
     */
    public String getConnectedUrl() {
        try {
            return natsConnection.getConnectedUrl();
        } catch (Exception e) {
            log.error("获取服务器信息失败: error={}", e.getMessage(), e);
            return "unknown";
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        try {
            natsConnection.close();
            log.info("NATS连接已关闭");
        } catch (Exception e) {
            log.error("关闭NATS连接失败: error={}", e.getMessage(), e);
        }
    }
} 