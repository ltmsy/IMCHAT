package com.acme.im.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gson配置类
 * 提供统一的Gson实例配置，支持LocalDateTime序列化
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
public class GsonConfig {

    // 日期时间格式化器
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * LocalDateTime序列化器
     */
    private static final JsonSerializer<LocalDateTime> LOCAL_DATE_TIME_SERIALIZER = 
        (src, typeOfSrc, context) -> new JsonPrimitive(DATE_TIME_FORMATTER.format(src));

    /**
     * LocalDateTime反序列化器
     */
    private static final JsonDeserializer<LocalDateTime> LOCAL_DATE_TIME_DESERIALIZER = 
        (json, typeOfT, context) -> {
            try {
                return LocalDateTime.parse(json.getAsString(), DATE_TIME_FORMATTER);
            } catch (Exception e) {
                throw new JsonParseException("无法解析LocalDateTime: " + json.getAsString(), e);
            }
        };

    /**
     * 创建标准Gson实例
     * 配置日期格式、空值处理、LocalDateTime支持等
     */
    @Bean
    @Primary
    public Gson gson() {
        return new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, LOCAL_DATE_TIME_SERIALIZER)
            .registerTypeAdapter(LocalDateTime.class, LOCAL_DATE_TIME_DESERIALIZER)
            .serializeNulls() // 序列化null值
            .create();
    }

    /**
     * 创建紧凑Gson实例
     * 不序列化null值，减少JSON大小，支持LocalDateTime
     */
    @Bean("compactGson")
    public Gson compactGson() {
        return new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, LOCAL_DATE_TIME_SERIALIZER)
            .registerTypeAdapter(LocalDateTime.class, LOCAL_DATE_TIME_DESERIALIZER)
            .create();
    }
} 