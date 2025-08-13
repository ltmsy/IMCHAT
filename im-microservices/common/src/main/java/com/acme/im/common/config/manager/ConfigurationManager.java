package com.acme.im.common.config.manager;

import com.acme.im.common.config.ConfigurationValidator;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置管理器
 * 负责管理各种配置的加载、验证和更新
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class ConfigurationManager {

    private final Gson gson;
    
    // 配置缓存
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    
    // 配置验证器
    private final Map<String, ConfigurationValidator> validators = new ConcurrentHashMap<>();

    public ConfigurationManager(Gson gson) {
        this.gson = gson;
    }

    /**
     * 加载配置
     * 
     * @param key 配置键
     * @param configClass 配置类型
     * @return 配置对象
     */
    public <T> T loadConfig(String key, Class<T> configClass) {
        try {
            Object cached = configCache.get(key);
            if (cached != null && configClass.isInstance(cached)) {
                return configClass.cast(cached);
            }
            
            // 从配置源加载配置
            T config = loadFromSource(key, configClass);
            if (config != null) {
                configCache.put(key, config);
                log.info("配置加载成功: key={}, type={}", key, configClass.getSimpleName());
            }
            
            return config;
        } catch (Exception e) {
            log.error("配置加载失败: key={}, type={}", key, configClass.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 更新配置
     * 
     * @param key 配置键
     * @param config 配置对象
     */
    public void updateConfig(String key, Object config) {
        try {
            // 验证配置
            ConfigurationValidator validator = validators.get(key);
            if (validator != null) {
                ConfigurationValidator.ValidationResult result = validator.validate();
                if (!result.isValid()) {
                    log.warn("配置验证失败: key={}, reason={}", key, result.getMessage());
                    return;
                }
            }
            
            // 更新缓存
            configCache.put(key, config);
            log.info("配置更新成功: key={}, type={}", key, config.getClass().getSimpleName());
            
        } catch (Exception e) {
            log.error("配置更新失败: key={}", key, e);
        }
    }

    /**
     * 注册配置验证器
     * 
     * @param key 配置键
     * @param validator 验证器
     */
    public void registerValidator(String key, ConfigurationValidator validator) {
        validators.put(key, validator);
        log.debug("配置验证器注册成功: key={}, validator={}", key, validator.getClass().getSimpleName());
    }

    /**
     * 获取配置
     * 
     * @param key 配置键
     * @return 配置对象
     */
    public Object getConfig(String key) {
        return configCache.get(key);
    }

    /**
     * 清除配置缓存
     */
    public void clearCache() {
        configCache.clear();
        log.info("配置缓存已清除");
    }

    /**
     * 从配置源加载配置
     * 
     * @param key 配置键
     * @param configClass 配置类型
     * @return 配置对象
     */
    private <T> T loadFromSource(String key, Class<T> configClass) {
        // 这里可以从数据库、配置文件、远程配置中心等加载
        // 简化实现，返回null
        log.debug("从配置源加载配置: key={}, type={}", key, configClass.getSimpleName());
        return null;
    }

    /**
     * 将配置转换为JSON字符串
     * 
     * @param config 配置对象
     * @return JSON字符串
     */
    public String configToJson(Object config) {
        try {
            return gson.toJson(config);
        } catch (Exception e) {
            log.error("配置序列化失败: type={}", config.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * 从JSON字符串解析配置
     * 
     * @param json JSON字符串
     * @param configClass 配置类型
     * @return 配置对象
     */
    public <T> T configFromJson(String json, Class<T> configClass) {
        try {
            return gson.fromJson(json, configClass);
        } catch (JsonSyntaxException e) {
            log.error("JSON解析失败: json={}, type={}", json, configClass.getSimpleName(), e);
            return null;
        } catch (Exception e) {
            log.error("配置反序列化失败: json={}, type={}", json, e);
            return null;
        }
    }

    /**
     * 获取配置统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("cacheSize", configCache.size());
        stats.put("validatorCount", validators.size());
        stats.put("cachedKeys", configCache.keySet());
        return stats;
    }
} 