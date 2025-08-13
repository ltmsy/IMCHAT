package com.acme.im.common.infrastructure.redis.config;

import com.acme.im.common.infrastructure.redis.serializer.GsonRedisSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Redis配置类
 * 配置Lettuce客户端和序列化器
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@EnableRedisRepositories
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisProperties redisProperties;

    /**
     * 创建Redis连接工厂
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        config.setPassword(redisProperties.getPassword());
        config.setDatabase(redisProperties.getDatabase());
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * 创建安全的Gson序列化器
     * 限制反序列化只允许受信的包
     */
    private RedisSerializer<Object> createSecureGsonSerializer() {
        return new GsonRedisSerializer();
    }

    /**
     * 创建字符串序列化器
     */
    private StringRedisSerializer createStringSerializer() {
        return new StringRedisSerializer();
    }

    /**
     * 通用Redis模板配置
     */
    private <K, V> void configureRedisTemplate(RedisTemplate<K, V> template, 
                                             RedisConnectionFactory connectionFactory,
                                             RedisSerializer<Object> jsonSerializer,
                                             StringRedisSerializer stringSerializer) {
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();
    }

    /**
     * 主Redis模板配置
     * 使用安全的Gson序列化器进行JSON序列化
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        RedisSerializer<Object> jsonSerializer = createSecureGsonSerializer();
        StringRedisSerializer stringSerializer = createStringSerializer();
        
        configureRedisTemplate(template, connectionFactory, jsonSerializer, stringSerializer);
        
        return template;
    }

    /**
     * 字符串专用Redis模板
     * 专门用于处理字符串类型的数据
     */
    @Bean("customStringRedisTemplate")
    public RedisTemplate<String, String> customStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        StringRedisSerializer stringSerializer = createStringSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
} 