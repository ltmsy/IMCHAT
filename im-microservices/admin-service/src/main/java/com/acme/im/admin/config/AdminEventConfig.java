package com.acme.im.admin.config;

import com.acme.im.common.infrastructure.nats.config.EventTopicManager;
import com.acme.im.common.infrastructure.nats.subscriber.EventSubscriber;
import io.nats.client.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * 管理服务事件配置
 * 负责启动事件订阅和初始化事件处理
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
public class AdminEventConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminEventConfig.class);

    @Autowired
    @Lazy
    private EventSubscriber eventSubscriber;

    // 存储订阅对象，便于管理
    private List<Subscription> subscriptions;

    /**
     * 应用启动完成后，启动事件订阅
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("管理服务事件配置初始化开始...");
        
        // 获取管理服务需要订阅的事件主题
        List<String> adminTopics = EventTopicManager.getServiceTopics(
                EventTopicManager.ServiceNames.ADMIN);
        
        // 订阅管理服务相关的事件主题
        subscriptions = eventSubscriber.subscribeToTopics(adminTopics);
        
        // 过滤掉订阅失败的主题
        long successCount = subscriptions.stream()
                .filter(subscription -> subscription != null)
                .count();
        
        log.info("管理服务事件配置启动成功，成功订阅 {}/{} 个主题", 
                successCount, adminTopics.size());
        
        if (log.isDebugEnabled()) {
            log.debug("已订阅的主题: {}", adminTopics);
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