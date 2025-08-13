package com.acme.im.common.infrastructure.redis.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Redisson配置类
 * 配置Redisson客户端连接Redis
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class RedissonConfig {

    private final RedisProperties redisProperties;

    /**
     * 创建Redisson客户端
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 单机模式配置
        config.useSingleServer()
                .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase())
                .setConnectionPoolSize(redisProperties.getLettuce().getPool().getMaxActive())
                .setConnectionMinimumIdleSize(redisProperties.getLettuce().getPool().getMinIdle())
                .setIdleConnectionTimeout((int) redisProperties.getTimeout().toMillis())
                .setConnectTimeout(Math.toIntExact(redisProperties.getTimeout().toMillis()));
        
        // 如果设置了密码，则配置密码
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            config.useSingleServer().setPassword(redisProperties.getPassword());
        }
        
        return Redisson.create(config);
    }
} 