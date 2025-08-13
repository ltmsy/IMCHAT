package com.acme.im.common.loadbalancer.strategy;

import com.acme.im.common.discovery.registry.ServiceRegistry.ServiceInfo;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 负载均衡策略接口
 * 定义负载均衡的基本行为
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public interface LoadBalancerStrategy {

    /**
     * 选择服务实例
     * 
     * @param serviceInstances 服务实例列表
     * @return 选择的服务实例
     */
    ServiceInfo select(List<ServiceInfo> serviceInstances);

    /**
     * 选择服务实例（带请求上下文）
     * 
     * @param serviceInstances 服务实例列表
     * @param requestContext 请求上下文
     * @return 选择的服务实例
     */
    ServiceInfo select(List<ServiceInfo> serviceInstances, RequestContext requestContext);

    /**
     * 获取策略名称
     * 
     * @return 策略名称
     */
    String getStrategyName();

    /**
     * 获取策略描述
     * 
     * @return 策略描述
     */
    String getStrategyDescription();

    /**
     * 检查策略是否可用
     * 
     * @param serviceInstances 服务实例列表
     * @return 是否可用
     */
    boolean isAvailable(List<ServiceInfo> serviceInstances);

    /**
     * 获取策略权重
     * 
     * @return 策略权重
     */
    int getWeight();

    /**
     * 设置策略权重
     * 
     * @param weight 策略权重
     */
    void setWeight(int weight);

    /**
     * 获取策略统计信息
     * 
     * @return 统计信息
     */
    StrategyStatistics getStatistics();

    /**
     * 重置策略统计信息
     */
    void resetStatistics();

    /**
     * 请求上下文
     */
    class RequestContext {
        private String userId;
        private String sessionId;
        private String requestId;
        private String clientIp;
        private String userAgent;
        private String requestPath;
        private String requestMethod;
        private long requestTime;
        private Map<String, Object> attributes;

        public RequestContext() {
            this.requestTime = System.currentTimeMillis();
            this.attributes = new HashMap<>();
        }

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }

        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public String getRequestPath() { return requestPath; }
        public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

        public String getRequestMethod() { return requestMethod; }
        public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }

        public long getRequestTime() { return requestTime; }
        public void setRequestTime(long requestTime) { this.requestTime = requestTime; }

        public Map<String, Object> getAttributes() { return attributes; }
        public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }

        public Object getAttribute(String key) { return attributes.get(key); }
        public void setAttribute(String key, Object value) { attributes.put(key, value); }

        public boolean hasAttribute(String key) { return attributes.containsKey(key); }
        public void removeAttribute(String key) { attributes.remove(key); }

        public void clearAttributes() { attributes.clear(); }

        @Override
        public String toString() {
            return String.format("RequestContext{userId='%s', sessionId='%s', requestId='%s', clientIp='%s', requestPath='%s', requestMethod='%s'}", 
                    userId, sessionId, requestId, clientIp, requestPath, requestMethod);
        }
    }

    /**
     * 策略统计信息
     */
    class StrategyStatistics {
        private long totalRequests;
        private long successfulRequests;
        private long failedRequests;
        private long totalResponseTime;
        private long minResponseTime;
        private long maxResponseTime;
        private Map<String, Long> instanceSelectionCounts;
        private long lastResetTime;

        public StrategyStatistics() {
            this.lastResetTime = System.currentTimeMillis();
            this.instanceSelectionCounts = new HashMap<>();
            reset();
        }

        public void incrementTotalRequests() { totalRequests++; }
        public void incrementSuccessfulRequests() { successfulRequests++; }
        public void incrementFailedRequests() { failedRequests++; }

        public void addResponseTime(long responseTime) {
            totalResponseTime += responseTime;
            if (minResponseTime == 0 || responseTime < minResponseTime) {
                minResponseTime = responseTime;
            }
            if (responseTime > maxResponseTime) {
                maxResponseTime = responseTime;
            }
        }

        public void incrementInstanceSelection(String instanceId) {
            instanceSelectionCounts.merge(instanceId, 1L, Long::sum);
        }

        public void reset() {
            totalRequests = 0;
            successfulRequests = 0;
            failedRequests = 0;
            totalResponseTime = 0;
            minResponseTime = 0;
            maxResponseTime = 0;
            instanceSelectionCounts.clear();
            lastResetTime = System.currentTimeMillis();
        }

        // Getters
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
        public long getTotalResponseTime() { return totalResponseTime; }
        public long getMinResponseTime() { return minResponseTime; }
        public long getMaxResponseTime() { return maxResponseTime; }
        public Map<String, Long> getInstanceSelectionCounts() { return instanceSelectionCounts; }
        public long getLastResetTime() { return lastResetTime; }

        public double getSuccessRate() {
            return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
        }

        public double getFailureRate() {
            return totalRequests > 0 ? (double) failedRequests / totalRequests : 0.0;
        }

        public long getAverageResponseTime() {
            return totalRequests > 0 ? totalResponseTime / totalRequests : 0;
        }

        public String getMostSelectedInstance() {
            return instanceSelectionCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("none");
        }

        public long getInstanceSelectionCount(String instanceId) {
            return instanceSelectionCounts.getOrDefault(instanceId, 0L);
        }

        @Override
        public String toString() {
            return String.format("StrategyStatistics{totalRequests=%d, successRate=%.2f%%, avgResponseTime=%dms, mostSelectedInstance='%s'}", 
                    totalRequests, getSuccessRate() * 100, getAverageResponseTime(), getMostSelectedInstance());
        }
    }
} 