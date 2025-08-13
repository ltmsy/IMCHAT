package com.acme.im.common.infrastructure.minio.config;

import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * MinIO配置类
 * 支持S3兼容API、连接池、重试机制等
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class MinioConfig {

    /**
     * MinIO属性配置
     */
    @Bean
    @ConfigurationProperties(prefix = "minio")
    public MinioProperties minioProperties() {
        return new MinioProperties();
    }

    /**
     * MinIO客户端
     */
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(properties.getEndpoint())
                    .credentials(properties.getAccessKey(), properties.getSecretKey())
                    .region(properties.getRegion())
                    .httpClient(createHttpClient(properties))
                    .build();

            // 测试连接
            if (properties.isTestConnection()) {
                testConnection(minioClient, properties);
            }

            log.info("MinIO客户端初始化成功: endpoint={}, region={}", 
                    properties.getEndpoint(), properties.getRegion());
            return minioClient;

        } catch (Exception e) {
            log.error("MinIO客户端初始化失败: endpoint={}", properties.getEndpoint(), e);
            throw new RuntimeException("MinIO客户端初始化失败", e);
        }
    }

    /**
     * 创建HTTP客户端
     */
    private okhttp3.OkHttpClient createHttpClient(MinioProperties properties) {
        return new okhttp3.OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeout()))
                .readTimeout(Duration.ofSeconds(properties.getReadTimeout()))
                .writeTimeout(Duration.ofSeconds(properties.getWriteTimeout()))
                .retryOnConnectionFailure(properties.isRetryOnConnectionFailure())
                .connectionPool(new okhttp3.ConnectionPool(
                        properties.getMaxIdleConnections(),
                        properties.getKeepAliveDuration(),
                        TimeUnit.MINUTES))
                .addInterceptor(chain -> {
                    okhttp3.Request request = chain.request();
                    log.debug("MinIO请求: {} {}", request.method(), request.url());
                    return chain.proceed(request);
                })
                .build();
    }

    /**
     * 测试连接
     */
    private void testConnection(MinioClient minioClient, MinioProperties properties) {
        try {
            // 检查存储桶是否存在
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(properties.getBucket())
                            .build());

            if (!bucketExists) {
                log.warn("存储桶不存在: {}", properties.getBucket());
                if (properties.isCreateBucketIfNotExists()) {
                    minioClient.makeBucket(MakeBucketArgs.builder()
                            .bucket(properties.getBucket())
                            .build());
                    log.info("存储桶创建成功: {}", properties.getBucket());
                }
            } else {
                log.info("存储桶连接正常: {}", properties.getBucket());
            }

        } catch (Exception e) {
            log.error("MinIO连接测试失败", e);
            if (properties.isFailOnConnectionError()) {
                throw new RuntimeException("MinIO连接测试失败", e);
            }
        }
    }

    /**
     * MinIO属性配置类
     */
    @Data
    public static class MinioProperties {
        /**
         * MinIO服务端点
         */
        private String endpoint = "http://localhost:9000";

        /**
         * 访问密钥
         */
        private String accessKey = "minioadmin";

        /**
         * 秘密密钥
         */
        private String secretKey = "minioadmin";

        /**
         * 区域
         */
        private String region = "us-east-1";

        /**
         * 存储桶名称
         */
        private String bucket = "im-files";

        /**
         * 连接超时时间（秒）
         */
        private int connectTimeout = 10;

        /**
         * 读取超时时间（秒）
         */
        private int readTimeout = 30;

        /**
         * 写入超时时间（秒）
         */
        private int writeTimeout = 30;

        /**
         * 连接失败时是否重试
         */
        private boolean retryOnConnectionFailure = true;

        /**
         * 最大空闲连接数
         */
        private int maxIdleConnections = 10;

        /**
         * 连接保活时间（分钟）
         */
        private int keepAliveDuration = 5;

        /**
         * 是否测试连接
         */
        private boolean testConnection = true;

        /**
         * 连接失败时是否抛出异常
         */
        private boolean failOnConnectionError = false;

        /**
         * 存储桶不存在时是否自动创建
         */
        private boolean createBucketIfNotExists = true;

        /**
         * 默认分片大小（字节）
         */
        private long defaultPartSize = 5 * 1024 * 1024; // 5MB

        /**
         * 最大分片大小（字节）
         */
        private long maxPartSize = 5 * 1024 * 1024 * 1024L; // 5GB

        /**
         * 是否启用HTTPS
         */
        private boolean secure = false;

        /**
         * 是否启用路径样式访问
         */
        private boolean pathStyleAccess = true;

        /**
         * 是否启用虚拟主机样式访问
         */
        private boolean virtualHostStyleAccess = false;

        /**
         * 是否启用对象锁定
         */
        private boolean objectLockEnabled = false;

        /**
         * 是否启用版本控制
         */
        private boolean versioningEnabled = false;

        /**
         * 是否启用加密
         */
        private boolean encryptionEnabled = false;

        /**
         * 加密算法
         */
        private String encryptionAlgorithm = "AES256";

        /**
         * 是否启用压缩
         */
        private boolean compressionEnabled = false;

        /**
         * 压缩算法
         */
        private String compressionAlgorithm = "gzip";

        /**
         * 是否启用日志记录
         */
        private boolean loggingEnabled = true;

        /**
         * 日志级别
         */
        private String logLevel = "INFO";

        /**
         * 是否启用指标收集
         */
        private boolean metricsEnabled = true;

        /**
         * 是否启用健康检查
         */
        private boolean healthCheckEnabled = true;

        /**
         * 健康检查间隔（秒）
         */
        private int healthCheckInterval = 30;

        /**
         * 是否启用缓存
         */
        private boolean cacheEnabled = true;

        /**
         * 缓存大小（MB）
         */
        private int cacheSize = 100;

        /**
         * 缓存过期时间（分钟）
         */
        private int cacheExpiration = 60;
    }
} 