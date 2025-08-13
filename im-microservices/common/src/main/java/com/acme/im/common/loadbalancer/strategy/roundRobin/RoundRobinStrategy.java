package com.acme.im.common.loadbalancer.strategy.roundRobin;

import com.acme.im.common.discovery.registry.ServiceRegistry.ServiceInfo;
import com.acme.im.common.loadbalancer.strategy.LoadBalancerStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 轮询负载均衡策略实现
 * 支持权重轮询和会话亲和性
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class RoundRobinStrategy implements LoadBalancerStrategy {

    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final AtomicLong requestCounter = new AtomicLong(0);
    private int weight = 100;
    private final StrategyStatistics statistics = new StrategyStatistics();

    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInstances) {
        return select(serviceInstances, null);
    }

    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInstances, RequestContext requestContext) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 检查策略是否可用
            if (!isAvailable(serviceInstances)) {
                log.warn("轮询策略不可用: 服务实例数量={}", serviceInstances.size());
                return null;
            }

            // 过滤可用的服务实例
            List<ServiceInfo> availableInstances = serviceInstances.stream()
                    .filter(instance -> "UP".equals(instance.getStatus()) && instance.isEnabled())
                    .collect(Collectors.toList());

            if (availableInstances.isEmpty()) {
                log.warn("没有可用的服务实例");
                return null;
            }

            ServiceInfo selectedInstance;
            
            // 如果启用了会话亲和性，尝试保持会话
            if (requestContext != null && requestContext.getSessionId() != null) {
                selectedInstance = selectWithSessionAffinity(availableInstances, requestContext);
            } else {
                // 普通轮询选择
                selectedInstance = selectWithRoundRobin(availableInstances);
            }

            if (selectedInstance != null) {
                // 更新统计信息
                statistics.incrementTotalRequests();
                statistics.incrementInstanceSelection(selectedInstance.getInstanceId());
                
                log.debug("轮询策略选择实例成功: instanceId={}, serviceName={}, host={}:{}", 
                        selectedInstance.getInstanceId(), 
                        selectedInstance.getServiceName(),
                        selectedInstance.getHost(), 
                        selectedInstance.getPort());
            }

            return selectedInstance;
        } catch (Exception e) {
            log.error("轮询策略选择实例异常", e);
            statistics.incrementFailedRequests();
            return null;
        } finally {
            // 记录响应时间
            long responseTime = System.currentTimeMillis() - startTime;
            statistics.addResponseTime(responseTime);
        }
    }

    /**
     * 带会话亲和性的选择
     */
    private ServiceInfo selectWithSessionAffinity(List<ServiceInfo> availableInstances, RequestContext requestContext) {
        String sessionId = requestContext.getSessionId();
        int hash = Math.abs(sessionId.hashCode());
        int index = hash % availableInstances.size();
        
        ServiceInfo selectedInstance = availableInstances.get(index);
        log.debug("会话亲和性选择: sessionId={}, index={}, instanceId={}", 
                sessionId, index, selectedInstance.getInstanceId());
        
        return selectedInstance;
    }

    /**
     * 轮询选择
     */
    private ServiceInfo selectWithRoundRobin(List<ServiceInfo> availableInstances) {
        int size = availableInstances.size();
        if (size == 0) {
            return null;
        }

        // 获取当前索引
        int current = currentIndex.getAndIncrement();
        
        // 处理索引溢出
        if (current < 0) {
            currentIndex.set(0);
            current = 0;
        }
        
        // 计算实际索引
        int index = current % size;
        
        // 获取选中的实例
        ServiceInfo selectedInstance = availableInstances.get(index);
        
        // 增加请求计数器
        requestCounter.incrementAndGet();
        
        log.debug("轮询选择: current={}, index={}, size={}, instanceId={}", 
                current, index, size, selectedInstance.getInstanceId());
        
        return selectedInstance;
    }

    /**
     * 权重轮询选择
     */
    private ServiceInfo selectWithWeightedRoundRobin(List<ServiceInfo> availableInstances) {
        if (availableInstances.isEmpty()) {
            return null;
        }

        // 计算总权重
        int totalWeight = availableInstances.stream()
                .mapToInt(ServiceInfo::getWeight)
                .sum();

        if (totalWeight <= 0) {
            // 如果总权重为0，使用普通轮询
            return selectWithRoundRobin(availableInstances);
        }

        // 获取当前权重计数器
        long currentWeight = requestCounter.getAndIncrement();
        
        // 计算权重索引
        int weightIndex = (int) (currentWeight % totalWeight);
        
        // 根据权重选择实例
        int currentWeightSum = 0;
        for (ServiceInfo instance : availableInstances) {
            currentWeightSum += instance.getWeight();
            if (weightIndex < currentWeightSum) {
                log.debug("权重轮询选择: weightIndex={}, totalWeight={}, selectedWeight={}, instanceId={}", 
                        weightIndex, totalWeight, instance.getWeight(), instance.getInstanceId());
                return instance;
            }
        }

        // 如果权重计算出现问题，返回第一个实例
        log.warn("权重轮询计算异常，返回第一个实例");
        return availableInstances.get(0);
    }

    /**
     * 平滑权重轮询选择
     */
    private ServiceInfo selectWithSmoothWeightedRoundRobin(List<ServiceInfo> availableInstances) {
        if (availableInstances.isEmpty()) {
            return null;
        }

        // 找到权重最大的实例
        ServiceInfo maxWeightInstance = availableInstances.stream()
                .max((a, b) -> Integer.compare(a.getWeight(), b.getWeight()))
                .orElse(null);

        if (maxWeightInstance == null) {
            return null;
        }

        // 计算动态权重
        int maxWeight = maxWeightInstance.getWeight();
        int totalWeight = availableInstances.stream()
                .mapToInt(ServiceInfo::getWeight)
                .sum();

        // 使用平滑权重算法
        long currentWeight = requestCounter.getAndIncrement();
        int smoothIndex = (int) (currentWeight % (maxWeight * availableInstances.size()));
        
        int instanceIndex = smoothIndex / maxWeight;
        if (instanceIndex >= availableInstances.size()) {
            instanceIndex = availableInstances.size() - 1;
        }

        ServiceInfo selectedInstance = availableInstances.get(instanceIndex);
        
        log.debug("平滑权重轮询选择: smoothIndex={}, instanceIndex={}, instanceId={}", 
                smoothIndex, instanceIndex, selectedInstance.getInstanceId());
        
        return selectedInstance;
    }

    @Override
    public String getStrategyName() {
        return "RoundRobin";
    }

    @Override
    public String getStrategyDescription() {
        return "轮询负载均衡策略，支持权重轮询和会话亲和性";
    }

    @Override
    public boolean isAvailable(List<ServiceInfo> serviceInstances) {
        if (serviceInstances == null || serviceInstances.isEmpty()) {
            return false;
        }

        // 检查是否有可用的服务实例
        long availableCount = serviceInstances.stream()
                .filter(instance -> "UP".equals(instance.getStatus()) && instance.isEnabled())
                .count();

        return availableCount > 0;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public void setWeight(int weight) {
        this.weight = Math.max(1, weight);
        log.info("轮询策略权重已更新: {}", this.weight);
    }

    @Override
    public StrategyStatistics getStatistics() {
        return statistics;
    }

    @Override
    public void resetStatistics() {
        statistics.reset();
        currentIndex.set(0);
        requestCounter.set(0);
        log.info("轮询策略统计信息已重置");
    }

    /**
     * 获取当前索引
     */
    public int getCurrentIndex() {
        return currentIndex.get();
    }

    /**
     * 设置当前索引
     */
    public void setCurrentIndex(int index) {
        currentIndex.set(Math.max(0, index));
        log.debug("轮询策略当前索引已设置: {}", currentIndex.get());
    }

    /**
     * 获取请求计数器
     */
    public long getRequestCounter() {
        return requestCounter.get();
    }

    /**
     * 重置轮询状态
     */
    public void resetRoundRobin() {
        currentIndex.set(0);
        requestCounter.set(0);
        log.info("轮询策略状态已重置");
    }

    /**
     * 获取策略详细信息
     */
    public String getStrategyDetails() {
        return String.format("RoundRobinStrategy{currentIndex=%d, requestCounter=%d, weight=%d, statistics=%s}", 
                currentIndex.get(), requestCounter.get(), weight, statistics);
    }
} 