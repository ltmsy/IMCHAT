package com.acme.im.communication.realtime;

import com.acme.im.common.websocket.constants.WebSocketConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.CloseStatus;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * WebSocket连接管理器
 * 负责WebSocket连接的生命周期管理
 * 
 * 主要功能：
 * 1. 连接注册和注销
 * 2. 连接状态维护
 * 3. 用户多设备连接管理
 * 4. 连接池管理和限流
 * 5. 心跳检测和超时处理
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketConnectionManager {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 本地连接缓存：connectionId -> WebSocketSession
     */
    private final ConcurrentMap<String, WebSocketSession> localConnections = new ConcurrentHashMap<>();

    /**
     * 用户连接映射：userId -> Set<connectionId>
     */
    private final ConcurrentMap<Long, Set<String>> userConnections = new ConcurrentHashMap<>();

    /**
     * 连接元数据：connectionId -> ConnectionMetadata
     */
    private final ConcurrentMap<String, ConnectionMetadata> connectionMetadata = new ConcurrentHashMap<>();

    /**
     * Redis键前缀
     */
    private static final String REDIS_KEY_PREFIX = "comm:conn:";
    private static final String USER_CONN_PREFIX = "comm:user:";
    
    /**
     * 连接信息过期时间（秒）
     */
    private static final long CONNECTION_EXPIRE_SECONDS = 300; // 5分钟

    /**
     * 注册WebSocket连接
     * 
     * @param session WebSocket会话
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @return 连接ID
     */
    public String registerConnection(WebSocketSession session, Long userId, String deviceId) {
        String connectionId = generateConnectionId(userId, deviceId);
        
        try {
            // 检查用户连接数限制
            if (!checkConnectionLimit(userId)) {
                log.warn("用户连接数超限: userId={}", userId);
                return null;
            }
            
            // 创建连接元数据
            ConnectionMetadata metadata = new ConnectionMetadata(
                connectionId, userId, deviceId, session.getId(), 
                LocalDateTime.now(), getClientInfo(session)
            );
            
            // 本地存储
            localConnections.put(connectionId, session);
            connectionMetadata.put(connectionId, metadata);
            
            // 更新用户连接映射
            userConnections.computeIfAbsent(userId, k -> Collections.synchronizedSet(new HashSet<>()))
                          .add(connectionId);
            
            // Redis存储（用于跨实例查询）
            storeConnectionInRedis(connectionId, metadata);
            updateUserConnectionsInRedis(userId);
            
            log.info("注册WebSocket连接: connectionId={}, userId={}, deviceId={}, sessionId={}", 
                    connectionId, userId, deviceId, session.getId());
            
            return connectionId;
            
        } catch (Exception e) {
            log.error("注册WebSocket连接失败: userId={}, deviceId={}", userId, deviceId, e);
            return null;
        }
    }

    /**
     * 注销WebSocket连接
     * 
     * @param connectionId 连接ID
     */
    public void unregisterConnection(String connectionId) {
        try {
            ConnectionMetadata metadata = connectionMetadata.get(connectionId);
            if (metadata == null) {
                log.debug("连接元数据不存在: connectionId={}", connectionId);
                return;
            }
            
            Long userId = metadata.getUserId();
            
            // 移除本地存储
            localConnections.remove(connectionId);
            connectionMetadata.remove(connectionId);
            
            // 更新用户连接映射
            Set<String> userConns = userConnections.get(userId);
            if (userConns != null) {
                userConns.remove(connectionId);
                if (userConns.isEmpty()) {
                    userConnections.remove(userId);
                }
            }
            
            // 移除Redis存储
            removeConnectionFromRedis(connectionId);
            updateUserConnectionsInRedis(userId);
            
            log.info("注销WebSocket连接: connectionId={}, userId={}, deviceId={}", 
                    connectionId, userId, metadata.getDeviceId());
            
        } catch (Exception e) {
            log.error("注销WebSocket连接失败: connectionId={}", connectionId, e);
        }
    }

    /**
     * 获取WebSocket会话
     * 
     * @param connectionId 连接ID
     * @return WebSocket会话
     */
    public WebSocketSession getSession(String connectionId) {
        return localConnections.get(connectionId);
    }

    /**
     * 获取用户的所有连接
     * 
     * @param userId 用户ID
     * @return 连接ID集合
     */
    public Set<String> getUserConnections(Long userId) {
        Set<String> connections = userConnections.get(userId);
        return connections != null ? new HashSet<>(connections) : new HashSet<>();
    }

    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        Set<String> connections = userConnections.get(userId);
        return connections != null && !connections.isEmpty();
    }

    /**
     * 获取在线用户数量
     * 
     * @return 在线用户数量
     */
    public int getOnlineUserCount() {
        return userConnections.size();
    }

    /**
     * 获取总连接数
     * 
     * @return 总连接数
     */
    public int getTotalConnectionCount() {
        return localConnections.size();
    }

    /**
     * 更新连接心跳时间
     * 
     * @param connectionId 连接ID
     */
    public void updateHeartbeat(String connectionId) {
        ConnectionMetadata metadata = connectionMetadata.get(connectionId);
        if (metadata != null) {
            metadata.setLastHeartbeatAt(LocalDateTime.now());
            
            // 更新Redis中的心跳时间
            String redisKey = REDIS_KEY_PREFIX + connectionId + ":heartbeat";
            redisTemplate.opsForValue().set(redisKey, System.currentTimeMillis(), 
                                          CONNECTION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * 检查连接是否超时
     * 
     * @param connectionId 连接ID
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否超时
     */
    public boolean isConnectionTimeout(String connectionId, long timeoutSeconds) {
        ConnectionMetadata metadata = connectionMetadata.get(connectionId);
        if (metadata == null) {
            return true;
        }
        
        LocalDateTime lastHeartbeat = metadata.getLastHeartbeatAt();
        if (lastHeartbeat == null) {
            lastHeartbeat = metadata.getConnectedAt();
        }
        
        return lastHeartbeat.isBefore(LocalDateTime.now().minusSeconds(timeoutSeconds));
    }

    /**
     * 清理超时连接
     * 
     * @param timeoutSeconds 超时时间（秒）
     * @return 清理的连接数
     */
    public int cleanTimeoutConnections(long timeoutSeconds) {
        int cleanedCount = 0;
        
        for (String connectionId : new HashSet<>(connectionMetadata.keySet())) {
            if (isConnectionTimeout(connectionId, timeoutSeconds)) {
                WebSocketSession session = localConnections.get(connectionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.close(CloseStatus.NORMAL)
                               .subscribe();
                    } catch (Exception e) {
                        log.error("关闭超时连接失败: connectionId={}", connectionId, e);
                    }
                }
                
                unregisterConnection(connectionId);
                cleanedCount++;
            }
        }
        
        if (cleanedCount > 0) {
            log.info("清理超时连接: count={}, timeoutSeconds={}", cleanedCount, timeoutSeconds);
        }
        
        return cleanedCount;
    }

    /**
     * 强制断开用户的所有连接
     * 
     * @param userId 用户ID
     * @param reason 断开原因
     */
    public void disconnectUser(Long userId, String reason) {
        Set<String> connections = getUserConnections(userId);
        
        for (String connectionId : connections) {
            WebSocketSession session = localConnections.get(connectionId);
            if (session != null && session.isOpen()) {
                try {
                    session.close(CloseStatus.POLICY_VIOLATION)
                           .subscribe();
                } catch (Exception e) {
                    log.error("强制断开连接失败: connectionId={}", connectionId, e);
                }
            }
            
            unregisterConnection(connectionId);
        }
        
        log.info("强制断开用户连接: userId={}, reason={}, connectionCount={}", 
                userId, reason, connections.size());
    }

    // ================================
    // 私有方法
    // ================================

    /**
     * 生成连接ID
     */
    private String generateConnectionId(Long userId, String deviceId) {
        return userId + "_" + deviceId + "_" + System.currentTimeMillis();
    }

    /**
     * 检查连接数限制
     */
    private boolean checkConnectionLimit(Long userId) {
        Set<String> connections = userConnections.get(userId);
        int currentCount = connections != null ? connections.size() : 0;
        return currentCount < WebSocketConstants.Limit.MAX_CONNECTIONS;
    }

    /**
     * 获取客户端信息
     */
    private String getClientInfo(WebSocketSession session) {
        try {
            return session.getHandshakeInfo().getHeaders().getFirst("User-Agent");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * 在Redis中存储连接信息
     */
    private void storeConnectionInRedis(String connectionId, ConnectionMetadata metadata) {
        try {
            String redisKey = REDIS_KEY_PREFIX + connectionId;
            redisTemplate.opsForHash().putAll(redisKey, metadata.toMap());
            redisTemplate.expire(redisKey, CONNECTION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("存储连接信息到Redis失败: connectionId={}", connectionId, e);
        }
    }

    /**
     * 从Redis中移除连接信息
     */
    private void removeConnectionFromRedis(String connectionId) {
        try {
            String redisKey = REDIS_KEY_PREFIX + connectionId;
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("从Redis移除连接信息失败: connectionId={}", connectionId, e);
        }
    }

    /**
     * 更新用户连接列表到Redis
     */
    private void updateUserConnectionsInRedis(Long userId) {
        try {
            String redisKey = USER_CONN_PREFIX + userId;
            Set<String> connections = userConnections.get(userId);
            
            if (connections == null || connections.isEmpty()) {
                redisTemplate.delete(redisKey);
            } else {
                redisTemplate.opsForSet().getOperations().delete(redisKey);
                redisTemplate.opsForSet().add(redisKey, connections.toArray(new String[0]));
                redisTemplate.expire(redisKey, CONNECTION_EXPIRE_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("更新用户连接列表到Redis失败: userId={}", userId, e);
        }
    }

    /**
     * 连接元数据内部类
     */
    private static class ConnectionMetadata {
        private final String connectionId;
        private final Long userId;
        private final String deviceId;
        private final String sessionId;
        private final LocalDateTime connectedAt;
        private final String clientInfo;
        private volatile LocalDateTime lastHeartbeatAt;

        public ConnectionMetadata(String connectionId, Long userId, String deviceId, 
                                String sessionId, LocalDateTime connectedAt, String clientInfo) {
            this.connectionId = connectionId;
            this.userId = userId;
            this.deviceId = deviceId;
            this.sessionId = sessionId;
            this.connectedAt = connectedAt;
            this.clientInfo = clientInfo;
            this.lastHeartbeatAt = connectedAt;
        }

        // Getters and setters
        public String getConnectionId() { return connectionId; }
        public Long getUserId() { return userId; }
        public String getDeviceId() { return deviceId; }
        public String getSessionId() { return sessionId; }
        public LocalDateTime getConnectedAt() { return connectedAt; }
        public String getClientInfo() { return clientInfo; }
        public LocalDateTime getLastHeartbeatAt() { return lastHeartbeatAt; }
        public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }

        public java.util.Map<String, Object> toMap() {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("connectionId", connectionId);
            map.put("userId", userId);
            map.put("deviceId", deviceId);
            map.put("sessionId", sessionId);
            map.put("connectedAt", connectedAt.toString());
            map.put("clientInfo", clientInfo);
            map.put("lastHeartbeatAt", lastHeartbeatAt.toString());
            return map;
        }
    }
} 