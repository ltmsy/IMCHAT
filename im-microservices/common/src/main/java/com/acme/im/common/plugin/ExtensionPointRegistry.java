package com.acme.im.common.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 扩展点注册表
 * 专门负责扩展点的注册、查询和管理
 * 
 * 职责：
 * 1. 扩展点注册和注销
 * 2. 扩展点查询和检索
 * 3. 扩展点元数据管理
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class ExtensionPointRegistry {

    // 扩展点注册表：扩展点名称 -> 扩展点定义
    private final ConcurrentHashMap<String, ExtensionPointDefinition> extensionPoints = new ConcurrentHashMap<>();
    
    // 扩展点实现注册表：扩展点名称 -> 实现列表
    private final ConcurrentHashMap<String, List<ExtensionPointImplementation>> implementations = new ConcurrentHashMap<>();

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
     * 执行策略枚举
     */
    public enum ExecutionStrategy {
        /**
         * 执行所有实现
         */
        ALL,
        
        /**
         * 执行第一个成功实现
         */
        FIRST_SUCCESS,
        
        /**
         * 执行优先级最高的实现
         */
        HIGHEST_PRIORITY,
        
        /**
         * 执行随机一个实现
         */
        RANDOM
    }

    /**
     * 扩展点定义
     */
    public static class ExtensionPointDefinition {
        private final String name;
        private final String description;
        private final ExecutionStrategy strategy;
        private final boolean required;
        private final int priority;
        private final Class<?> targetClass;
        private final String targetMethod;

        public ExtensionPointDefinition(String name, String description, ExecutionStrategy strategy, 
                                     boolean required, int priority, Class<?> targetClass, String targetMethod) {
            this.name = name;
            this.description = description;
            this.strategy = strategy;
            this.required = required;
            this.priority = priority;
            this.targetClass = targetClass;
            this.targetMethod = targetMethod;
        }

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public ExecutionStrategy getStrategy() { return strategy; }
        public boolean isRequired() { return required; }
        public int getPriority() { return priority; }
        public Class<?> getTargetClass() { return targetClass; }
        public String getTargetMethod() { return targetMethod; }
    }

    /**
     * 扩展点实现
     */
    public static class ExtensionPointImplementation {
        private final String name;
        private final Object instance;
        private final int priority;
        private final boolean enabled;

        public ExtensionPointImplementation(String name, Object instance, int priority, boolean enabled) {
            this.name = name;
            this.instance = instance;
            this.priority = priority;
            this.enabled = enabled;
        }

        // Getters
        public String getName() { return name; }
        public Object getInstance() { return instance; }
        public int getPriority() { return priority; }
        public boolean isEnabled() { return enabled; }
    }

    /**
     * 注册扩展点
     */
    public void registerExtensionPoint(String name, ExtensionPointDefinition definition) {
        extensionPoints.put(name, definition);
        implementations.putIfAbsent(name, new ArrayList<>());
        log.info("注册扩展点: name={}, strategy={}, required={}", 
                name, definition.getStrategy(), definition.isRequired());
    }

    /**
     * 注册扩展点实现
     */
    public void registerImplementation(String extensionPointName, Object instance, int priority) {
        List<ExtensionPointImplementation> implList = implementations.get(extensionPointName);
        if (implList != null) {
            ExtensionPointImplementation impl = new ExtensionPointImplementation(
                extensionPointName, instance, priority, true);
            implList.add(impl);
            
            // 按优先级排序
            implList.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
            
            log.debug("注册扩展点实现: extensionPoint={}, instance={}, priority={}", 
                    extensionPointName, instance.getClass().getSimpleName(), priority);
        }
    }

    /**
     * 获取扩展点定义
     */
    public ExtensionPointDefinition getExtensionPoint(String name) {
        return extensionPoints.get(name);
    }

    /**
     * 获取扩展点实现列表
     */
    public List<ExtensionPointImplementation> getImplementations(String name) {
        List<ExtensionPointImplementation> impls = implementations.get(name);
        if (impls == null) {
            return Collections.emptyList();
        }
        return impls.stream()
                   .filter(ExtensionPointImplementation::isEnabled)
                   .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 检查扩展点是否存在
     */
    public boolean hasExtensionPoint(String name) {
        return extensionPoints.containsKey(name);
    }

    /**
     * 检查扩展点是否有实现
     */
    public boolean hasImplementation(String name) {
        List<ExtensionPointImplementation> impls = implementations.get(name);
        return impls != null && !impls.isEmpty();
    }

    /**
     * 获取所有扩展点名称
     */
    public List<String> getAllExtensionPointNames() {
        return new ArrayList<>(extensionPoints.keySet());
    }

    /**
     * 注销扩展点
     */
    public void unregisterExtensionPoint(String name) {
        extensionPoints.remove(name);
        implementations.remove(name);
        log.info("注销扩展点: name={}", name);
    }

    /**
     * 注销扩展点实现
     */
    public void unregisterImplementation(String extensionPointName, Object instance) {
        List<ExtensionPointImplementation> implList = implementations.get(extensionPointName);
        if (implList != null) {
            implList.removeIf(impl -> impl.getInstance() == instance);
            log.debug("注销扩展点实现: extensionPoint={}, instance={}", 
                    extensionPointName, instance.getClass().getSimpleName());
        }
    }
} 