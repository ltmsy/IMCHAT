package com.acme.im.common.utils.monitoring;

import com.acme.im.common.infrastructure.nats.publisher.EventPublisher;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 监控管理器
 * 支持指标收集、健康检查、性能监控等功能
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class MonitoringManager {

    @Autowired
    private EventPublisher eventPublisher;

    // 监控指标缓存
    private final Map<String, Metric> metricCache = new ConcurrentHashMap<>();
    private final Map<String, HealthCheck> healthCheckCache = new ConcurrentHashMap<>();
    
    // 性能监控
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 监控指标
     */
    @Data
    public static class Metric {
        private String name;
        private String type;
        private Object value;
        private String unit;
        private long timestamp;
        private String[] tags;
        private Map<String, Object> attributes;
    }

    /**
     * 健康检查
     */
    @Data
    public static class HealthCheck {
        private String name;
        private String status; // UP, DOWN, UNKNOWN
        private String message;
        private long timestamp;
        private long checkTime;
        private Map<String, Object> details;
    }

    /**
     * 性能指标
     */
    @Data
    public static class PerformanceMetrics {
        private long totalRequests;
        private long successfulRequests;
        private long failedRequests;
        private double successRate;
        private long averageResponseTime;
        private long minResponseTime;
        private long maxResponseTime;
        private long currentActiveRequests;
        private double requestsPerSecond;
        private long timestamp;
    }

    /**
     * 系统资源指标
     */
    @Data
    public static class SystemMetrics {
        private double cpuUsage;
        private long memoryUsage;
        private long memoryMax;
        private double memoryUsagePercentage;
        private int threadCount;
        private int activeThreadCount;
        private long uptime;
        private long timestamp;
    }

    /**
     * 监控事件
     */
    @Data
    public static class MonitoringEvent {
        private String eventType;
        private String metricName;
        private Object metricValue;
        private String status;
        private long timestamp;
        private Map<String, Object> metadata;
    }

    /**
     * 初始化监控管理器
     */
    public void initialize() {
        try {
            // 启动监控任务
            startMonitoringTasks();
            
            // 注册默认健康检查
            registerDefaultHealthChecks();
            
            log.info("监控管理器初始化成功");
        } catch (Exception e) {
            log.error("监控管理器初始化失败", e);
        }
    }

    /**
     * 记录请求指标
     * 
     * @param success 是否成功
     * @param responseTime 响应时间
     */
    public void recordRequest(boolean success, long responseTime) {
        totalRequests.incrementAndGet();
        
        if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        
        totalResponseTime.addAndGet(responseTime);
        
        // 更新指标缓存
        updateMetric("requests.total", totalRequests.get());
        updateMetric("requests.successful", successfulRequests.get());
        updateMetric("requests.failed", failedRequests.get());
        updateMetric("response.time.total", totalResponseTime.get());
        updateMetric("response.time.average", 
                totalRequests.get() > 0 ? totalResponseTime.get() / totalRequests.get() : 0);
    }

    /**
     * 更新指标
     * 
     * @param name 指标名称
     * @param value 指标值
     */
    public void updateMetric(String name, Object value) {
        updateMetric(name, value, null);
    }

    /**
     * 更新指标（带标签）
     * 
     * @param name 指标名称
     * @param value 指标值
     * @param tags 标签
     */
    public void updateMetric(String name, Object value, String[] tags) {
        try {
            Metric metric = new Metric();
            metric.setName(name);
            metric.setType(determineMetricType(value));
            metric.setValue(value);
            metric.setUnit(determineMetricUnit(name));
            metric.setTimestamp(System.currentTimeMillis());
            metric.setTags(tags != null ? tags : new String[0]);
            metric.setAttributes(new ConcurrentHashMap<>());

            metricCache.put(name, metric);
            
            // 发布指标更新事件
            publishMetricEvent("METRIC_UPDATED", name, value);
            
            log.debug("指标更新成功: name={}, value={}", name, value);
        } catch (Exception e) {
            log.error("指标更新失败: name={}, value={}", name, value, e);
        }
    }

    /**
     * 获取指标
     * 
     * @param name 指标名称
     * @return 指标
     */
    public Metric getMetric(String name) {
        return metricCache.get(name);
    }

    /**
     * 注册健康检查
     * 
     * @param name 健康检查名称
     * @param healthCheck 健康检查
     */
    public void registerHealthCheck(String name, HealthCheck healthCheck) {
        try {
            healthCheckCache.put(name, healthCheck);
            log.info("健康检查注册成功: name={}", name);
        } catch (Exception e) {
            log.error("健康检查注册失败: name={}", name, e);
        }
    }

    /**
     * 执行健康检查
     * 
     * @param name 健康检查名称
     * @return 健康检查结果
     */
    public HealthCheck executeHealthCheck(String name) {
        try {
            HealthCheck healthCheck = healthCheckCache.get(name);
            if (healthCheck == null) {
                log.warn("健康检查不存在: name={}", name);
                return null;
            }

            long startTime = System.currentTimeMillis();
            
            // 执行具体的健康检查逻辑
            boolean isHealthy = performHealthCheck(name);
            
            healthCheck.setStatus(isHealthy ? "UP" : "DOWN");
            healthCheck.setMessage(isHealthy ? "服务健康" : "服务异常");
            healthCheck.setTimestamp(System.currentTimeMillis());
            healthCheck.setCheckTime(System.currentTimeMillis() - startTime);
            
            // 发布健康检查事件
            publishHealthCheckEvent(name, healthCheck.getStatus());
            
            log.debug("健康检查执行完成: name={}, status={}, checkTime={}ms", 
                    name, healthCheck.getStatus(), healthCheck.getCheckTime());
            
            return healthCheck;
        } catch (Exception e) {
            log.error("健康检查执行失败: name={}", name, e);
            
            HealthCheck failedCheck = new HealthCheck();
            failedCheck.setName(name);
            failedCheck.setStatus("UNKNOWN");
            failedCheck.setMessage("健康检查执行异常: " + e.getMessage());
            failedCheck.setTimestamp(System.currentTimeMillis());
            failedCheck.setCheckTime(0);
            failedCheck.setDetails(new ConcurrentHashMap<>());
            
            return failedCheck;
        }
    }

    /**
     * 执行所有健康检查
     * 
     * @return 健康检查结果映射
     */
    public Map<String, HealthCheck> executeAllHealthChecks() {
        Map<String, HealthCheck> results = new ConcurrentHashMap<>();
        
        for (String name : healthCheckCache.keySet()) {
            HealthCheck result = executeHealthCheck(name);
            if (result != null) {
                results.put(name, result);
            }
        }
        
        return results;
    }

    /**
     * 获取性能指标
     * 
     * @return 性能指标
     */
    public PerformanceMetrics getPerformanceMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setTotalRequests(totalRequests.get());
        metrics.setSuccessfulRequests(successfulRequests.get());
        metrics.setFailedRequests(failedRequests.get());
        metrics.setSuccessRate(totalRequests.get() > 0 ? 
                (double) successfulRequests.get() / totalRequests.get() : 0.0);
        metrics.setAverageResponseTime(totalRequests.get() > 0 ? 
                totalResponseTime.get() / totalRequests.get() : 0);
        metrics.setTimestamp(System.currentTimeMillis());
        
        return metrics;
    }

    /**
     * 获取系统资源指标
     * 
     * @return 系统资源指标
     */
    public SystemMetrics getSystemMetrics() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            
            SystemMetrics metrics = new SystemMetrics();
            metrics.setCpuUsage(osBean.getSystemLoadAverage());
            metrics.setMemoryUsage(memoryBean.getHeapMemoryUsage().getUsed());
            metrics.setMemoryMax(memoryBean.getHeapMemoryUsage().getMax());
            metrics.setMemoryUsagePercentage(memoryBean.getHeapMemoryUsage().getMax() > 0 ? 
                    (double) memoryBean.getHeapMemoryUsage().getUsed() / memoryBean.getHeapMemoryUsage().getMax() : 0.0);
            metrics.setThreadCount(threadBean.getThreadCount());
            metrics.setActiveThreadCount(threadBean.getThreadCount());
            metrics.setUptime(ManagementFactory.getRuntimeMXBean().getUptime());
            metrics.setTimestamp(System.currentTimeMillis());
            
            return metrics;
        } catch (Exception e) {
            log.error("获取系统资源指标失败", e);
            return new SystemMetrics();
        }
    }

    /**
     * 启动监控任务
     */
    private void startMonitoringTasks() {
        // 系统指标收集任务（每30秒）
        scheduler.scheduleAtFixedRate(() -> {
            try {
                collectSystemMetrics();
            } catch (Exception e) {
                log.error("系统指标收集任务执行异常", e);
            }
        }, 0, 30, TimeUnit.SECONDS);
        
        // 性能指标收集任务（每10秒）
        scheduler.scheduleAtFixedRate(() -> {
            try {
                collectPerformanceMetrics();
            } catch (Exception e) {
                log.error("性能指标收集任务执行异常", e);
            }
        }, 0, 10, TimeUnit.SECONDS);
        
        // 健康检查任务（每60秒）
        scheduler.scheduleAtFixedRate(() -> {
            try {
                executeAllHealthChecks();
            } catch (Exception e) {
                log.error("健康检查任务执行异常", e);
            }
        }, 0, 60, TimeUnit.SECONDS);
        
        log.info("监控任务已启动");
    }

    /**
     * 注册默认健康检查
     */
    private void registerDefaultHealthChecks() {
        // 系统健康检查
        HealthCheck systemHealth = new HealthCheck();
        systemHealth.setName("system");
        systemHealth.setStatus("UP");
        systemHealth.setMessage("系统运行正常");
        systemHealth.setTimestamp(System.currentTimeMillis());
        systemHealth.setDetails(new ConcurrentHashMap<>());
        registerHealthCheck("system", systemHealth);
        
        // 内存健康检查
        HealthCheck memoryHealth = new HealthCheck();
        memoryHealth.setName("memory");
        memoryHealth.setStatus("UP");
        memoryHealth.setMessage("内存使用正常");
        memoryHealth.setTimestamp(System.currentTimeMillis());
        memoryHealth.setDetails(new ConcurrentHashMap<>());
        registerHealthCheck("memory", memoryHealth);
        
        // 线程健康检查
        HealthCheck threadHealth = new HealthCheck();
        threadHealth.setName("thread");
        threadHealth.setStatus("UP");
        threadHealth.setMessage("线程状态正常");
        threadHealth.setTimestamp(System.currentTimeMillis());
        threadHealth.setDetails(new ConcurrentHashMap<>());
        registerHealthCheck("thread", threadHealth);
    }

    /**
     * 执行具体的健康检查
     * 
     * @param name 健康检查名称
     * @return 是否健康
     */
    private boolean performHealthCheck(String name) {
        try {
            switch (name) {
                case "system":
                    return checkSystemHealth();
                case "memory":
                    return checkMemoryHealth();
                case "thread":
                    return checkThreadHealth();
                default:
                    return true;
            }
        } catch (Exception e) {
            log.error("健康检查执行异常: name={}", name, e);
            return false;
        }
    }

    /**
     * 检查系统健康状态
     */
    private boolean checkSystemHealth() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double loadAverage = osBean.getSystemLoadAverage();
            
            // 系统负载超过80%认为不健康
            return loadAverage < 0.8;
        } catch (Exception e) {
            log.error("系统健康检查异常", e);
            return false;
        }
    }

    /**
     * 检查内存健康状态
     */
    private boolean checkMemoryHealth() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long used = memoryBean.getHeapMemoryUsage().getUsed();
            long max = memoryBean.getHeapMemoryUsage().getMax();
            
            // 内存使用率超过90%认为不健康
            return max > 0 && (double) used / max < 0.9;
        } catch (Exception e) {
            log.error("内存健康检查异常", e);
            return false;
        }
    }

    /**
     * 检查线程健康状态
     */
    private boolean checkThreadHealth() {
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            int threadCount = threadBean.getThreadCount();
            
            // 线程数超过1000认为不健康
            return threadCount < 1000;
        } catch (Exception e) {
            log.error("线程健康检查异常", e);
            return false;
        }
    }

    /**
     * 收集系统指标
     */
    private void collectSystemMetrics() {
        try {
            SystemMetrics metrics = getSystemMetrics();
            
            updateMetric("system.cpu.usage", metrics.getCpuUsage());
            updateMetric("system.memory.usage", metrics.getMemoryUsage());
            updateMetric("system.memory.max", metrics.getMemoryMax());
            updateMetric("system.memory.usage.percentage", metrics.getMemoryUsagePercentage());
            updateMetric("system.thread.count", metrics.getThreadCount());
            updateMetric("system.uptime", metrics.getUptime());
            
            log.debug("系统指标收集完成");
        } catch (Exception e) {
            log.error("系统指标收集异常", e);
        }
    }

    /**
     * 收集性能指标
     */
    private void collectPerformanceMetrics() {
        try {
            PerformanceMetrics metrics = getPerformanceMetrics();
            
            updateMetric("performance.requests.total", metrics.getTotalRequests());
            updateMetric("performance.requests.successful", metrics.getSuccessfulRequests());
            updateMetric("performance.requests.failed", metrics.getFailedRequests());
            updateMetric("performance.success.rate", metrics.getSuccessRate());
            updateMetric("performance.response.time.average", metrics.getAverageResponseTime());
            
            log.debug("性能指标收集完成");
        } catch (Exception e) {
            log.error("性能指标收集异常", e);
        }
    }

    /**
     * 确定指标类型
     */
    private String determineMetricType(Object value) {
        if (value instanceof Number) {
            return "NUMBER";
        } else if (value instanceof String) {
            return "STRING";
        } else if (value instanceof Boolean) {
            return "BOOLEAN";
        } else {
            return "OBJECT";
        }
    }

    /**
     * 确定指标单位
     */
    private String determineMetricUnit(String metricName) {
        if (metricName.contains("time") || metricName.contains("duration")) {
            return "ms";
        } else if (metricName.contains("memory")) {
            return "bytes";
        } else if (metricName.contains("cpu")) {
            return "%";
        } else if (metricName.contains("rate")) {
            return "per_second";
        } else if (metricName.contains("count")) {
            return "count";
        } else {
            return "unknown";
        }
    }

    /**
     * 发布指标事件
     */
    private void publishMetricEvent(String eventType, String metricName, Object metricValue) {
        try {
            MonitoringEvent event = new MonitoringEvent();
            event.setEventType(eventType);
            event.setMetricName(metricName);
            event.setMetricValue(metricValue);
            event.setTimestamp(System.currentTimeMillis());
            event.setMetadata(new ConcurrentHashMap<>());

            String subject = "im.monitoring.metric";
            eventPublisher.publishEvent(subject, event);
            log.debug("指标事件发布: eventType={}, metricName={}", eventType, metricName);
        } catch (Exception e) {
            log.error("发布指标事件异常: eventType={}, metricName={}", eventType, metricName, e);
        }
    }

    /**
     * 发布健康检查事件
     */
    private void publishHealthCheckEvent(String checkName, String status) {
        try {
            MonitoringEvent event = new MonitoringEvent();
            event.setEventType("HEALTH_CHECK");
            event.setMetricName(checkName);
            event.setStatus(status);
            event.setTimestamp(System.currentTimeMillis());
            event.setMetadata(new ConcurrentHashMap<>());

            String subject = "im.monitoring.health";
            eventPublisher.publishEvent(subject, event);
            log.debug("健康检查事件发布: checkName={}, status={}", checkName, status);
        } catch (Exception e) {
            log.error("发布健康检查事件异常: checkName={}, status={}", checkName, status, e);
        }
    }

    /**
     * 获取监控统计信息
     */
    public String getMonitoringStatistics() {
        return String.format("MonitoringStatistics{metrics=%d, healthChecks=%d, totalRequests=%d, successRate=%.2f%%}", 
                metricCache.size(), healthCheckCache.size(), totalRequests.get(), 
                totalRequests.get() > 0 ? (double) successfulRequests.get() / totalRequests.get() * 100 : 0.0);
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            log.info("监控管理器资源已释放");
        } catch (Exception e) {
            log.error("监控管理器资源释放异常", e);
        }
    }
} 