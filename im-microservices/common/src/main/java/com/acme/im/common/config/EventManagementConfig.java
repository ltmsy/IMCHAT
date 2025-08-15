package com.acme.im.common.config;

import com.acme.im.common.infrastructure.nats.config.EventTopicManager;
import com.acme.im.common.infrastructure.nats.subscriber.EventSubscriber;
import io.nats.client.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * 事件管理配置
 * 负责管理公共模块需要订阅的通用事件
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class EventManagementConfig implements ApplicationRunner {

    @Autowired
    @Lazy
    private EventSubscriber eventSubscriber;

    // 存储订阅对象，便于管理
    private List<Subscription> subscriptions;

    /**
     * 应用启动完成后，启动公共模块的事件订阅
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("公共模块事件管理配置初始化开始...");
        
        // 获取公共模块需要订阅的事件主题
        List<String> commonTopics = EventTopicManager.getServiceTopics(
                EventTopicManager.ServiceNames.COMMON);
        
        // 订阅公共模块需要的通用事件主题
        subscriptions = eventSubscriber.subscribeToTopics(commonTopics);
        
        // 过滤掉订阅失败的主题
        long successCount = subscriptions.stream()
                .filter(subscription -> subscription != null)
                .count();
        
        log.info("公共模块事件管理配置启动成功，成功订阅 {}/{} 个通用主题", 
                successCount, commonTopics.size());
        
        if (log.isDebugEnabled()) {
            log.debug("已订阅的通用主题: {}", commonTopics);
        }
    }

    /**
     * 获取当前活跃的订阅信息
     */
    public List<Subscription> getActiveSubscriptions() {
        return subscriptions;
    }

    /**
     * 获取订阅统计信息
     */
    public String getSubscriptionStats() {
        if (subscriptions == null) {
            return "未初始化";
        }
        
        long activeCount = subscriptions.stream()
                .filter(subscription -> subscription != null)
                .count();
        
        return String.format("总订阅数: %d, 活跃订阅数: %d", 
                subscriptions.size(), activeCount);
    }
} 