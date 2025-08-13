package com.acme.im.common.discovery.registry;

import com.acme.im.common.infrastructure.nats.publisher.EventPublisher;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务注册器
 * 负责服务的注册、发现和元数据管理
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class ServiceRegistry {

    private final EventPublisher eventPublisher;
    private final Gson gson;
    
    // 服务注册表
    private final Map<String, ServiceInfo> serviceRegistry = new ConcurrentHashMap<>();
    
    // 服务健康检查缓存
    private final Map<String, HealthStatus> healthCache = new ConcurrentHashMap<>();

    public ServiceRegistry(EventPublisher eventPublisher, Gson gson) {
        this.eventPublisher = eventPublisher;
        this.gson = gson;
    }

    /**
     * 注册服务
     * 
     * @param serviceInfo 服务信息
     * @return 是否注册成功
     */
    public boolean registerService(ServiceInfo serviceInfo) {
        try {
            if (serviceInfo == null || serviceInfo.getServiceId() == null) {
                log.warn("服务信息无效，无法注册");
                return false;
            }
            
            // 更新注册表
            serviceRegistry.put(serviceInfo.getServiceId(), serviceInfo);
            
            // 发布服务注册事件
            eventPublisher.publishEvent("im.service.registered", serviceInfo);
            
            log.info("服务注册成功: serviceId={}, serviceName={}, endpoint={}", 
                    serviceInfo.getServiceId(), serviceInfo.getServiceName(), serviceInfo.getEndpoint());
            return true;
            
        } catch (Exception e) {
            log.error("服务注册失败: serviceId={}", serviceInfo != null ? serviceInfo.getServiceId() : "null", e);
            return false;
        }
    }

    /**
     * 注销服务
     * 
     * @param serviceId 服务ID
     * @return 是否注销成功
     */
    public boolean unregisterService(String serviceId) {
        try {
            ServiceInfo serviceInfo = serviceRegistry.remove(serviceId);
            if (serviceInfo != null) {
                // 发布服务注销事件
                eventPublisher.publishEvent("im.service.unregistered", serviceInfo);
                
                log.info("服务注销成功: serviceId={}, serviceName={}", serviceId, serviceInfo.getServiceName());
                return true;
            } else {
                log.warn("服务不存在，无法注销: serviceId={}", serviceId);
                return false;
            }
        } catch (Exception e) {
            log.error("服务注销失败: serviceId={}", serviceId, e);
            return false;
        }
    }

    /**
     * 查找服务
     * 
     * @param serviceId 服务ID
     * @return 服务信息
     */
    public ServiceInfo findService(String serviceId) {
        return serviceRegistry.get(serviceId);
    }

    /**
     * 查找服务（按名称）
     * 
     * @param serviceName 服务名称
     * @return 服务信息列表
     */
    public java.util.List<ServiceInfo> findServicesByName(String serviceName) {
        return serviceRegistry.values().stream()
            .filter(service -> serviceName.equals(service.getServiceName()))
            .toList();
    }

    /**
     * 更新服务健康状态
     * 
     * @param serviceId 服务ID
     * @param healthStatus 健康状态
     */
    public void updateHealthStatus(String serviceId, HealthStatus healthStatus) {
        try {
            healthCache.put(serviceId, healthStatus);
            
            // 发布健康状态更新事件
            eventPublisher.publishEvent("im.service.health.updated", healthStatus);
            
            log.debug("服务健康状态更新: serviceId={}, status={}", serviceId, healthStatus.getStatus());
        } catch (Exception e) {
            log.error("更新服务健康状态失败: serviceId={}", serviceId, e);
        }
    }

    /**
     * 获取服务健康状态
     * 
     * @param serviceId 服务ID
     * @return 健康状态
     */
    public HealthStatus getHealthStatus(String serviceId) {
        return healthCache.get(serviceId);
    }

    /**
     * 获取所有服务
     * 
     * @return 服务信息列表
     */
    public java.util.List<ServiceInfo> getAllServices() {
        return serviceRegistry.values().stream().toList();
    }

    /**
     * 获取服务统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalServices", serviceRegistry.size());
        stats.put("healthyServices", healthCache.values().stream()
            .filter(h -> "UP".equals(h.getStatus())).count());
        stats.put("unhealthyServices", healthCache.values().stream()
            .filter(h -> !"UP".equals(h.getStatus())).count());
        return stats;
    }

    /**
     * 服务信息
     */
    @Data
    public static class ServiceInfo {
        private String serviceId;
        private String serviceName;
        private String version;
        private String endpoint;
        private String host;
        private int port;
        private String protocol;
        private Map<String, Object> metadata;
        private LocalDateTime registrationTime;
        private LocalDateTime lastUpdateTime;
        
        // 负载均衡相关字段
        private String instanceId;
        private String status = "UP";
        private boolean enabled = true;
        private int weight = 100;
        private String cluster;
        private long heartbeatInterval = 30;
        private String healthCheckUrl;
        private String[] tags;
    }

    /**
     * 健康状态
     */
    @Data
    public static class HealthStatus {
        private String serviceId;
        private String status; // UP, DOWN, UNKNOWN
        private String details;
        private LocalDateTime checkTime;
        private Map<String, Object> metrics;
    }
} 