package com.acme.im.common.infrastructure.nats.subscriber;

import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.handler.EventHandlerRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.nats.client.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * NATS事件订阅器
 * 负责订阅和处理NATS事件
 * 
 * 职责：
 * 1. 提供事件订阅能力
 * 2. 自动路由事件到EventHandlerRegistry
 * 3. 管理订阅生命周期
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class EventSubscriber {

    private final Connection natsConnection;
    private final Gson gson;
    private final Executor taskExecutor;
    
    @Autowired
    @Lazy
    private EventHandlerRegistry eventHandlerRegistry;

    public EventSubscriber(Connection natsConnection, @Qualifier("gson") Gson gson, 
                          @Qualifier("customTaskExecutor") Executor taskExecutor) {
        this.natsConnection = natsConnection;
        this.gson = gson;
        this.taskExecutor = taskExecutor;
    }
    
    // 订阅器缓存 - 使用Subscription作为key
    private final ConcurrentHashMap<Subscription, Dispatcher> dispatchers = new ConcurrentHashMap<>();

    /**
     * 订阅事件
     * 
     * @param subject 主题
     * @param handler 事件处理器
     * @return 订阅对象
     */
    public Subscription subscribe(String subject, Consumer<Message> handler) {
        try {
            Dispatcher dispatcher = natsConnection.createDispatcher();
            Subscription subscription = dispatcher.subscribe(subject, new MessageHandler() {
                @Override
                public void onMessage(Message msg) {
                    try {
                        handler.accept(msg);
                    } catch (Exception e) {
                        log.error("处理消息失败: subject={}", subject, e);
                    }
                }
            });
            
            dispatchers.put(subscription, dispatcher);
            log.info("事件订阅成功: subject={}, sid={}", subject, subscription.getSubject());
            return subscription;
        } catch (Exception e) {
            log.error("订阅事件失败: subject={}", subject, e);
            return null;
        }
    }

    /**
     * 订阅事件（带类型转换）
     * 
     * @param subject 主题
     * @param eventType 事件类型
     * @param handler 事件处理器
     * @return 订阅对象
     */
    public <T> Subscription subscribe(String subject, Class<T> eventType, Consumer<T> handler) {
        return subscribe(subject, msg -> {
            try {
                String json = new String(msg.getData(), StandardCharsets.UTF_8);
                T event = gson.fromJson(json, eventType);
                handler.accept(event);
            } catch (JsonSyntaxException e) {
                log.error("JSON解析失败: subject={}, json={}", subject, 
                    new String(msg.getData(), StandardCharsets.UTF_8), e);
            } catch (Exception e) {
                log.error("处理事件失败: subject={}, eventType={}", subject, eventType.getSimpleName(), e);
            }
        });
    }

    /**
     * 订阅事件并自动路由到EventHandlerRegistry
     * 
     * @param subject 主题
     * @return 订阅对象
     */
    public Subscription subscribeWithAutoRouting(String subject) {
        log.info("🔔 订阅主题: {}", subject);
        
        return subscribe(subject, msg -> {
            try {
                log.info("📨 收到NATS消息: subject={}, replyTo={}, dataSize={} bytes", 
                        subject, msg.getReplyTo(), msg.getData().length);
                
                // 确保EventHandlerRegistry已初始化
                if (eventHandlerRegistry != null && !eventHandlerRegistry.isInitialized()) {
                    log.info("🔄 初始化EventHandlerRegistry...");
                    eventHandlerRegistry.initialize();
                }
                
                String json = new String(msg.getData(), StandardCharsets.UTF_8);
                log.debug("📄 消息JSON内容: {}", json);
                
                // 尝试解析为BaseEvent
                BaseEvent<?> event = gson.fromJson(json, BaseEvent.class);
                if (event != null) {
                    log.info("✅ 成功解析BaseEvent: eventId={}, eventType={}, userId={}", 
                            event.getEventId(), event.getEventType(), event.getUserId());
                    
                    // 设置事件主题
                    event.setSubject(subject);
                    log.debug("📝 设置事件主题: {}", subject);
                    
                    // 路由到事件处理器
                    if (eventHandlerRegistry != null) {
                        log.info("🚀 开始路由事件到EventHandlerRegistry...");
                        eventHandlerRegistry.handleEvent(subject, event);
                        log.info("✅ 事件自动路由成功: subject={}, eventId={}", subject, event.getEventId());
                    } else {
                        log.error("❌ EventHandlerRegistry未初始化，无法处理事件: subject={}", subject);
                    }
                } else {
                    log.warn("⚠️ 无法解析为BaseEvent: subject={}, json={}", subject, json);
                }
                
            } catch (Exception e) {
                log.error("❌ 自动路由事件失败: subject={}, error: {}", subject, e.getMessage(), e);
            }
        });
    }

    /**
     * 批量订阅指定主题列表（自动路由）
     * 
     * @param subjects 主题列表
     * @return 订阅对象列表
     */
    public List<Subscription> subscribeToTopics(List<String> subjects) {
        return subjects.stream()
                .map(this::subscribeWithAutoRouting)
                .filter(subscription -> subscription != null)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 批量订阅指定主题数组（自动路由）
     * 
     * @param subjects 主题数组
     * @return 订阅对象列表
     */
    public List<Subscription> subscribeToTopics(String... subjects) {
        return subscribeToTopics(java.util.Arrays.asList(subjects));
    }

    /**
     * 取消订阅
     * 
     * @param subscription 订阅对象
     */
    public void unsubscribe(Subscription subscription) {
        if (subscription != null) {
            Dispatcher dispatcher = dispatchers.remove(subscription);
            if (dispatcher != null) {
                subscription.unsubscribe();
                log.info("取消订阅成功: subject={}", subscription.getSubject());
            }
        }
    }

    /**
     * 批量取消订阅
     * 
     * @param subscriptions 订阅对象列表
     */
    public void unsubscribe(List<Subscription> subscriptions) {
        if (subscriptions != null) {
            subscriptions.forEach(this::unsubscribe);
        }
    }

    /**
     * 取消所有订阅
     */
    public void unsubscribeAll() {
        dispatchers.forEach((subscription, dispatcher) -> {
            subscription.unsubscribe();
            log.debug("取消订阅: subject={}", subscription.getSubject());
        });
        dispatchers.clear();
        log.info("所有订阅已取消");
    }

    /**
     * 获取活跃订阅数量
     * 
     * @return 订阅数量
     */
    public int getActiveSubscriptionCount() {
        return dispatchers.size();
    }

    /**
     * 获取所有活跃订阅的主题
     * 
     * @return 主题列表
     */
    public List<String> getActiveTopics() {
        return dispatchers.keySet().stream()
                .map(Subscription::getSubject)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 检查是否已订阅指定主题
     * 
     * @param subject 主题
     * @return 是否已订阅
     */
    public boolean isSubscribed(String subject) {
        return dispatchers.keySet().stream()
                .anyMatch(subscription -> subject.equals(subscription.getSubject()));
    }
} 