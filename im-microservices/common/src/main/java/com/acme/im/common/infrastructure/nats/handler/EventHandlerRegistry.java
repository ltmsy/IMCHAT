package com.acme.im.common.infrastructure.nats.handler;

import com.acme.im.common.infrastructure.nats.annotation.NatsEventHandler;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

/**
 * 事件处理器注册器
 * 自动发现和注册带有@NatsEventHandler注解的方法
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class EventHandlerRegistry {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 事件处理器映射：主题 -> 处理器列表
     */
    private final Map<String, List<EventHandlerInfo>> eventHandlers = new ConcurrentHashMap<>();

    /**
     * 异步执行器
     */
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(10);

    /**
     * 是否已初始化
     */
    private volatile boolean initialized = false;

    /**
     * 手动初始化事件处理器注册器
     * 避免在@PostConstruct中自动发现导致的循环依赖
     */
    public void initialize() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    discoverEventHandlers();
                    initialized = true;
                    log.info("事件处理器注册完成，共发现 {} 个处理器", getTotalHandlerCount());
                }
            }
        }
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 发现所有事件处理器
     */
    private void discoverEventHandlers() {
        log.info("🚀 开始发现事件处理器...");
        
        // 获取所有Spring Bean
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        log.info("📋 发现 {} 个Spring Bean", beanNames.length);
        
        int handlerCount = 0;
        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                Class<?> beanClass = bean.getClass();
                
                // 跳过Spring框架的代理类
                if (beanClass.getName().contains("$$")) {
                    continue;
                }
                
                // 查找带有@NatsEventHandler注解的方法
                Method[] methods = beanClass.getDeclaredMethods();
                for (Method method : methods) {
                    NatsEventHandler annotation = method.getAnnotation(NatsEventHandler.class);
                    if (annotation != null) {
                        registerEventHandler(bean, method, annotation);
                        handlerCount++;
                    }
                }
            } catch (Exception e) {
                log.warn("处理Bean时出错: beanName={}, error={}", beanName, e.getMessage());
            }
        }
        
        log.info("✅ 事件处理器发现完成，共发现 {} 个处理器", handlerCount);
        log.info("📊 已注册的主题: {}", eventHandlers.keySet());
    }

    /**
     * 注册事件处理器
     */
    private void registerEventHandler(Object bean, Method method, NatsEventHandler annotation) {
        String subject = annotation.value();
        String eventType = annotation.eventType();
        int priority = annotation.priority();
        boolean async = annotation.async();
        int retryCount = annotation.retryCount();
        long timeout = annotation.timeout();
        boolean enabled = annotation.enabled();
        String description = annotation.description();

        // 创建处理器信息
        EventHandlerInfo handlerInfo = EventHandlerInfo.builder()
                .bean(bean)
                .method(method)
                .subject(subject)
                .eventType(eventType)
                .priority(priority)
                .async(async)
                .retryCount(retryCount)
                .timeout(timeout)
                .enabled(enabled)
                .description(description)
                .build();

        // 注册到映射中
        eventHandlers.computeIfAbsent(subject, k -> new ArrayList<>()).add(handlerInfo);
        
        // 按优先级排序
        eventHandlers.get(subject).sort(Comparator.comparingInt(EventHandlerInfo::getPriority));

        log.debug("注册事件处理器: subject={}, method={}, priority={}, async={}", 
                subject, method.getName(), priority, async);
    }

    /**
     * 处理事件
     */
    public void handleEvent(String subject, BaseEvent<?> event) {
        log.info("📨 收到事件: subject={}, eventId={}, eventType={}", 
                subject, event.getEventId(), event.getEventType());
        
        // 按优先级排序处理器
        List<EventHandlerInfo> handlers = findHandlersForSubject(subject);
        
        if (handlers.isEmpty()) {
            log.warn("未找到事件处理器: subject={}", subject);
            log.warn("已注册的主题: {}", eventHandlers.keySet());
            return;
        }
        
        log.info("✅ 找到 {} 个事件处理器: subject={}", handlers.size(), subject);
        
        // 按优先级排序：数字越小优先级越高
        handlers.sort(Comparator.comparingInt(EventHandlerInfo::getPriority));
        
        // 记录找到的处理器详情
        for (EventHandlerInfo handler : handlers) {
            log.info("  - 处理器: {}.{}, priority={}, async={}", 
                    handler.getBean().getClass().getSimpleName(),
                    handler.getMethod().getName(),
                    handler.getPriority(),
                    handler.isAsync());
        }
        
        // 执行处理器
        for (EventHandlerInfo handler : handlers) {
            try {
                if (handler.isAsync()) {
                    log.debug("⚡ 异步执行处理器: {}.{}", 
                            handler.getBean().getClass().getSimpleName(),
                            handler.getMethod().getName());
                    // 异步执行
                    CompletableFuture.runAsync(() -> executeHandler(handler, event));
                } else {
                    log.debug("⚡ 同步执行处理器: {}.{}", 
                            handler.getBean().getClass().getSimpleName(),
                            handler.getMethod().getName());
                    // 同步执行
                    executeHandler(handler, event);
                }
            } catch (Exception e) {
                log.error("事件处理器执行失败: handler={}.{}, error: {}", 
                        handler.getBean().getClass().getSimpleName(),
                        handler.getMethod().getName(),
                        e.getMessage(), e);
            }
        }
    }

    /**
     * 查找指定主题的处理器
     * 优先返回精确匹配的处理器，然后返回通配符处理器
     */
    private List<EventHandlerInfo> findHandlersForSubject(String subject) {
        List<EventHandlerInfo> exactMatches = new ArrayList<>();
        List<EventHandlerInfo> wildcardMatches = new ArrayList<>();
        
        for (Map.Entry<String, List<EventHandlerInfo>> entry : eventHandlers.entrySet()) {
            String handlerSubject = entry.getKey();
            List<EventHandlerInfo> handlers = entry.getValue();
            
            if (handlerSubject.equals(subject)) {
                // 精确匹配，优先级最高
                exactMatches.addAll(handlers);
            } else if (isWildcardMatch(handlerSubject, subject)) {
                // 通配符匹配，优先级较低
                wildcardMatches.addAll(handlers);
            }
        }
        
        // 组合结果：精确匹配优先，通配符匹配其次
        List<EventHandlerInfo> result = new ArrayList<>();
        result.addAll(exactMatches);
        result.addAll(wildcardMatches);
        
        return result;
    }
    
    /**
     * 检查通配符匹配
     */
    private boolean isWildcardMatch(String pattern, String subject) {
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return subject.startsWith(prefix);
        }
        return false;
    }

    /**
     * 执行处理器
     */
    private void executeHandler(EventHandlerInfo handler, BaseEvent<?> event) {
        String handlerName = handler.getBean().getClass().getSimpleName() + "." + handler.getMethod().getName();
        
        try {
            log.info("🚀 开始执行处理器: {}, subject={}, eventId={}", 
                    handlerName, event.getSubject(), event.getEventId());
            
            Method method = handler.getMethod();
            Object bean = handler.getBean();
            
            // 检查方法参数类型
            Class<?>[] paramTypes = method.getParameterTypes();
            log.debug("🔍 处理器方法参数类型: {}", Arrays.toString(paramTypes));
            
            Object result = null;
            if (paramTypes.length == 1 && BaseEvent.class.isAssignableFrom(paramTypes[0])) {
                // 方法接受BaseEvent参数
                log.debug("📤 调用处理器方法，传递BaseEvent参数");
                result = method.invoke(bean, event);
            } else if (paramTypes.length == 1 && paramTypes[0].isAssignableFrom(event.getData().getClass())) {
                // 方法接受事件数据参数
                log.debug("📤 调用处理器方法，传递事件数据参数");
                result = method.invoke(bean, event.getData());
            } else {
                // 方法接受Object参数
                log.debug("📤 调用处理器方法，传递Object参数");
                result = method.invoke(bean, event);
            }
            
            log.info("✅ 事件处理器执行成功: {}, subject={}, result={}", 
                    handlerName, handler.getSubject(), result);
                    
        } catch (Exception e) {
            log.error("❌ 事件处理器执行失败: {}, subject={}, error={}", 
                    handlerName, handler.getSubject(), e.getMessage(), e);
        }
    }

    /**
     * 获取处理器总数
     */
    public int getTotalHandlerCount() {
        return eventHandlers.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * 获取所有处理器信息
     */
    public Map<String, List<EventHandlerInfo>> getAllHandlers() {
        return new HashMap<>(eventHandlers);
    }

    /**
     * 事件处理器信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EventHandlerInfo {
        private Object bean;
        private Method method;
        private String subject;
        private String eventType;
        private int priority;
        private boolean async;
        private int retryCount;
        private long timeout;
        private boolean enabled;
        private String description;
    }
} 