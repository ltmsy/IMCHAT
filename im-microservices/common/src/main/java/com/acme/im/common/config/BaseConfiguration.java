package com.acme.im.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

/**
 * 配置基类
 * 提供通用的配置方法和验证
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
public abstract class BaseConfiguration {

    protected final Environment environment;

    protected BaseConfiguration(Environment environment) {
        this.environment = environment;
    }

    /**
     * 配置初始化后的验证
     */
    @PostConstruct
    protected void validateConfiguration() {
        log.info("开始验证配置: {}", this.getClass().getSimpleName());
        try {
            doValidate();
            log.info("配置验证通过: {}", this.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("配置验证失败: {}", this.getClass().getSimpleName(), e);
            throw new IllegalStateException("配置验证失败: " + this.getClass().getSimpleName(), e);
        }
    }

    /**
     * 子类实现具体的验证逻辑
     */
    protected abstract void doValidate();

    /**
     * 获取配置值，如果不存在则返回默认值
     */
    protected String getProperty(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

    /**
     * 获取必需的配置值
     */
    protected String getRequiredProperty(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("必需的配置项缺失: " + key);
        }
        return value;
    }

    /**
     * 获取整数配置值
     */
    protected int getIntProperty(String key, int defaultValue) {
        return environment.getProperty(key, Integer.class, defaultValue);
    }

    /**
     * 获取长整型配置值
     */
    protected long getLongProperty(String key, long defaultValue) {
        return environment.getProperty(key, Long.class, defaultValue);
    }

    /**
     * 获取布尔配置值
     */
    protected boolean getBooleanProperty(String key, boolean defaultValue) {
        return environment.getProperty(key, Boolean.class, defaultValue);
    }
} 