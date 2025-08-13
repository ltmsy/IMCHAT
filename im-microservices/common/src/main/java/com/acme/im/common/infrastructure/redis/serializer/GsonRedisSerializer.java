package com.acme.im.common.infrastructure.redis.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Gson Redis序列化器
 * 提供安全的JSON序列化和反序列化
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
public class GsonRedisSerializer implements RedisSerializer<Object> {

    private final Gson gson;
    
    // 受信的包列表，限制反序列化
    private static final List<String> TRUSTED_PACKAGES = Arrays.asList(
        "com.acme.im.common.dto",
        "com.acme.im.business.dto", 
        "com.acme.im.communication.dto",
        "com.acme.im.admin.dto",
        "java.util",
        "java.lang"
    );

    public GsonRedisSerializer() {
        this.gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
    }

    @Override
    public byte[] serialize(Object object) throws SerializationException {
        if (object == null) {
            return new byte[0];
        }
        
        try {
            String json = gson.toJson(object);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("序列化对象失败: {}", object.getClass().getSimpleName(), e);
            throw new SerializationException("序列化失败", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        try {
            String json = new String(bytes, StandardCharsets.UTF_8);
            
            // 安全检查：只允许反序列化受信的类型
            if (!isTrustedJson(json)) {
                log.warn("尝试反序列化不受信的JSON: {}", json);
                throw new SerializationException("反序列化被拒绝：不受信的类型");
            }
            
            // 使用Object.class作为目标类型，Gson会尝试推断类型
            return gson.fromJson(json, Object.class);
        } catch (Exception e) {
            log.error("反序列化失败", e);
            throw new SerializationException("反序列化失败", e);
        }
    }

    /**
     * 检查JSON是否来自受信的包
     * 这是一个简化的安全检查，实际项目中可能需要更复杂的验证
     */
    private boolean isTrustedJson(String json) {
        // 简单的安全检查：检查是否包含受信包的特征
        return TRUSTED_PACKAGES.stream()
            .anyMatch(pkg -> json.contains(pkg.replace(".", "/")));
    }
} 