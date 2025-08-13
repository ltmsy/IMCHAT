package com.acme.im.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Gson配置类
 * 提供统一的Gson实例配置
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
public class GsonConfig {

    /**
     * 创建标准Gson实例
     * 配置日期格式、空值处理等
     */
    @Bean
    @Primary
    public Gson gson() {
        return new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls() // 序列化null值
            .create();
    }

    /**
     * 创建紧凑Gson实例
     * 不序列化null值，减少JSON大小
     */
    @Bean("compactGson")
    public Gson compactGson() {
        return new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
    }
} 