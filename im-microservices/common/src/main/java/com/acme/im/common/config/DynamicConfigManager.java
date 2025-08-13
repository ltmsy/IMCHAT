package com.acme.im.common.config;

import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import com.acme.im.common.infrastructure.nats.subscriber.EventSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 动态配置管理器
 * 提供配置的动态加载、热更新和变化通知机制
 * 
 * 特性：
 * 1. 热更新 - 配置变更无需重启应用
 * 2. 多数据源 - 支持Redis、数据库、文件等配置源
 * 3. 变化通知 - 配置变更事件通知
 * 4. 类型转换 - 自动类型转换和验证
 * 5. 缓存机制 - 本地缓存提升访问性能
 * 6. 配置监听 - 支持配置变更监听器
 * 7. 优先级管理 - 多配置源优先级处理
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class DynamicConfigManager {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private AsyncEventPublisher eventPublisher;
    
    @Autowired
    private EventSubscriber eventSubscriber;
    
    @Autowired
    private ObjectMapper objectMapper;

    // 配置缓存
    private final ConcurrentHashMap<String, ConfigEntry> configCache = new ConcurrentHashMap<>();
    
    // 配置监听器
    private final ConcurrentHashMap<String, List<ConfigChangeListener>> listeners = new ConcurrentHashMap<>();
    
    // 配置源
    private final List<ConfigSource> configSources = new CopyOnWriteArrayList<>();
    
    // 定时刷新任务
    private ScheduledExecutorService refreshExecutor;
    
    // 配置常量
    private static final String CONFIG_KEY_PREFIX = "config:";
    private static final String CONFIG_CHANGE_SUBJECT = "config.change";
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(5);

    /**
     * 配置条目
     */
    private static class ConfigEntry {
        private final Object value;
        private final long timestamp;
        private final String source;
        private final Duration ttl;

        public ConfigEntry(Object value, String source, Duration ttl) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.source = source;
            this.ttl = ttl;
        }

        public Object getValue() { return value; }
        public long getTimestamp() { return timestamp; }
        public String getSource() { return source; }
        public Duration getTtl() { return ttl; }
        
        public boolean isExpired() {
            return ttl != null && (System.currentTimeMillis() - timestamp) > ttl.toMillis();
        }
    }

    /**
     * 配置变更事件
     */
    public static class ConfigChangeEvent {
        private final String key;
        private final Object oldValue;
        private final Object newValue;
        private final String source;
        private final long timestamp;

        public ConfigChangeEvent(String key, Object oldValue, Object newValue, String source) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.source = source;
            this.timestamp = System.currentTimeMillis();
        }

        public String getKey() { return key; }
        public Object getOldValue() { return oldValue; }
        public Object getNewValue() { return newValue; }
        public String getSource() { return source; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * 配置变更监听器
     */
    @FunctionalInterface
    public interface ConfigChangeListener {
        void onConfigChange(ConfigChangeEvent event);
    }

    /**
     * 配置源接口
     */
    public interface ConfigSource {
        String getName();
        int getPriority(); // 优先级，数字越大优先级越高
        Object getValue(String key);
        Map<String, Object> getAllValues();
        boolean supports(String key);
        void refresh();
    }

    /**
     * Redis配置源
     */
    private class RedisConfigSource implements ConfigSource {
        
        @Override
        public String getName() {
            return "redis";
        }

        @Override
        public int getPriority() {
            return 100; // 高优先级
        }

        @Override
        public Object getValue(String key) {
            try {
                return redisTemplate.opsForValue().get(CONFIG_KEY_PREFIX + key);
            } catch (Exception e) {
                log.warn("从Redis获取配置失败: key={}", key, e);
                return null;
            }
        }

        @Override
        public Map<String, Object> getAllValues() {
            try {
                Set<String> keys = redisTemplate.keys(CONFIG_KEY_PREFIX + "*");
                if (keys == null || keys.isEmpty()) {
                    return Collections.emptyMap();
                }
                
                Map<String, Object> result = new HashMap<>();
                List<Object> values = redisTemplate.opsForValue().multiGet(keys);
                
                int index = 0;
                for (String key : keys) {
                    String configKey = key.substring(CONFIG_KEY_PREFIX.length());
                    result.put(configKey, values.get(index++));
                }
                
                return result;
            } catch (Exception e) {
                log.error("从Redis获取所有配置失败", e);
                return Collections.emptyMap();
            }
        }

        @Override
        public boolean supports(String key) {
            return true; // Redis支持所有配置
        }

        @Override
        public void refresh() {
            // Redis配置实时更新，无需刷新
        }
    }

    /**
     * 系统属性配置源
     */
    private class SystemPropertiesConfigSource implements ConfigSource {
        
        @Override
        public String getName() {
            return "system-properties";
        }

        @Override
        public int getPriority() {
            return 50; // 中等优先级
        }

        @Override
        public Object getValue(String key) {
            return System.getProperty(key);
        }

        @Override
        public Map<String, Object> getAllValues() {
            Map<String, Object> result = new HashMap<>();
            Properties props = System.getProperties();
            props.forEach((k, v) -> result.put(k.toString(), v));
            return result;
        }

        @Override
        public boolean supports(String key) {
            return System.getProperty(key) != null;
        }

        @Override
        public void refresh() {
            // 系统属性无需刷新
        }
    }

    /**
     * 初始化动态配置管理器
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化动态配置管理器...");
        
        // 注册配置源
        registerConfigSource(new RedisConfigSource());
        registerConfigSource(new SystemPropertiesConfigSource());
        
        // 创建定时刷新任务
        refreshExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "config-refresh");
            t.setDaemon(true);
            return t;
        });
        
        // 启动定时刷新
        refreshExecutor.scheduleAtFixedRate(this::refreshConfigs, 30, 30, TimeUnit.SECONDS);
        
        // 订阅配置变更事件
        subscribeToConfigChanges();
        
        log.info("动态配置管理器初始化完成: 配置源数量={}", configSources.size());
    }

    /**
     * 注册配置源
     */
    public void registerConfigSource(ConfigSource configSource) {
        configSources.add(configSource);
        // 按优先级排序
        configSources.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        log.info("注册配置源: name={}, priority={}", configSource.getName(), configSource.getPriority());
    }

    /**
     * 获取配置值
     */
    public <T> T getConfig(String key, Class<T> type) {
        return getConfig(key, type, null);
    }

    /**
     * 获取配置值（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfig(String key, Class<T> type, T defaultValue) {
        try {
            // 检查本地缓存
            ConfigEntry entry = configCache.get(key);
            if (entry != null && !entry.isExpired()) {
                return convertValue(entry.getValue(), type);
            }

            // 从配置源获取
            Object value = getValueFromSources(key);
            if (value != null) {
                // 缓存配置
                configCache.put(key, new ConfigEntry(value, "unknown", DEFAULT_CACHE_TTL));
                return convertValue(value, type);
            }

            return defaultValue;
            
        } catch (Exception e) {
            log.error("获取配置失败: key={}, type={}", key, type.getSimpleName(), e);
            return defaultValue;
        }
    }

    /**
     * 设置配置值
     */
    public void setConfig(String key, Object value) {
        setConfig(key, value, "manual");
    }

    /**
     * 设置配置值（指定来源）
     */
    public void setConfig(String key, Object value, String source) {
        try {
            Object oldValue = getConfig(key, Object.class);
            
            // 保存到Redis
            redisTemplate.opsForValue().set(CONFIG_KEY_PREFIX + key, value);
            
            // 更新本地缓存
            configCache.put(key, new ConfigEntry(value, source, DEFAULT_CACHE_TTL));
            
            // 发布变更事件
            publishConfigChange(key, oldValue, value, source);
            
            log.info("配置已更新: key={}, value={}, source={}", key, value, source);
            
        } catch (Exception e) {
            log.error("设置配置失败: key={}, value={}", key, value, e);
            throw new RuntimeException("设置配置失败", e);
        }
    }

    /**
     * 删除配置
     */
    public void removeConfig(String key) {
        try {
            Object oldValue = getConfig(key, Object.class);
            
            // 从Redis删除
            redisTemplate.delete(CONFIG_KEY_PREFIX + key);
            
            // 从本地缓存删除
            configCache.remove(key);
            
            // 发布变更事件
            publishConfigChange(key, oldValue, null, "manual");
            
            log.info("配置已删除: key={}", key);
            
        } catch (Exception e) {
            log.error("删除配置失败: key={}", key, e);
            throw new RuntimeException("删除配置失败", e);
        }
    }

    /**
     * 添加配置变更监听器
     */
    public void addConfigChangeListener(String key, ConfigChangeListener listener) {
        listeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.debug("添加配置监听器: key={}", key);
    }

    /**
     * 移除配置变更监听器
     */
    public void removeConfigChangeListener(String key, ConfigChangeListener listener) {
        List<ConfigChangeListener> keyListeners = listeners.get(key);
        if (keyListeners != null) {
            keyListeners.remove(listener);
            if (keyListeners.isEmpty()) {
                listeners.remove(key);
            }
        }
    }

    /**
     * 添加全局配置变更监听器
     */
    public void addGlobalConfigChangeListener(ConfigChangeListener listener) {
        addConfigChangeListener("*", listener);
    }

    /**
     * 获取所有配置
     */
    public Map<String, Object> getAllConfigs() {
        Map<String, Object> result = new HashMap<>();
        
        // 从所有配置源获取配置
        for (ConfigSource source : configSources) {
            try {
                Map<String, Object> sourceConfigs = source.getAllValues();
                // 低优先级的配置不会覆盖高优先级的配置
                sourceConfigs.forEach(result::putIfAbsent);
            } catch (Exception e) {
                log.warn("从配置源获取配置失败: source={}", source.getName(), e);
            }
        }
        
        return result;
    }

    /**
     * 刷新配置缓存
     */
    public void refreshConfigs() {
        try {
            log.debug("开始刷新配置缓存...");
            
            // 清理过期缓存
            configCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            
            // 刷新配置源
            for (ConfigSource source : configSources) {
                try {
                    source.refresh();
                } catch (Exception e) {
                    log.warn("刷新配置源失败: source={}", source.getName(), e);
                }
            }
            
            log.debug("配置缓存刷新完成: 缓存大小={}", configCache.size());
            
        } catch (Exception e) {
            log.error("刷新配置缓存异常", e);
        }
    }

    /**
     * 销毁动态配置管理器
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭动态配置管理器...");
        
        if (refreshExecutor != null) {
            refreshExecutor.shutdown();
            try {
                if (!refreshExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    refreshExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                refreshExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        configCache.clear();
        listeners.clear();
        configSources.clear();
        
        log.info("动态配置管理器已关闭");
    }

    // ================================
    // 私有辅助方法
    // ================================

    /**
     * 从配置源获取值
     */
    private Object getValueFromSources(String key) {
        for (ConfigSource source : configSources) {
            try {
                if (source.supports(key)) {
                    Object value = source.getValue(key);
                    if (value != null) {
                        log.debug("从配置源获取配置: key={}, source={}, value={}", 
                                key, source.getName(), value);
                        return value;
                    }
                }
            } catch (Exception e) {
                log.warn("从配置源获取配置失败: key={}, source={}", key, source.getName(), e);
            }
        }
        return null;
    }

    /**
     * 类型转换
     */
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        try {
            // 字符串转换
            if (type == String.class) {
                return (T) value.toString();
            }
            
            // 数值转换
            if (type == Integer.class || type == int.class) {
                return (T) Integer.valueOf(value.toString());
            }
            if (type == Long.class || type == long.class) {
                return (T) Long.valueOf(value.toString());
            }
            if (type == Double.class || type == double.class) {
                return (T) Double.valueOf(value.toString());
            }
            if (type == Boolean.class || type == boolean.class) {
                return (T) Boolean.valueOf(value.toString());
            }
            
            // JSON转换
            if (value instanceof String) {
                return objectMapper.readValue((String) value, type);
            }
            
            // 对象转换
            return objectMapper.convertValue(value, type);
            
        } catch (Exception e) {
            log.error("配置类型转换失败: value={}, type={}", value, type.getSimpleName(), e);
            throw new RuntimeException("配置类型转换失败", e);
        }
    }

    /**
     * 发布配置变更事件
     */
    private void publishConfigChange(String key, Object oldValue, Object newValue, String source) {
        try {
            ConfigChangeEvent event = new ConfigChangeEvent(key, oldValue, newValue, source);
            
            // 发布到NATS
            eventPublisher.publishEventAsync(CONFIG_CHANGE_SUBJECT, event);
            
            // 通知本地监听器
            notifyListeners(event);
            
        } catch (Exception e) {
            log.error("发布配置变更事件失败: key={}", key, e);
        }
    }

    /**
     * 通知监听器
     */
    private void notifyListeners(ConfigChangeEvent event) {
        // 通知特定键的监听器
        List<ConfigChangeListener> keyListeners = listeners.get(event.getKey());
        if (keyListeners != null) {
            for (ConfigChangeListener listener : keyListeners) {
                try {
                    listener.onConfigChange(event);
                } catch (Exception e) {
                    log.error("配置变更监听器执行失败: key={}", event.getKey(), e);
                }
            }
        }
        
        // 通知全局监听器
        List<ConfigChangeListener> globalListeners = listeners.get("*");
        if (globalListeners != null) {
            for (ConfigChangeListener listener : globalListeners) {
                try {
                    listener.onConfigChange(event);
                } catch (Exception e) {
                    log.error("全局配置变更监听器执行失败: key={}", event.getKey(), e);
                }
            }
        }
    }

    /**
     * 订阅配置变更事件
     */
    private void subscribeToConfigChanges() {
        eventSubscriber.subscribe(CONFIG_CHANGE_SUBJECT, message -> {
            try {
                ConfigChangeEvent event = objectMapper.readValue(message.toString(), ConfigChangeEvent.class);
                
                // 更新本地缓存
                if (event.getNewValue() != null) {
                    configCache.put(event.getKey(), 
                            new ConfigEntry(event.getNewValue(), event.getSource(), DEFAULT_CACHE_TTL));
                } else {
                    configCache.remove(event.getKey());
                }
                
                // 通知监听器
                notifyListeners(event);
                
                log.debug("处理配置变更事件: key={}, source={}", event.getKey(), event.getSource());
                
            } catch (Exception e) {
                log.error("处理配置变更事件失败", e);
            }
        });
    }
} 