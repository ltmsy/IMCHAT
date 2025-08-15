package com.acme.im.common.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 执行监控器
 * 专门负责扩展点和钩子的执行统计和性能监控
 * 
 * 职责：
 * 1. 执行次数统计
 * 2. 执行时间监控
 * 3. 异常统计
 * 4. 性能指标收集
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class ExecutionMonitor {

    // 扩展点执行统计：扩展点名称 -> 执行统计
    private final ConcurrentHashMap<String, ExtensionPointStats> extensionPointStats = new ConcurrentHashMap<>();
    
    // 钩子执行统计：钩子名称 -> 执行统计
    private final ConcurrentHashMap<String, HookStats> hookStats = new ConcurrentHashMap<>();

    /**
     * 扩展点执行统计
     */
    public static class ExtensionPointStats {
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong successfulExecutions = new AtomicLong(0);
        private final AtomicLong failedExecutions = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxExecutionTime = new AtomicLong(0);
        private final AtomicInteger currentConcurrency = new AtomicInteger(0);
        private final AtomicInteger maxConcurrency = new AtomicInteger(0);
        private volatile LocalDateTime lastExecutionTime;
        private volatile LocalDateTime firstExecutionTime;

        public void recordExecution(long executionTime, boolean success) {
            totalExecutions.incrementAndGet();
            totalExecutionTime.addAndGet(executionTime);
            
            if (success) {
                successfulExecutions.incrementAndGet();
            } else {
                failedExecutions.incrementAndGet();
            }
            
            // 更新最小和最大执行时间
            minExecutionTime.updateAndGet(current -> Math.min(current, executionTime));
            maxExecutionTime.updateAndGet(current -> Math.max(current, executionTime));
            
            // 更新执行时间
            LocalDateTime now = LocalDateTime.now();
            lastExecutionTime = now;
            if (firstExecutionTime == null) {
                firstExecutionTime = now;
            }
        }

        public void recordConcurrencyChange(int delta) {
            int current = currentConcurrency.addAndGet(delta);
            maxConcurrency.updateAndGet(max -> Math.max(max, current));
        }

        // Getters
        public long getTotalExecutions() { return totalExecutions.get(); }
        public long getSuccessfulExecutions() { return successfulExecutions.get(); }
        public long getFailedExecutions() { return failedExecutions.get(); }
        public long getTotalExecutionTime() { return totalExecutionTime.get(); }
        public long getMinExecutionTime() { return minExecutionTime.get(); }
        public long getMaxExecutionTime() { return maxExecutionTime.get(); }
        public int getCurrentConcurrency() { return currentConcurrency.get(); }
        public int getMaxConcurrency() { return maxConcurrency.get(); }
        public LocalDateTime getLastExecutionTime() { return lastExecutionTime; }
        public LocalDateTime getFirstExecutionTime() { return firstExecutionTime; }
        
        public double getAverageExecutionTime() {
            long total = totalExecutions.get();
            return total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
        }
        
        public double getSuccessRate() {
            long total = totalExecutions.get();
            return total > 0 ? (double) successfulExecutions.get() / total : 0.0;
        }
    }

    /**
     * 钩子执行统计
     */
    public static class HookStats {
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong successfulExecutions = new AtomicLong(0);
        private final AtomicLong failedExecutions = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxExecutionTime = new AtomicLong(0);
        private volatile LocalDateTime lastExecutionTime;
        private volatile LocalDateTime firstExecutionTime;

        public void recordExecution(long executionTime, boolean success) {
            totalExecutions.incrementAndGet();
            totalExecutionTime.addAndGet(executionTime);
            
            if (success) {
                successfulExecutions.incrementAndGet();
            } else {
                failedExecutions.incrementAndGet();
            }
            
            // 更新最小和最大执行时间
            minExecutionTime.updateAndGet(current -> Math.min(current, executionTime));
            maxExecutionTime.updateAndGet(current -> Math.max(current, executionTime));
            
            // 更新执行时间
            LocalDateTime now = LocalDateTime.now();
            lastExecutionTime = now;
            if (firstExecutionTime == null) {
                firstExecutionTime = now;
            }
        }

        // Getters
        public long getTotalExecutions() { return totalExecutions.get(); }
        public long getSuccessfulExecutions() { return successfulExecutions.get(); }
        public long getFailedExecutions() { return failedExecutions.get(); }
        public long getTotalExecutionTime() { return totalExecutionTime.get(); }
        public long getMinExecutionTime() { return minExecutionTime.get(); }
        public long getMaxExecutionTime() { return maxExecutionTime.get(); }
        public LocalDateTime getLastExecutionTime() { return lastExecutionTime; }
        public LocalDateTime getFirstExecutionTime() { return firstExecutionTime; }
        
        public double getAverageExecutionTime() {
            long total = totalExecutions.get();
            return total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
        }
        
        public double getSuccessRate() {
            long total = totalExecutions.get();
            return total > 0 ? (double) successfulExecutions.get() / total : 0.0;
        }
    }

    /**
     * 记录扩展点执行
     */
    public void recordExtensionPointExecution(String name, long executionTime, boolean success) {
        ExtensionPointStats stats = extensionPointStats.computeIfAbsent(name, k -> new ExtensionPointStats());
        stats.recordExecution(executionTime, success);
        
        if (log.isDebugEnabled()) {
            log.debug("扩展点执行记录: name={}, time={}ms, success={}", name, executionTime, success);
        }
    }

    /**
     * 记录钩子执行
     */
    public void recordHookExecution(String name, long executionTime, boolean success) {
        HookStats stats = hookStats.computeIfAbsent(name, k -> new HookStats());
        stats.recordExecution(executionTime, success);
        
        if (log.isDebugEnabled()) {
            log.debug("钩子执行记录: name={}, time={}ms, success={}", name, executionTime, success);
        }
    }

    /**
     * 记录扩展点并发变化
     */
    public void recordExtensionPointConcurrencyChange(String name, int delta) {
        ExtensionPointStats stats = extensionPointStats.computeIfAbsent(name, k -> new ExtensionPointStats());
        stats.recordConcurrencyChange(delta);
    }

    /**
     * 获取扩展点统计信息
     */
    public ExtensionPointStats getExtensionPointStats(String name) {
        return extensionPointStats.get(name);
    }

    /**
     * 获取钩子统计信息
     */
    public HookStats getHookStats(String name) {
        return hookStats.get(name);
    }

    /**
     * 获取所有扩展点统计信息
     */
    public Map<String, ExtensionPointStats> getAllExtensionPointStats() {
        return new ConcurrentHashMap<>(extensionPointStats);
    }

    /**
     * 获取所有钩子统计信息
     */
    public Map<String, HookStats> getAllHookStats() {
        return new ConcurrentHashMap<>(hookStats);
    }

    /**
     * 获取性能报告
     */
    public PerformanceReport generatePerformanceReport() {
        PerformanceReport report = new PerformanceReport();
        report.setGeneratedAt(LocalDateTime.now());
        
        // 扩展点性能统计
        List<ExtensionPointPerformance> extensionPointPerformances = extensionPointStats.entrySet().stream()
                .map(entry -> {
                    ExtensionPointPerformance perf = new ExtensionPointPerformance();
                    perf.setName(entry.getKey());
                    perf.setStats(entry.getValue());
                    return perf;
                })
                .collect(Collectors.toList());
        report.setExtensionPointPerformances(extensionPointPerformances);
        
        // 钩子性能统计
        List<HookPerformance> hookPerformances = hookStats.entrySet().stream()
                .map(entry -> {
                    HookPerformance perf = new HookPerformance();
                    perf.setName(entry.getKey());
                    perf.setStats(entry.getValue());
                    return perf;
                })
                .collect(Collectors.toList());
        report.setHookPerformances(hookPerformances);
        
        return report;
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        extensionPointStats.clear();
        hookStats.clear();
        log.info("执行统计信息已重置");
    }

    /**
     * 清理过期的统计信息
     */
    public void cleanupExpiredStats(Duration maxAge) {
        LocalDateTime cutoff = LocalDateTime.now().minus(maxAge);
        
        // 清理扩展点统计
        extensionPointStats.entrySet().removeIf(entry -> {
            ExtensionPointStats stats = entry.getValue();
            return stats.getLastExecutionTime() != null && 
                   stats.getLastExecutionTime().isBefore(cutoff);
        });
        
        // 清理钩子统计
        hookStats.entrySet().removeIf(entry -> {
            HookStats stats = entry.getValue();
            return stats.getLastExecutionTime() != null && 
                   stats.getLastExecutionTime().isBefore(cutoff);
        });
        
        log.info("已清理过期统计信息，保留时间: {}", maxAge);
    }

    /**
     * 性能报告
     */
    public static class PerformanceReport {
        private LocalDateTime generatedAt;
        private List<ExtensionPointPerformance> extensionPointPerformances;
        private List<HookPerformance> hookPerformances;

        // Getters and Setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public List<ExtensionPointPerformance> getExtensionPointPerformances() { return extensionPointPerformances; }
        public void setExtensionPointPerformances(List<ExtensionPointPerformance> extensionPointPerformances) { this.extensionPointPerformances = extensionPointPerformances; }
        public List<HookPerformance> getHookPerformances() { return hookPerformances; }
        public void setHookPerformances(List<HookPerformance> hookPerformances) { this.hookPerformances = hookPerformances; }
    }

    /**
     * 扩展点性能
     */
    public static class ExtensionPointPerformance {
        private String name;
        private ExtensionPointStats stats;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public ExtensionPointStats getStats() { return stats; }
        public void setStats(ExtensionPointStats stats) { this.stats = stats; }
    }

    /**
     * 钩子性能
     */
    public static class HookPerformance {
        private String name;
        private HookStats stats;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public HookStats getStats() { return stats; }
        public void setStats(HookStats stats) { this.stats = stats; }
    }
} 