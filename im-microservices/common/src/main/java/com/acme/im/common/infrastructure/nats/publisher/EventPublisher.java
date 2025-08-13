package com.acme.im.common.infrastructure.nats.publisher;

import com.acme.im.common.infrastructure.nats.config.NatsConfig;
import com.google.gson.Gson;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * NATS事件发布器
 * 负责发布事件到NATS服务器
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final NatsConfig natsConfig;
    private final Gson gson;

    /**
     * 发布事件到指定主题
     * 
     * @param subject 主题
     * @param event 事件对象
     */
    public void publishEvent(String subject, Object event) {
        try {
            Connection connection = natsConfig.getConnection();
            if (connection != null && connection.getStatus() == Connection.Status.CONNECTED) {
                String jsonEvent = gson.toJson(event);
                connection.publish(subject, jsonEvent.getBytes(StandardCharsets.UTF_8));
                log.debug("事件发布成功: subject={}, event={}", subject, event.getClass().getSimpleName());
            } else {
                log.warn("NATS连接不可用，无法发布事件: subject={}", subject);
            }
        } catch (Exception e) {
            log.error("发布事件失败: subject={}, event={}", subject, event.getClass().getSimpleName(), e);
        }
    }

    /**
     * 异步发布事件
     * 
     * @param subject 主题
     * @param event 事件对象
     * @return CompletableFuture
     */
    public CompletableFuture<Void> publishEventAsync(String subject, Object event) {
        return CompletableFuture.runAsync(() -> publishEvent(subject, event));
    }

    /**
     * 发布事件到JetStream（持久化）
     * 
     * @param subject 主题
     * @param event 事件对象
     */
    public void publishToJetStream(String subject, Object event) {
        try {
            JetStream jetStream = natsConfig.getJetStream();
            if (jetStream != null) {
                String jsonEvent = gson.toJson(event);
                jetStream.publish(subject, jsonEvent.getBytes(StandardCharsets.UTF_8));
                log.debug("JetStream事件发布成功: subject={}, event={}", subject, event.getClass().getSimpleName());
            } else {
                log.warn("JetStream不可用，无法发布事件: subject={}", subject);
            }
        } catch (Exception e) {
            log.error("JetStream事件发布失败: subject={}, event={}", subject, event.getClass().getSimpleName(), e);
        }
    }

    /**
     * 创建或更新流
     * 
     * @param streamName 流名称
     * @param subjects 主题列表
     */
    public void createOrUpdateStream(String streamName, String... subjects) {
        try {
            JetStreamManagement jsm = natsConfig.getJetStreamManagement();
            if (jsm != null) {
                StreamConfiguration config = StreamConfiguration.builder()
                    .name(streamName)
                    .subjects(subjects)
                    .build();
                
                try {
                    StreamInfo streamInfo = jsm.getStreamInfo(streamName);
                    if (streamInfo != null) {
                        log.info("流已存在: {}", streamName);
                    }
                } catch (Exception e) {
                    // 流不存在，创建新流
                    jsm.addStream(config);
                    log.info("流创建成功: {}", streamName);
                }
            }
        } catch (JetStreamApiException | IOException e) {
            log.error("创建或更新流失败: streamName={}", streamName, e);
        }
    }
} 