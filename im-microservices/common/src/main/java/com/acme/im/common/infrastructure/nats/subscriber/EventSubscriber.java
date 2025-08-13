package com.acme.im.common.infrastructure.nats.subscriber;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.nats.client.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * NATS事件订阅器
 * 负责订阅和处理NATS事件
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

    public EventSubscriber(Connection natsConnection, @Qualifier("gson") Gson gson, @Qualifier("customTaskExecutor") Executor taskExecutor) {
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
} 