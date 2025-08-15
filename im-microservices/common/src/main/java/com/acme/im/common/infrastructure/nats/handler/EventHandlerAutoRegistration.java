package com.acme.im.common.infrastructure.nats.handler;

import com.acme.im.common.infrastructure.nats.annotation.NatsEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件处理器自动注册组件
 * 自动扫描@NatsEventHandler注解并注册到EventSubscriber
 * 
 * @author acme
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventHandlerAutoRegistration {

    private final ApplicationContext applicationContext;
    
    // 存储已注册的事件处理器
    private final Map<String, Object> registeredHandlers = new ConcurrentHashMap<>();

    /**
     * 应用启动后自动注册事件处理器
     */
    @PostConstruct
    public void registerEventHandlers() {
        log.info("开始自动注册NATS事件处理器...");
        
        try {
            // 扫描所有带有@NatsEventHandler注解的方法
            Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);
            
            for (Object bean : beans.values()) {
                Class<?> beanClass = bean.getClass();
                
                // 跳过Spring框架的代理类
                if (beanClass.getName().contains("$$")) {
                    continue;
                }
                
                Method[] methods = beanClass.getDeclaredMethods();
                for (Method method : methods) {
                    NatsEventHandler annotation = method.getAnnotation(NatsEventHandler.class);
                    if (annotation != null) {
                        registerEventHandler(bean, method, annotation);
                    }
                }
            }
            
            log.info("NATS事件处理器自动注册完成，共注册 {} 个处理器", registeredHandlers.size());
            
        } catch (Exception e) {
            log.error("自动注册NATS事件处理器失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 注册单个事件处理器
     */
    private void registerEventHandler(Object bean, Method method, NatsEventHandler annotation) {
        try {
            String subject = annotation.value();
            int priority = annotation.priority();
            boolean async = annotation.async();
            
            // 创建事件处理器
            EventHandler handler = new EventHandler(bean, method, subject, priority, async);
            
            // 注册到本地存储
            String handlerId = bean.getClass().getSimpleName() + "." + method.getName();
            registeredHandlers.put(handlerId, handler);
            
            log.info("注册事件处理器: subject={}, handler={}, priority={}, async={}", 
                    subject, handlerId, priority, async);
            
        } catch (Exception e) {
            log.error("注册事件处理器失败: bean={}, method={}, error={}", 
                    bean.getClass().getSimpleName(), method.getName(), e.getMessage(), e);
        }
    }

    /**
     * 获取已注册的事件处理器数量
     */
    public int getRegisteredHandlerCount() {
        return registeredHandlers.size();
    }

    /**
     * 获取已注册的事件处理器信息
     */
    public Map<String, Object> getRegisteredHandlers() {
        return new ConcurrentHashMap<>(registeredHandlers);
    }

    /**
     * 手动注册事件处理器（运行时动态注册）
     */
    public void registerEventHandler(String subject, Object handler, int priority, boolean async) {
        try {
            String handlerId = handler.getClass().getSimpleName() + "_" + System.currentTimeMillis();
            registeredHandlers.put(handlerId, handler);
            
            log.info("手动注册事件处理器: subject={}, handler={}, priority={}, async={}", 
                    subject, handlerId, priority, async);
            
        } catch (Exception e) {
            log.error("手动注册事件处理器失败: subject={}, error={}", subject, e.getMessage(), e);
        }
    }

    /**
     * 注销事件处理器
     */
    public void unregisterEventHandler(String handlerId) {
        Object handler = registeredHandlers.remove(handlerId);
        if (handler != null) {
            log.info("注销事件处理器: handlerId={}", handlerId);
        } else {
            log.warn("未找到要注销的事件处理器: handlerId={}", handlerId);
        }
    }
} 