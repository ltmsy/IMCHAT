package com.acme.im.common.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 扩展点管理器
 * 提供系统的钩子机制和扩展点管理，支持插件的灵活扩展
 * 
 * 特性：
 * 1. 扩展点定义 - 声明式扩展点定义
 * 2. 钩子机制 - 方法级钩子拦截
 * 3. 执行策略 - 多种扩展点执行策略
 * 4. 条件执行 - 基于条件的扩展点执行
 * 5. 结果聚合 - 多个扩展实现的结果聚合
 * 6. 异常处理 - 扩展点执行异常处理
 * 7. 性能监控 - 扩展点执行性能统计
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class ExtensionPointManager {

    @Autowired
    private PluginRegistry pluginRegistry;

    // 扩展点注册表
    private final ConcurrentHashMap<String, ExtensionPointDefinition> extensionPoints = new ConcurrentHashMap<>();
    
    // 钩子注册表
    private final ConcurrentHashMap<String, List<HookDescriptor>> hooks = new ConcurrentHashMap<>();
    
    // 执行统计
    private final ConcurrentHashMap<String, ExecutionStats> executionStats = new ConcurrentHashMap<>();

    /**
     * 扩展点注解
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ExtensionPoint {
        /**
         * 扩展点名称
         */
        String name();
        
        /**
         * 扩展点描述
         */
        String description() default "";
        
        /**
         * 执行策略
         */
        ExecutionStrategy strategy() default ExecutionStrategy.ALL;
        
        /**
         * 是否必须有实现
         */
        boolean required() default false;
        
        /**
         * 优先级
         */
        int priority() default 0;
    }

    /**
     * 钩子注解
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Hook {
        /**
         * 钩子名称
         */
        String name();
        
        /**
         * 钩子类型
         */
        HookType type() default HookType.AROUND;
        
        /**
         * 执行顺序
         */
        int order() default 0;
        
        /**
         * 条件表达式
         */
        String condition() default "";
    }

    /**
     * 执行策略
     */
    public enum ExecutionStrategy {
        FIRST,      // 执行第一个
        ALL,        // 执行所有
        UNTIL_SUCCESS, // 执行直到成功
        AGGREGATE   // 聚合结果
    }

    /**
     * 钩子类型
     */
    public enum HookType {
        BEFORE,     // 前置钩子
        AFTER,      // 后置钩子
        AROUND,     // 环绕钩子
        ON_ERROR    // 异常钩子
    }

    /**
     * 扩展点定义
     */
    public static class ExtensionPointDefinition {
        private final String name;
        private final String description;
        private final Class<?> extensionInterface;
        private final ExecutionStrategy strategy;
        private final boolean required;
        private final int priority;
        private final Map<String, Object> metadata;

        public ExtensionPointDefinition(String name, String description, Class<?> extensionInterface,
                                      ExecutionStrategy strategy, boolean required, int priority,
                                      Map<String, Object> metadata) {
            this.name = name;
            this.description = description;
            this.extensionInterface = extensionInterface;
            this.strategy = strategy;
            this.required = required;
            this.priority = priority;
            this.metadata = metadata != null ? metadata : Collections.emptyMap();
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public Class<?> getExtensionInterface() { return extensionInterface; }
        public ExecutionStrategy getStrategy() { return strategy; }
        public boolean isRequired() { return required; }
        public int getPriority() { return priority; }
        public Map<String, Object> getMetadata() { return metadata; }

        @Override
        public String toString() {
            return String.format("ExtensionPointDefinition{name=%s, interface=%s, strategy=%s, required=%s}",
                               name, extensionInterface.getSimpleName(), strategy, required);
        }
    }

    /**
     * 钩子描述符
     */
    public static class HookDescriptor {
        private final String name;
        private final HookType type;
        private final Method method;
        private final Object instance;
        private final int order;
        private final String condition;
        private final String pluginId;

        public HookDescriptor(String name, HookType type, Method method, Object instance,
                            int order, String condition, String pluginId) {
            this.name = name;
            this.type = type;
            this.method = method;
            this.instance = instance;
            this.order = order;
            this.condition = condition;
            this.pluginId = pluginId;
        }

        public String getName() { return name; }
        public HookType getType() { return type; }
        public Method getMethod() { return method; }
        public Object getInstance() { return instance; }
        public int getOrder() { return order; }
        public String getCondition() { return condition; }
        public String getPluginId() { return pluginId; }

        @Override
        public String toString() {
            return String.format("HookDescriptor{name=%s, type=%s, plugin=%s, order=%d}",
                               name, type, pluginId, order);
        }
    }

    /**
     * 执行上下文
     */
    public static class ExecutionContext {
        private final String extensionPointName;
        private final Object[] parameters;
        private final Map<String, Object> attributes;
        private Object result;
        private Throwable error;

        public ExecutionContext(String extensionPointName, Object[] parameters) {
            this.extensionPointName = extensionPointName;
            this.parameters = parameters;
            this.attributes = new ConcurrentHashMap<>();
        }

        public String getExtensionPointName() { return extensionPointName; }
        public Object[] getParameters() { return parameters; }
        public Map<String, Object> getAttributes() { return attributes; }
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        public Throwable getError() { return error; }
        public void setError(Throwable error) { this.error = error; }

        public void setAttribute(String key, Object value) { attributes.put(key, value); }
        public <T> T getAttribute(String key) { return (T) attributes.get(key); }
        public boolean hasAttribute(String key) { return attributes.containsKey(key); }
    }

    /**
     * 执行统计
     */
    public static class ExecutionStats {
        private long totalExecutions = 0;
        private long successfulExecutions = 0;
        private long failedExecutions = 0;
        private long totalExecutionTime = 0;
        private long maxExecutionTime = 0;
        private long minExecutionTime = Long.MAX_VALUE;

        public synchronized void recordExecution(long executionTime, boolean success) {
            totalExecutions++;
            totalExecutionTime += executionTime;
            
            if (executionTime > maxExecutionTime) {
                maxExecutionTime = executionTime;
            }
            if (executionTime < minExecutionTime) {
                minExecutionTime = executionTime;
            }
            
            if (success) {
                successfulExecutions++;
            } else {
                failedExecutions++;
            }
        }

        public long getTotalExecutions() { return totalExecutions; }
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public long getFailedExecutions() { return failedExecutions; }
        public double getSuccessRate() { 
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0; 
        }
        public double getAverageExecutionTime() { 
            return totalExecutions > 0 ? (double) totalExecutionTime / totalExecutions : 0.0; 
        }
        public long getMaxExecutionTime() { return maxExecutionTime; }
        public long getMinExecutionTime() { return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime; }

        @Override
        public String toString() {
            return String.format("ExecutionStats{total=%d, success=%d, failed=%d, successRate=%.2f%%, avgTime=%.2fms}",
                               totalExecutions, successfulExecutions, failedExecutions, 
                               getSuccessRate() * 100, getAverageExecutionTime());
        }
    }

    /**
     * 扩展点执行器
     */
    @FunctionalInterface
    public interface ExtensionExecutor<T> {
        T execute(Object extension, Object[] parameters) throws Exception;
    }

    /**
     * 初始化扩展点管理器
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化扩展点管理器...");
        
        // 注册系统内置扩展点
        registerSystemExtensionPoints();
        
        log.info("扩展点管理器初始化完成: 扩展点数量={}", extensionPoints.size());
    }

    /**
     * 定义扩展点
     */
    public void defineExtensionPoint(String name, String description, Class<?> extensionInterface,
                                   ExecutionStrategy strategy, boolean required, int priority,
                                   Map<String, Object> metadata) {
        ExtensionPointDefinition definition = new ExtensionPointDefinition(
                name, description, extensionInterface, strategy, required, priority, metadata);
        
        extensionPoints.put(name, definition);
        log.info("定义扩展点: {}", definition);
    }

    /**
     * 定义扩展点（简化版本）
     */
    public void defineExtensionPoint(String name, Class<?> extensionInterface) {
        defineExtensionPoint(name, "", extensionInterface, ExecutionStrategy.ALL, false, 0, null);
    }

    /**
     * 注册钩子
     */
    public void registerHook(String hookName, HookType type, Method method, Object instance,
                           int order, String condition, String pluginId) {
        HookDescriptor descriptor = new HookDescriptor(
                hookName, type, method, instance, order, condition, pluginId);
        
        hooks.computeIfAbsent(hookName, k -> new CopyOnWriteArrayList<>()).add(descriptor);
        
        // 按order排序
        hooks.get(hookName).sort(Comparator.comparingInt(HookDescriptor::getOrder));
        
        log.info("注册钩子: {}", descriptor);
    }

    /**
     * 执行扩展点
     */
    public <T> T executeExtensionPoint(String name, Object[] parameters, ExtensionExecutor<T> executor) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        
        try {
            ExtensionPointDefinition definition = extensionPoints.get(name);
            if (definition == null) {
                if (log.isDebugEnabled()) {
                    log.debug("扩展点未定义: {}", name);
                }
                return null;
            }

            // 创建执行上下文
            ExecutionContext context = new ExecutionContext(name, parameters);
            
            // 执行前置钩子
            executeHooks(name, HookType.BEFORE, context);
            
            try {
                // 获取扩展实现
                List<?> extensions = pluginRegistry.getExtensions(definition.getExtensionInterface());
                
                if (extensions.isEmpty() && definition.isRequired()) {
                    throw new RuntimeException("必需的扩展点没有实现: " + name);
                }

                // 根据策略执行扩展点
                T result = executeByStrategy(definition, extensions, parameters, executor);
                context.setResult(result);
                
                // 执行后置钩子
                executeHooks(name, HookType.AFTER, context);
                
                success = true;
                return result;
                
            } catch (Exception e) {
                context.setError(e);
                
                // 执行异常钩子
                executeHooks(name, HookType.ON_ERROR, context);
                
                throw e;
            }
            
        } catch (Exception e) {
            log.error("执行扩展点失败: name={}", name, e);
            throw new RuntimeException("扩展点执行失败", e);
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            recordExecution(name, executionTime, success);
        }
    }

    /**
     * 执行扩展点（无返回值）
     */
    public void executeExtensionPoint(String name, Object[] parameters) {
        executeExtensionPoint(name, parameters, (extension, params) -> {
            try {
                // 查找并执行方法
                Method method = findExecuteMethod(extension.getClass());
                if (method != null) {
                    method.invoke(extension, params);
                }
            } catch (Exception e) {
                throw new RuntimeException("执行扩展失败", e);
            }
            return null;
        });
    }

    /**
     * 获取扩展点定义
     */
    public ExtensionPointDefinition getExtensionPointDefinition(String name) {
        return extensionPoints.get(name);
    }

    /**
     * 获取所有扩展点定义
     */
    public List<ExtensionPointDefinition> getAllExtensionPointDefinitions() {
        return new ArrayList<>(extensionPoints.values());
    }

    /**
     * 获取钩子列表
     */
    public List<HookDescriptor> getHooks(String hookName) {
        return new ArrayList<>(hooks.getOrDefault(hookName, Collections.emptyList()));
    }

    /**
     * 获取执行统计
     */
    public ExecutionStats getExecutionStats(String extensionPointName) {
        return executionStats.get(extensionPointName);
    }

    /**
     * 获取所有执行统计
     */
    public Map<String, ExecutionStats> getAllExecutionStats() {
        return new HashMap<>(executionStats);
    }

    /**
     * 清理插件相关的钩子
     */
    public void cleanupPluginHooks(String pluginId) {
        hooks.values().forEach(hookList -> 
            hookList.removeIf(hook -> hook.getPluginId().equals(pluginId)));
        
        log.info("清理插件钩子: {}", pluginId);
    }

    // ================================
    // 私有辅助方法
    // ================================

    /**
     * 根据策略执行扩展点
     */
    @SuppressWarnings("unchecked")
    private <T> T executeByStrategy(ExtensionPointDefinition definition, List<?> extensions,
                                   Object[] parameters, ExtensionExecutor<T> executor) throws Exception {
        if (extensions.isEmpty()) {
            return null;
        }

        switch (definition.getStrategy()) {
            case FIRST:
                return executor.execute(extensions.get(0), parameters);
                
            case ALL:
                T lastResult = null;
                for (Object extension : extensions) {
                    lastResult = executor.execute(extension, parameters);
                }
                return lastResult;
                
            case UNTIL_SUCCESS:
                for (Object extension : extensions) {
                    try {
                        T result = executor.execute(extension, parameters);
                        if (result != null) {
                            return result;
                        }
                    } catch (Exception e) {
                        log.debug("扩展执行失败，尝试下一个: {}", extension.getClass().getName(), e);
                    }
                }
                return null;
                
            case AGGREGATE:
                List<T> results = new ArrayList<>();
                for (Object extension : extensions) {
                    try {
                        T result = executor.execute(extension, parameters);
                        if (result != null) {
                            results.add(result);
                        }
                    } catch (Exception e) {
                        log.warn("聚合执行中的扩展失败: {}", extension.getClass().getName(), e);
                    }
                }
                return (T) results;
                
            default:
                throw new UnsupportedOperationException("不支持的执行策略: " + definition.getStrategy());
        }
    }

    /**
     * 执行钩子
     */
    private void executeHooks(String extensionPointName, HookType hookType, ExecutionContext context) {
        List<HookDescriptor> hookList = hooks.get(extensionPointName);
        if (hookList == null) {
            return;
        }

        List<HookDescriptor> typeHooks = hookList.stream()
                .filter(hook -> hook.getType() == hookType)
                .collect(Collectors.toList());

        for (HookDescriptor hook : typeHooks) {
            try {
                // 检查条件
                if (!evaluateCondition(hook.getCondition(), context)) {
                    continue;
                }

                // 执行钩子
                hook.getMethod().invoke(hook.getInstance(), context);
                
            } catch (Exception e) {
                log.error("钩子执行失败: hook={}, extensionPoint={}", 
                         hook.getName(), extensionPointName, e);
            }
        }
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition, ExecutionContext context) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }
        
        // 简单的条件评估实现
        // 实际项目中可以集成SpEL或其他表达式引擎
        try {
            // 这里只是示例，实际应该实现完整的表达式评估
            if (condition.equals("true")) return true;
            if (condition.equals("false")) return false;
            
            // 支持简单的属性检查
            if (condition.startsWith("hasAttribute(") && condition.endsWith(")")) {
                String attrName = condition.substring(13, condition.length() - 1).replace("'", "").replace("\"", "");
                return context.hasAttribute(attrName);
            }
            
            return true;
        } catch (Exception e) {
            log.warn("条件表达式评估失败: {}", condition, e);
            return true;
        }
    }

    /**
     * 查找执行方法
     */
    private Method findExecuteMethod(Class<?> extensionClass) {
        // 查找标准的execute方法
        for (Method method : extensionClass.getMethods()) {
            if ("execute".equals(method.getName())) {
                return method;
            }
        }
        
        // 查找带有特定注解的方法
        for (Method method : extensionClass.getMethods()) {
            if (method.isAnnotationPresent(ExtensionPoint.class)) {
                return method;
            }
        }
        
        return null;
    }

    /**
     * 记录执行统计
     */
    private void recordExecution(String extensionPointName, long executionTime, boolean success) {
        executionStats.computeIfAbsent(extensionPointName, k -> new ExecutionStats())
                     .recordExecution(executionTime, success);
    }

    /**
     * 注册系统内置扩展点
     */
    private void registerSystemExtensionPoints() {
        // 消息处理扩展点
        defineExtensionPoint("message.process", "消息处理扩展点", 
                MessageProcessor.class, ExecutionStrategy.ALL, false, 100, null);
        
        // 用户认证扩展点
        defineExtensionPoint("user.authenticate", "用户认证扩展点", 
                UserAuthenticator.class, ExecutionStrategy.UNTIL_SUCCESS, true, 200, null);
        
        // 消息过滤扩展点
        defineExtensionPoint("message.filter", "消息过滤扩展点", 
                MessageFilter.class, ExecutionStrategy.ALL, false, 150, null);
        
        log.info("注册系统内置扩展点完成");
    }

    // ================================
    // 内置扩展点接口定义
    // ================================

    /**
     * 消息处理器扩展点
     */
    public interface MessageProcessor {
        void processMessage(Object message, Map<String, Object> context);
    }

    /**
     * 用户认证器扩展点
     */
    public interface UserAuthenticator {
        boolean authenticate(String username, String password, Map<String, Object> context);
    }

    /**
     * 消息过滤器扩展点
     */
    public interface MessageFilter {
        boolean filter(Object message, Map<String, Object> context);
    }
} 