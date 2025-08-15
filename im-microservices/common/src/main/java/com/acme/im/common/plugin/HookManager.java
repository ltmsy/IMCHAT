package com.acme.im.common.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;

/**
 * 钩子管理器
 * 专门负责钩子的注册、管理和执行
 * 
 * 职责：
 * 1. 钩子注册和注销
 * 2. 钩子执行和拦截
 * 3. 钩子链式调用
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class HookManager {

    // 钩子注册表：钩子名称 -> 钩子描述符列表
    private final ConcurrentHashMap<String, List<HookDescriptor>> hooks = new ConcurrentHashMap<>();

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
         * 是否启用
         */
        boolean enabled() default true;
    }

    /**
     * 钩子类型枚举
     */
    public enum HookType {
        /**
         * 前置钩子：在目标方法执行前执行
         */
        BEFORE,
        
        /**
         * 后置钩子：在目标方法执行后执行
         */
        AFTER,
        
        /**
         * 环绕钩子：可以完全控制目标方法的执行
         */
        AROUND,
        
        /**
         * 异常钩子：在目标方法抛出异常时执行
         */
        EXCEPTION
    }

    /**
     * 钩子描述符
     */
    public static class HookDescriptor {
        private final String name;
        private final HookType type;
        private final int order;
        private final Object instance;
        private final Method method;
        private final boolean enabled;

        public HookDescriptor(String name, HookType type, int order, Object instance, Method method, boolean enabled) {
            this.name = name;
            this.type = type;
            this.order = order;
            this.instance = instance;
            this.method = method;
            this.enabled = enabled;
        }

        // Getters
        public String getName() { return name; }
        public HookType getType() { return type; }
        public int getOrder() { return order; }
        public Object getInstance() { return instance; }
        public Method getMethod() { return method; }
        public boolean isEnabled() { return enabled; }
    }

    /**
     * 钩子执行上下文
     */
    public static class HookContext {
        private final String hookName;
        private final Object[] arguments;
        private final Object target;
        private final Method targetMethod;
        private Object result;
        private Throwable exception;
        private boolean skipTargetExecution = false;

        public HookContext(String hookName, Object[] arguments, Object target, Method targetMethod) {
            this.hookName = hookName;
            this.arguments = arguments;
            this.target = target;
            this.targetMethod = targetMethod;
        }

        // Getters and Setters
        public String getHookName() { return hookName; }
        public Object[] getArguments() { return arguments; }
        public Object getTarget() { return target; }
        public Method getTargetMethod() { return targetMethod; }
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        public Throwable getException() { return exception; }
        public void setException(Throwable exception) { this.exception = exception; }
        public boolean isSkipTargetExecution() { return skipTargetExecution; }
        public void setSkipTargetExecution(boolean skipTargetExecution) { this.skipTargetExecution = skipTargetExecution; }
    }

    /**
     * 目标执行器接口
     */
    @FunctionalInterface
    public interface TargetExecutor {
        Object execute(HookContext context);
    }

    /**
     * 注册钩子
     */
    public void registerHook(String name, HookType type, int order, Object instance, Method method) {
        HookDescriptor descriptor = new HookDescriptor(name, type, order, instance, method, true);
        
        hooks.computeIfAbsent(name, k -> new ArrayList<>()).add(descriptor);
        
        // 按执行顺序排序
        List<HookDescriptor> hookList = hooks.get(name);
        hookList.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        
        log.debug("注册钩子: name={}, type={}, order={}, instance={}", 
                name, type, order, instance.getClass().getSimpleName());
    }

    /**
     * 注销钩子
     */
    public void unregisterHook(String name, Object instance) {
        List<HookDescriptor> hookList = hooks.get(name);
        if (hookList != null) {
            hookList.removeIf(hook -> hook.getInstance() == instance);
            log.debug("注销钩子: name={}, instance={}", name, instance.getClass().getSimpleName());
        }
    }

    /**
     * 获取钩子列表
     */
    public List<HookDescriptor> getHooks(String name) {
        List<HookDescriptor> hookList = hooks.get(name);
        if (hookList == null) {
            return Collections.emptyList();
        }
        return hookList.stream()
                      .filter(HookDescriptor::isEnabled)
                      .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 执行前置钩子
     */
    public void executeBeforeHooks(String name, HookContext context) {
        List<HookDescriptor> beforeHooks = getHooksByType(name, HookType.BEFORE);
        executeHooks(beforeHooks, context);
    }

    /**
     * 执行后置钩子
     */
    public void executeAfterHooks(String name, HookContext context) {
        List<HookDescriptor> afterHooks = getHooksByType(name, HookType.AFTER);
        executeHooks(afterHooks, context);
    }

    /**
     * 执行环绕钩子
     */
    public Object executeAroundHooks(String name, HookContext context, TargetExecutor targetExecution) {
        List<HookDescriptor> aroundHooks = getHooksByType(name, HookType.AROUND);
        
        if (aroundHooks.isEmpty()) {
            // 没有环绕钩子，直接执行目标方法
            return targetExecution.execute(context);
        }
        
        // 执行环绕钩子链
        return executeAroundHookChain(aroundHooks, 0, context, targetExecution);
    }

    /**
     * 执行异常钩子
     */
    public void executeExceptionHooks(String name, HookContext context) {
        List<HookDescriptor> exceptionHooks = getHooksByType(name, HookType.EXCEPTION);
        executeHooks(exceptionHooks, context);
    }

    /**
     * 根据类型获取钩子
     */
    private List<HookDescriptor> getHooksByType(String name, HookType type) {
        return getHooks(name).stream()
                            .filter(hook -> hook.getType() == type)
                            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 执行钩子列表
     */
    private void executeHooks(List<HookDescriptor> hooks, HookContext context) {
        for (HookDescriptor hook : hooks) {
            try {
                executeHook(hook, context);
            } catch (Exception e) {
                log.error("执行钩子失败: hook={}, error={}", hook.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 执行单个钩子
     */
    private void executeHook(HookDescriptor hook, HookContext context) throws Exception {
        if (!hook.isEnabled()) {
            return;
        }
        
        try {
            hook.getMethod().invoke(hook.getInstance(), context);
        } catch (Exception e) {
            log.error("钩子执行异常: hook={}, method={}, error={}", 
                    hook.getName(), hook.getMethod().getName(), e.getMessage());
            throw e;
        }
    }

    /**
     * 执行环绕钩子链
     */
    private Object executeAroundHookChain(List<HookDescriptor> hooks, int index, 
                                        HookContext context, TargetExecutor targetExecution) {
        if (index >= hooks.size()) {
            // 所有环绕钩子执行完毕，执行目标方法
            return targetExecution.execute(context);
        }
        
        HookDescriptor hook = hooks.get(index);
        if (!hook.isEnabled()) {
            return executeAroundHookChain(hooks, index + 1, context, targetExecution);
        }
        
        try {
            // 创建下一个执行器
            TargetExecutor nextExecutor = (ctx) -> executeAroundHookChain(hooks, index + 1, ctx, targetExecution);
            // 调用钩子方法，传入上下文和下一个执行器
            return hook.getMethod().invoke(hook.getInstance(), context, nextExecutor);
        } catch (Exception e) {
            log.error("环绕钩子执行异常: hook={}, error={}", hook.getName(), e.getMessage(), e);
            throw new RuntimeException("环绕钩子执行失败", e);
        }
    }

    /**
     * 检查钩子是否存在
     */
    public boolean hasHooks(String name) {
        List<HookDescriptor> hookList = hooks.get(name);
        return hookList != null && !hookList.isEmpty();
    }

    /**
     * 获取所有钩子名称
     */
    public List<String> getAllHookNames() {
        return new ArrayList<>(hooks.keySet());
    }

    /**
     * 启用/禁用钩子
     */
    public void setHookEnabled(String name, Object instance, boolean enabled) {
        List<HookDescriptor> hookList = hooks.get(name);
        if (hookList != null) {
            hookList.stream()
                   .filter(hook -> hook.getInstance() == instance)
                   .forEach(hook -> {
                       // 注意：这里需要重新创建HookDescriptor，因为它是不可变的
                       // 在实际实现中，可以考虑使用Builder模式或可变的描述符
                   });
        }
    }
} 