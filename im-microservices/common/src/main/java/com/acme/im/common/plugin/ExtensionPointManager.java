package com.acme.im.common.plugin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * 扩展点管理器（重构版）
 * 作为三个专门管理器的协调器，职责更加清晰
 * 
 * 重构后的职责：
 * 1. 协调扩展点注册表、钩子管理器和执行监控器
 * 2. 提供统一的扩展点执行接口
 * 3. 管理扩展点的生命周期
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExtensionPointManager {

    private final ExtensionPointRegistry extensionPointRegistry;
    private final HookManager hookManager;
    private final ExecutionMonitor executionMonitor;
    
    // 异步执行器
    private final Executor asyncExecutor = Executors.newFixedThreadPool(10);

    /**
     * 执行扩展点
     * 
     * @param name 扩展点名称
     * @param context 执行上下文
     * @param executor 扩展点执行器
     * @return 执行结果
     */
    public <T> T executeExtensionPoint(String name, Object context, Function<Object, T> executor) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        
        try {
            // 检查扩展点是否存在
            if (!extensionPointRegistry.hasExtensionPoint(name)) {
                log.warn("扩展点不存在: {}", name);
                return null;
            }

            // 获取扩展点定义
            ExtensionPointRegistry.ExtensionPointDefinition definition = 
                extensionPointRegistry.getExtensionPoint(name);
            
            // 记录并发变化
            executionMonitor.recordExtensionPointConcurrencyChange(name, 1);
            
            try {
                // 执行前置钩子
                hookManager.executeBeforeHooks(name, createHookContext(name, context));
                
                // 执行扩展点
                T result = executor.apply(context);
                success = true;
                
                // 执行后置钩子
                hookManager.executeAfterHooks(name, createHookContext(name, context));
                
                return result;
                
            } finally {
                // 记录并发变化
                executionMonitor.recordExtensionPointConcurrencyChange(name, -1);
            }
            
        } catch (Exception e) {
            log.error("扩展点执行异常: name={}, error={}", name, e.getMessage(), e);
            
            // 执行异常钩子
            try {
                hookManager.executeExceptionHooks(name, createHookContext(name, context));
            } catch (Exception hookException) {
                log.error("异常钩子执行失败: name={}, error={}", name, hookException.getMessage(), hookException);
            }
            
            throw new RuntimeException("扩展点执行失败: " + name, e);
            
        } finally {
            // 记录执行统计
            long executionTime = System.currentTimeMillis() - startTime;
            executionMonitor.recordExtensionPointExecution(name, executionTime, success);
        }
    }

    /**
     * 异步执行扩展点
     */
    public <T> CompletableFuture<T> executeExtensionPointAsync(String name, Object context, Function<Object, T> executor) {
        return CompletableFuture.supplyAsync(() -> executeExtensionPoint(name, context, executor), asyncExecutor);
    }

    /**
     * 执行扩展点实现
     */
    public <T> List<T> executeExtensionPointImplementations(String name, Object context, Function<Object, T> executor) {
        List<ExtensionPointRegistry.ExtensionPointImplementation> implementations = 
            extensionPointRegistry.getImplementations(name);
        
        if (implementations.isEmpty()) {
            log.warn("扩展点没有实现: {}", name);
            return List.of();
    }

        return implementations.stream()
                .map(impl -> {
                    try {
                        return executor.apply(impl.getInstance());
                    } catch (Exception e) {
                        log.error("扩展点实现执行失败: name={}, implementation={}, error={}", 
                                name, impl.getInstance().getClass().getSimpleName(), e.getMessage(), e);
                        return null;
                    }
                })
                .filter(result -> result != null)
                .toList();
    }

    /**
     * 注册扩展点
     */
    public void registerExtensionPoint(String name, String description, 
                                     ExtensionPointRegistry.ExecutionStrategy strategy, 
                                     boolean required, int priority, Class<?> targetClass, String targetMethod) {
        ExtensionPointRegistry.ExtensionPointDefinition definition = 
            new ExtensionPointRegistry.ExtensionPointDefinition(name, description, strategy, required, priority, targetClass, targetMethod);
        
        extensionPointRegistry.registerExtensionPoint(name, definition);
        log.info("注册扩展点: name={}, strategy={}, required={}", name, strategy, required);
    }

    /**
     * 注册扩展点实现
     */
    public void registerImplementation(String extensionPointName, Object instance, int priority) {
        extensionPointRegistry.registerImplementation(extensionPointName, instance, priority);
        }

    /**
     * 注册钩子
     */
    public void registerHook(String name, HookManager.HookType type, int order, Object instance, Method method) {
        hookManager.registerHook(name, type, order, instance, method);
    }

    /**
     * 创建钩子上下文
     */
    private HookManager.HookContext createHookContext(String hookName, Object context) {
        return new HookManager.HookContext(hookName, new Object[]{context}, this, null);
    }

    /**
     * 获取扩展点信息
     */
    public ExtensionPointRegistry.ExtensionPointDefinition getExtensionPoint(String name) {
        return extensionPointRegistry.getExtensionPoint(name);
    }

    /**
     * 获取钩子信息
     */
    public List<HookManager.HookDescriptor> getHooks(String name) {
        return hookManager.getHooks(name);
    }

    /**
     * 获取性能报告
     */
    public ExecutionMonitor.PerformanceReport getPerformanceReport() {
        return executionMonitor.generatePerformanceReport();
    }

    /**
     * 检查扩展点是否存在
     */
    public boolean hasExtensionPoint(String name) {
        return extensionPointRegistry.hasExtensionPoint(name);
        }
        
    /**
     * 检查钩子是否存在
     */
    public boolean hasHooks(String name) {
        return hookManager.hasHooks(name);
    }

    /**
     * 获取所有扩展点名称
     */
    public List<String> getAllExtensionPointNames() {
        return extensionPointRegistry.getAllExtensionPointNames();
    }

    /**
     * 获取所有钩子名称
     */
    public List<String> getAllHookNames() {
        return hookManager.getAllHookNames();
    }

    /**
     * 注销扩展点
     */
    public void unregisterExtensionPoint(String name) {
        extensionPointRegistry.unregisterExtensionPoint(name);
    }

    /**
     * 注销扩展点实现
     */
    public void unregisterImplementation(String extensionPointName, Object instance) {
        extensionPointRegistry.unregisterImplementation(extensionPointName, instance);
    }

    /**
     * 注销钩子
     */
    public void unregisterHook(String name, Object instance) {
        hookManager.unregisterHook(name, instance);
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        executionMonitor.resetStats();
    }

    /**
     * 清理过期统计信息
     */
    public void cleanupExpiredStats(java.time.Duration maxAge) {
        executionMonitor.cleanupExpiredStats(maxAge);
    }
} 