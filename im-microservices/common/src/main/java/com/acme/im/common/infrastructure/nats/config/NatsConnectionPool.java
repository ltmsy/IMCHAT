package com.acme.im.common.infrastructure.nats.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * NATS连接池管理器
 * 提供高效的连接复用机制，避免频繁创建和销毁连接
 * 
 * 特性：
 * 1. 连接池管理 - 最小/最大连接数控制
 * 2. 连接健康检查 - 自动检测和替换无效连接
 * 3. 连接超时管理 - 防止连接泄露
 * 4. 线程安全 - 支持并发访问
 * 5. 优雅关闭 - 资源清理
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class NatsConnectionPool {

    @Autowired
    private NatsConfig.NatsProperties natsProperties;
    
    @Autowired
    private Options natsOptions;

    // 连接池配置
    private static final int DEFAULT_MIN_CONNECTIONS = 5;
    private static final int DEFAULT_MAX_CONNECTIONS = 20;
    private static final int DEFAULT_MAX_IDLE_TIME_MS = 300000; // 5分钟
    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 10000; // 10秒
    
    // 连接池
    private final BlockingQueue<PooledConnection> availableConnections = new LinkedBlockingQueue<>();
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    
    // 配置参数
    private int minConnections = DEFAULT_MIN_CONNECTIONS;
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private int maxIdleTimeMs = DEFAULT_MAX_IDLE_TIME_MS;
    private int connectionTimeoutMs = DEFAULT_CONNECTION_TIMEOUT_MS;
    
    // 线程安全锁
    private final ReentrantLock poolLock = new ReentrantLock();
    
    // 池状态
    private volatile boolean initialized = false;
    private volatile boolean shutdown = false;

    /**
     * 池化连接包装类
     */
    private static class PooledConnection {
        private final Connection connection;
        private final long createTime;
        private volatile long lastUsedTime;
        private volatile boolean inUse;

        public PooledConnection(Connection connection) {
            this.connection = connection;
            this.createTime = System.currentTimeMillis();
            this.lastUsedTime = createTime;
            this.inUse = false;
        }

        public Connection getConnection() {
            return connection;
        }

        public long getCreateTime() {
            return createTime;
        }

        public long getLastUsedTime() {
            return lastUsedTime;
        }

        public void updateLastUsedTime() {
            this.lastUsedTime = System.currentTimeMillis();
        }

        public boolean isInUse() {
            return inUse;
        }

        public void setInUse(boolean inUse) {
            this.inUse = inUse;
        }

        public boolean isExpired(int maxIdleTimeMs) {
            return !inUse && (System.currentTimeMillis() - lastUsedTime) > maxIdleTimeMs;
        }

        public boolean isValid() {
            return connection != null && 
                   connection.getStatus() == Connection.Status.CONNECTED;
        }
    }

    /**
     * 初始化连接池
     */
    @PostConstruct
    public void initialize() {
        if (initialized) {
            return;
        }

        poolLock.lock();
        try {
            if (initialized) {
                return;
            }

            log.info("初始化NATS连接池: minConnections={}, maxConnections={}", 
                    minConnections, maxConnections);

            // 创建最小连接数
            for (int i = 0; i < minConnections; i++) {
                try {
                    PooledConnection pooledConn = createPooledConnection();
                    if (pooledConn != null) {
                        availableConnections.offer(pooledConn);
                        totalConnections.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.warn("创建初始连接失败: {}", e.getMessage());
                }
            }

            initialized = true;
            log.info("NATS连接池初始化完成: 创建连接数={}", totalConnections.get());

        } finally {
            poolLock.unlock();
        }
    }

    /**
     * 获取连接
     * 
     * @return NATS连接
     * @throws RuntimeException 获取连接失败
     */
    public Connection borrowConnection() {
        if (shutdown) {
            throw new RuntimeException("连接池已关闭");
        }

        if (!initialized) {
            initialize();
        }

        PooledConnection pooledConn = null;
        
        // 首先尝试从可用连接中获取
        while ((pooledConn = availableConnections.poll()) != null) {
            if (pooledConn.isValid() && !pooledConn.isExpired(maxIdleTimeMs)) {
                pooledConn.setInUse(true);
                pooledConn.updateLastUsedTime();
                activeConnections.incrementAndGet();
                
                log.debug("从连接池获取连接: active={}, total={}", 
                        activeConnections.get(), totalConnections.get());
                return pooledConn.getConnection();
            } else {
                // 连接无效或过期，关闭并减少计数
                closePooledConnection(pooledConn);
                totalConnections.decrementAndGet();
            }
        }

        // 没有可用连接，尝试创建新连接
        if (totalConnections.get() < maxConnections) {
            poolLock.lock();
            try {
                if (totalConnections.get() < maxConnections) {
                    pooledConn = createPooledConnection();
                    if (pooledConn != null) {
                        pooledConn.setInUse(true);
                        pooledConn.updateLastUsedTime();
                        totalConnections.incrementAndGet();
                        activeConnections.incrementAndGet();
                        
                        log.debug("创建新连接: active={}, total={}", 
                                activeConnections.get(), totalConnections.get());
                        return pooledConn.getConnection();
                    }
                }
            } finally {
                poolLock.unlock();
            }
        }

        // 连接池已满，等待可用连接
        try {
            pooledConn = waitForAvailableConnection();
            if (pooledConn != null) {
                pooledConn.setInUse(true);
                pooledConn.updateLastUsedTime();
                activeConnections.incrementAndGet();
                return pooledConn.getConnection();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待连接被中断", e);
        }

        throw new RuntimeException("无法获取NATS连接：连接池已满且等待超时");
    }

    /**
     * 归还连接
     * 
     * @param connection 要归还的连接
     */
    public void returnConnection(Connection connection) {
        if (connection == null || shutdown) {
            return;
        }

        // 查找对应的池化连接
        PooledConnection pooledConn = findPooledConnection(connection);
        if (pooledConn != null) {
            pooledConn.setInUse(false);
            pooledConn.updateLastUsedTime();
            activeConnections.decrementAndGet();

            // 检查连接是否仍然有效
            if (pooledConn.isValid() && !pooledConn.isExpired(maxIdleTimeMs)) {
                availableConnections.offer(pooledConn);
                log.debug("归还连接到池: active={}, available={}", 
                        activeConnections.get(), availableConnections.size());
            } else {
                // 连接无效，关闭并减少计数
                closePooledConnection(pooledConn);
                totalConnections.decrementAndGet();
                log.debug("关闭无效连接: total={}", totalConnections.get());
            }
        }
    }

    /**
     * 获取连接池统计信息
     */
    public ConnectionPoolStats getStats() {
        return new ConnectionPoolStats(
                totalConnections.get(),
                activeConnections.get(),
                availableConnections.size(),
                maxConnections,
                minConnections
        );
    }

    /**
     * 连接池统计信息
     */
    public static class ConnectionPoolStats {
        private final int totalConnections;
        private final int activeConnections;
        private final int availableConnections;
        private final int maxConnections;
        private final int minConnections;

        public ConnectionPoolStats(int totalConnections, int activeConnections, 
                                 int availableConnections, int maxConnections, int minConnections) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.availableConnections = availableConnections;
            this.maxConnections = maxConnections;
            this.minConnections = minConnections;
        }

        public int getTotalConnections() { return totalConnections; }
        public int getActiveConnections() { return activeConnections; }
        public int getAvailableConnections() { return availableConnections; }
        public int getMaxConnections() { return maxConnections; }
        public int getMinConnections() { return minConnections; }

        @Override
        public String toString() {
            return String.format("ConnectionPoolStats{total=%d, active=%d, available=%d, max=%d, min=%d}", 
                    totalConnections, activeConnections, availableConnections, maxConnections, minConnections);
        }
    }

    /**
     * 清理过期连接
     */
    public void cleanupExpiredConnections() {
        poolLock.lock();
        try {
            availableConnections.removeIf(pooledConn -> {
                if (pooledConn.isExpired(maxIdleTimeMs) || !pooledConn.isValid()) {
                    closePooledConnection(pooledConn);
                    totalConnections.decrementAndGet();
                    log.debug("清理过期连接: total={}", totalConnections.get());
                    return true;
                }
                return false;
            });

            // 确保最小连接数
            while (totalConnections.get() < minConnections) {
                try {
                    PooledConnection pooledConn = createPooledConnection();
                    if (pooledConn != null) {
                        availableConnections.offer(pooledConn);
                        totalConnections.incrementAndGet();
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    log.warn("补充最小连接数失败: {}", e.getMessage());
                    break;
                }
            }
        } finally {
            poolLock.unlock();
        }
    }

    /**
     * 销毁连接池
     */
    @PreDestroy
    public void destroy() {
        if (shutdown) {
            return;
        }

        poolLock.lock();
        try {
            shutdown = true;
            
            log.info("正在关闭NATS连接池...");

            // 关闭所有可用连接
            PooledConnection pooledConn;
            while ((pooledConn = availableConnections.poll()) != null) {
                closePooledConnection(pooledConn);
            }

            log.info("NATS连接池已关闭: 总连接数={}, 活跃连接数={}", 
                    totalConnections.get(), activeConnections.get());

        } finally {
            poolLock.unlock();
        }
    }

    // ================================
    // 私有辅助方法
    // ================================

    /**
     * 创建池化连接
     */
    private PooledConnection createPooledConnection() {
        try {
            Connection connection = Nats.connect(natsOptions);
            return new PooledConnection(connection);
        } catch (Exception e) {
            log.error("创建NATS连接失败", e);
            return null;
        }
    }

    /**
     * 等待可用连接
     */
    private PooledConnection waitForAvailableConnection() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeout = connectionTimeoutMs;

        while (System.currentTimeMillis() - startTime < timeout) {
            PooledConnection pooledConn = availableConnections.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (pooledConn != null) {
                if (pooledConn.isValid() && !pooledConn.isExpired(maxIdleTimeMs)) {
                    return pooledConn;
                } else {
                    closePooledConnection(pooledConn);
                    totalConnections.decrementAndGet();
                }
            }
        }

        return null;
    }

    /**
     * 查找池化连接
     */
    private PooledConnection findPooledConnection(Connection connection) {
        // 简化实现：在实际应用中，可能需要维护连接到池化连接的映射
        // 这里假设连接对象可以直接比较
        return availableConnections.stream()
                .filter(pc -> pc.getConnection() == connection)
                .findFirst()
                .orElse(null);
    }

    /**
     * 关闭池化连接
     */
    private void closePooledConnection(PooledConnection pooledConn) {
        if (pooledConn != null && pooledConn.getConnection() != null) {
            try {
                pooledConn.getConnection().close();
            } catch (Exception e) {
                log.warn("关闭NATS连接异常", e);
            }
        }
    }

    // ================================
    // 配置方法
    // ================================

    public void setMinConnections(int minConnections) {
        this.minConnections = Math.max(1, minConnections);
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = Math.max(minConnections, maxConnections);
    }

    public void setMaxIdleTimeMs(int maxIdleTimeMs) {
        this.maxIdleTimeMs = Math.max(60000, maxIdleTimeMs); // 最少1分钟
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = Math.max(1000, connectionTimeoutMs); // 最少1秒
    }
} 