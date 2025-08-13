package com.acme.im.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置热更新机制
 * 支持Spring配置属性的动态热更新，无需重启应用
 * 
 * 特性：
 * 1. 属性热更新 - 动态更新@ConfigurationProperties标注的Bean
 * 2. 环境变量更新 - 更新Spring Environment中的属性
 * 3. Bean刷新 - 重新初始化依赖配置的Bean
 * 4. 回调机制 - 支持配置更新后的回调处理
 * 5. 类型安全 - 保证配置类型的正确性
 * 6. 异常处理 - 配置更新失败时的回滚机制
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class ConfigHotReload {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private DynamicConfigManager configManager;

    // 配置属性Bean缓存
    private final Map<String, Object> configurationBeans = new ConcurrentHashMap<>();
    
    // 属性更新回调
    private final Map<String, List<ConfigUpdateCallback>> updateCallbacks = new ConcurrentHashMap<>();
    
    // 动态属性源名称
    private static final String DYNAMIC_PROPERTY_SOURCE_NAME = "dynamicConfigProperties";

    /**
     * 配置更新回调接口
     */
    @FunctionalInterface
    public interface ConfigUpdateCallback {
        void onConfigUpdated(String propertyName, Object oldValue, Object newValue);
    }

    /**
     * 配置更新结果
     */
    public static class ConfigUpdateResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> updatedProperties;
        private final Exception error;

        public ConfigUpdateResult(boolean success, String message, Map<String, Object> updatedProperties, Exception error) {
            this.success = success;
            this.message = message;
            this.updatedProperties = updatedProperties != null ? updatedProperties : Collections.emptyMap();
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getUpdatedProperties() { return updatedProperties; }
        public Exception getError() { return error; }

        public static ConfigUpdateResult success(String message, Map<String, Object> updatedProperties) {
            return new ConfigUpdateResult(true, message, updatedProperties, null);
        }

        public static ConfigUpdateResult failure(String message, Exception error) {
            return new ConfigUpdateResult(false, message, null, error);
        }
    }

    /**
     * 初始化配置热更新机制
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化配置热更新机制...");
        
        // 扫描并注册@ConfigurationProperties Bean
        scanConfigurationPropertiesBeans();
        
        // 创建动态属性源
        createDynamicPropertySource();
        
        // 注册配置变更监听器
        registerConfigChangeListener();
        
        log.info("配置热更新机制初始化完成: 配置Bean数量={}", configurationBeans.size());
    }

    /**
     * 热更新单个配置属性
     * 
     * @param propertyName 属性名（支持.分隔的嵌套属性）
     * @param newValue 新值
     * @return 更新结果
     */
    public ConfigUpdateResult updateProperty(String propertyName, Object newValue) {
        try {
            log.info("开始热更新配置属性: {}={}", propertyName, newValue);
            
            Map<String, Object> updatedProperties = new HashMap<>();
            
            // 更新Environment中的属性
            updateEnvironmentProperty(propertyName, newValue);
            updatedProperties.put(propertyName, newValue);
            
            // 更新相关的配置Bean
            updateConfigurationBeans(propertyName, newValue);
            
            // 执行回调
            executeUpdateCallbacks(propertyName, null, newValue);
            
            log.info("配置属性热更新成功: {}", propertyName);
            return ConfigUpdateResult.success("配置属性更新成功", updatedProperties);
            
        } catch (Exception e) {
            log.error("配置属性热更新失败: {}={}", propertyName, newValue, e);
            return ConfigUpdateResult.failure("配置属性更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量热更新配置属性
     * 
     * @param properties 属性映射
     * @return 更新结果
     */
    public ConfigUpdateResult updateProperties(Map<String, Object> properties) {
        try {
            log.info("开始批量热更新配置属性: 数量={}", properties.size());
            
            Map<String, Object> updatedProperties = new HashMap<>();
            Map<String, Object> originalValues = new HashMap<>();
            
            // 备份原始值
            for (String propertyName : properties.keySet()) {
                originalValues.put(propertyName, getEnvironmentProperty(propertyName));
            }
            
            try {
                // 批量更新Environment属性
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    updateEnvironmentProperty(entry.getKey(), entry.getValue());
                    updatedProperties.put(entry.getKey(), entry.getValue());
                }
                
                // 批量更新配置Bean
                updateAllConfigurationBeans();
                
                // 批量执行回调
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    Object oldValue = originalValues.get(entry.getKey());
                    executeUpdateCallbacks(entry.getKey(), oldValue, entry.getValue());
                }
                
                log.info("批量配置属性热更新成功: 数量={}", properties.size());
                return ConfigUpdateResult.success("批量配置属性更新成功", updatedProperties);
                
            } catch (Exception e) {
                // 回滚操作
                log.warn("配置更新失败，开始回滚: {}", e.getMessage());
                rollbackProperties(originalValues);
                throw e;
            }
            
        } catch (Exception e) {
            log.error("批量配置属性热更新失败", e);
            return ConfigUpdateResult.failure("批量配置属性更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新指定的配置Bean
     * 
     * @param beanName Bean名称
     * @return 更新结果
     */
    public ConfigUpdateResult refreshConfigurationBean(String beanName) {
        try {
            log.info("开始刷新配置Bean: {}", beanName);
            
            Object bean = configurationBeans.get(beanName);
            if (bean == null) {
                return ConfigUpdateResult.failure("配置Bean不存在: " + beanName, null);
            }
            
            // 重新绑定配置属性
            rebindConfigurationProperties(bean);
            
            log.info("配置Bean刷新成功: {}", beanName);
            return ConfigUpdateResult.success("配置Bean刷新成功", Collections.singletonMap(beanName, bean));
            
        } catch (Exception e) {
            log.error("配置Bean刷新失败: {}", beanName, e);
            return ConfigUpdateResult.failure("配置Bean刷新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 添加配置更新回调
     * 
     * @param propertyName 属性名（支持*通配符）
     * @param callback 回调函数
     */
    public void addUpdateCallback(String propertyName, ConfigUpdateCallback callback) {
        updateCallbacks.computeIfAbsent(propertyName, k -> new ArrayList<>()).add(callback);
        log.debug("添加配置更新回调: {}", propertyName);
    }

    /**
     * 移除配置更新回调
     */
    public void removeUpdateCallback(String propertyName, ConfigUpdateCallback callback) {
        List<ConfigUpdateCallback> callbacks = updateCallbacks.get(propertyName);
        if (callbacks != null) {
            callbacks.remove(callback);
            if (callbacks.isEmpty()) {
                updateCallbacks.remove(propertyName);
            }
        }
    }

    /**
     * 获取当前所有配置属性
     */
    public Map<String, Object> getAllProperties() {
        Map<String, Object> result = new HashMap<>();
        
        ConfigurableEnvironment env = (ConfigurableEnvironment) applicationContext.getEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();
        
        propertySources.forEach(propertySource -> {
            if (propertySource instanceof MapPropertySource) {
                MapPropertySource mapSource = (MapPropertySource) propertySource;
                result.putAll(mapSource.getSource());
            }
        });
        
        return result;
    }

    /**
     * 获取配置Bean信息
     */
    public Map<String, Object> getConfigurationBeansInfo() {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : configurationBeans.entrySet()) {
            Object bean = entry.getValue();
            Map<String, Object> beanInfo = new HashMap<>();
            beanInfo.put("class", bean.getClass().getName());
            beanInfo.put("properties", extractBeanProperties(bean));
            result.put(entry.getKey(), beanInfo);
        }
        
        return result;
    }

    // ================================
    // 私有辅助方法
    // ================================

    /**
     * 扫描@ConfigurationProperties Bean
     */
    private void scanConfigurationPropertiesBeans() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        
        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                Class<?> beanClass = bean.getClass();
                
                // 检查是否有@ConfigurationProperties注解
                if (beanClass.isAnnotationPresent(ConfigurationProperties.class)) {
                    configurationBeans.put(beanName, bean);
                    log.debug("发现配置属性Bean: {} -> {}", beanName, beanClass.getName());
                }
            } catch (Exception e) {
                log.debug("跳过Bean扫描: {}", beanName);
            }
        }
    }

    /**
     * 创建动态属性源
     */
    private void createDynamicPropertySource() {
        ConfigurableEnvironment env = (ConfigurableEnvironment) applicationContext.getEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();
        
        // 检查动态属性源是否已存在
        if (!propertySources.contains(DYNAMIC_PROPERTY_SOURCE_NAME)) {
            Map<String, Object> dynamicProperties = new ConcurrentHashMap<>();
            MapPropertySource dynamicSource = new MapPropertySource(DYNAMIC_PROPERTY_SOURCE_NAME, dynamicProperties);
            
            // 添加到最高优先级
            propertySources.addFirst(dynamicSource);
            log.debug("创建动态属性源: {}", DYNAMIC_PROPERTY_SOURCE_NAME);
        }
    }

    /**
     * 更新Environment中的属性
     */
    private void updateEnvironmentProperty(String propertyName, Object newValue) {
        ConfigurableEnvironment env = (ConfigurableEnvironment) applicationContext.getEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();
        
        MapPropertySource dynamicSource = (MapPropertySource) propertySources.get(DYNAMIC_PROPERTY_SOURCE_NAME);
        if (dynamicSource != null) {
            dynamicSource.getSource().put(propertyName, newValue);
            log.debug("更新Environment属性: {}={}", propertyName, newValue);
        }
    }

    /**
     * 获取Environment中的属性
     */
    private Object getEnvironmentProperty(String propertyName) {
        return applicationContext.getEnvironment().getProperty(propertyName);
    }

    /**
     * 更新相关的配置Bean
     */
    private void updateConfigurationBeans(String propertyName, Object newValue) {
        for (Map.Entry<String, Object> entry : configurationBeans.entrySet()) {
            try {
                Object bean = entry.getValue();
                if (isPropertyRelevant(bean, propertyName)) {
                    rebindConfigurationProperties(bean);
                    log.debug("更新配置Bean: {} 属性: {}", entry.getKey(), propertyName);
                }
            } catch (Exception e) {
                log.warn("更新配置Bean失败: {} 属性: {}", entry.getKey(), propertyName, e);
            }
        }
    }

    /**
     * 更新所有配置Bean
     */
    private void updateAllConfigurationBeans() {
        for (Map.Entry<String, Object> entry : configurationBeans.entrySet()) {
            try {
                rebindConfigurationProperties(entry.getValue());
                log.debug("更新配置Bean: {}", entry.getKey());
            } catch (Exception e) {
                log.warn("更新配置Bean失败: {}", entry.getKey(), e);
            }
        }
    }

    /**
     * 重新绑定配置属性
     */
    private void rebindConfigurationProperties(Object bean) {
        // 这里可以使用Spring Boot的Binder来重新绑定属性
        // 简化实现：通过反射更新字段值
        Class<?> beanClass = bean.getClass();
        ConfigurationProperties annotation = beanClass.getAnnotation(ConfigurationProperties.class);
        
        if (annotation != null) {
            String prefix = annotation.prefix();
            updateBeanFields(bean, prefix);
        }
    }

    /**
     * 更新Bean字段
     */
    private void updateBeanFields(Object bean, String prefix) {
        Class<?> beanClass = bean.getClass();
        Field[] fields = beanClass.getDeclaredFields();
        
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                String propertyName = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
                Object value = applicationContext.getEnvironment().getProperty(propertyName, field.getType());
                
                if (value != null) {
                    field.set(bean, value);
                    log.debug("更新Bean字段: {}.{} = {}", bean.getClass().getSimpleName(), field.getName(), value);
                }
            } catch (Exception e) {
                log.debug("更新Bean字段失败: {}.{}", bean.getClass().getSimpleName(), field.getName());
            }
        }
    }

    /**
     * 检查属性是否与Bean相关
     */
    private boolean isPropertyRelevant(Object bean, String propertyName) {
        Class<?> beanClass = bean.getClass();
        ConfigurationProperties annotation = beanClass.getAnnotation(ConfigurationProperties.class);
        
        if (annotation != null) {
            String prefix = annotation.prefix();
            return propertyName.startsWith(prefix);
        }
        
        return false;
    }

    /**
     * 执行更新回调
     */
    private void executeUpdateCallbacks(String propertyName, Object oldValue, Object newValue) {
        // 执行特定属性的回调
        List<ConfigUpdateCallback> callbacks = updateCallbacks.get(propertyName);
        if (callbacks != null) {
            for (ConfigUpdateCallback callback : callbacks) {
                try {
                    callback.onConfigUpdated(propertyName, oldValue, newValue);
                } catch (Exception e) {
                    log.error("配置更新回调执行失败: {}", propertyName, e);
                }
            }
        }
        
        // 执行通配符回调
        List<ConfigUpdateCallback> wildcardCallbacks = updateCallbacks.get("*");
        if (wildcardCallbacks != null) {
            for (ConfigUpdateCallback callback : wildcardCallbacks) {
                try {
                    callback.onConfigUpdated(propertyName, oldValue, newValue);
                } catch (Exception e) {
                    log.error("通配符配置更新回调执行失败: {}", propertyName, e);
                }
            }
        }
    }

    /**
     * 回滚属性
     */
    private void rollbackProperties(Map<String, Object> originalValues) {
        for (Map.Entry<String, Object> entry : originalValues.entrySet()) {
            try {
                updateEnvironmentProperty(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("回滚属性失败: {}={}", entry.getKey(), entry.getValue(), e);
            }
        }
        
        // 重新更新所有配置Bean
        updateAllConfigurationBeans();
    }

    /**
     * 提取Bean属性
     */
    private Map<String, Object> extractBeanProperties(Object bean) {
        Map<String, Object> properties = new HashMap<>();
        Class<?> beanClass = bean.getClass();
        
        // 通过getter方法获取属性
        Method[] methods = beanClass.getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                try {
                    String propertyName = method.getName().substring(3);
                    propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
                    Object value = method.invoke(bean);
                    properties.put(propertyName, value);
                } catch (Exception e) {
                    log.debug("提取Bean属性失败: {}.{}", beanClass.getSimpleName(), method.getName());
                }
            }
        }
        
        return properties;
    }

    /**
     * 注册配置变更监听器
     */
    private void registerConfigChangeListener() {
        configManager.addGlobalConfigChangeListener(event -> {
            try {
                // 自动热更新配置
                updateProperty(event.getKey(), event.getNewValue());
                log.debug("自动热更新配置: {}={}", event.getKey(), event.getNewValue());
            } catch (Exception e) {
                log.error("自动热更新配置失败: {}", event.getKey(), e);
            }
        });
    }
} 