package com.acme.im.common.infrastructure.database.aspect;

import com.acme.im.common.infrastructure.database.annotation.DataSource;
import com.acme.im.common.infrastructure.database.config.DataSourceRouter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据源路由切面
 * 使用AOP动态切换数据源，替代原来的静态方法调用
 * 
 * 职责：
 * 1. 拦截带有@DataSource注解的方法
 * 2. 根据注解配置动态切换数据源
 * 3. 支持自动数据源选择
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
@Slf4j
public class DataSourceAspect {

    /**
     * 定义切点：所有带有@DataSource注解的方法
     */
    @Pointcut("@annotation(com.acme.im.common.infrastructure.database.annotation.DataSource)")
    public void dataSourcePointcut() {}

    /**
     * 定义切点：所有带有@DataSource注解的类中的方法
     */
    @Pointcut("@within(com.acme.im.common.infrastructure.database.annotation.DataSource)")
    public void dataSourceClassPointcut() {}

    /**
     * 环绕通知：在方法执行前后切换数据源
     */
    @Around("dataSourcePointcut() || dataSourceClassPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        String originalDataSource = null;
        String targetDataSource = null;
        
        try {
            // 获取目标数据源
            targetDataSource = determineDataSource(point);
            
            if (targetDataSource != null) {
                // 保存当前数据源
                originalDataSource = DataSourceRouter.getDataSource();
                
                // 切换到目标数据源
                DataSourceRouter.setDataSource(targetDataSource);
                
                log.debug("切换数据源: {} -> {}", originalDataSource, targetDataSource);
            }
            
            // 执行目标方法
            return point.proceed();
            
        } finally {
            // 恢复原始数据源
            if (originalDataSource != null) {
                DataSourceRouter.setDataSource(originalDataSource);
                log.debug("恢复数据源: {} -> {}", targetDataSource, originalDataSource);
            }
        }
    }

    /**
     * 确定目标数据源
     */
    private String determineDataSource(ProceedingJoinPoint point) {
        // 优先获取方法级别的注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        
        DataSource methodAnnotation = AnnotationUtils.findAnnotation(method, DataSource.class);
        if (methodAnnotation != null) {
            return resolveDataSource(methodAnnotation, method);
        }
        
        // 如果没有方法级注解，获取类级别的注解
        Class<?> targetClass = point.getTarget().getClass();
        DataSource classAnnotation = AnnotationUtils.findAnnotation(targetClass, DataSource.class);
        if (classAnnotation != null) {
            return resolveDataSource(classAnnotation, method);
        }
        
        // 默认使用自动选择
        return autoSelectDataSource(method);
    }

    /**
     * 解析数据源注解
     */
    private String resolveDataSource(DataSource annotation, Method method) {
        // 如果强制使用指定数据源，直接返回
        if (annotation.force()) {
            return annotation.value();
        }
        
        // 根据类型选择数据源
        switch (annotation.type()) {
            case PRIMARY:
                return "primary";
            case SECONDARY:
                return "secondary";
            case AUTO:
            default:
                return autoSelectDataSource(method);
        }
    }

    /**
     * 自动选择数据源
     * 根据方法名和参数自动判断应该使用主库还是从库
     */
    private String autoSelectDataSource(Method method) {
        String methodName = method.getName().toLowerCase();
        
        // 写操作使用主库
        if (isWriteOperation(methodName)) {
            return "primary";
        }
        
        // 读操作使用从库
        if (isReadOperation(methodName)) {
            return "secondary";
        }
        
        // 默认使用从库（读操作）
        return "secondary";
    }

    /**
     * 判断是否为写操作
     */
    private boolean isWriteOperation(String methodName) {
        return methodName.startsWith("save") ||
               methodName.startsWith("insert") ||
               methodName.startsWith("update") ||
               methodName.startsWith("delete") ||
               methodName.startsWith("remove") ||
               methodName.startsWith("create") ||
               methodName.startsWith("modify") ||
               methodName.startsWith("batch") ||
               methodName.contains("save") ||
               methodName.contains("insert") ||
               methodName.contains("update") ||
               methodName.contains("delete") ||
               methodName.contains("remove") ||
               methodName.contains("create") ||
               methodName.contains("modify");
    }

    /**
     * 判断是否为读操作
     */
    private boolean isReadOperation(String methodName) {
        return methodName.startsWith("find") ||
               methodName.startsWith("get") ||
               methodName.startsWith("query") ||
               methodName.startsWith("select") ||
               methodName.startsWith("list") ||
               methodName.startsWith("count") ||
               methodName.startsWith("exists") ||
               methodName.startsWith("check") ||
               methodName.contains("find") ||
               methodName.contains("get") ||
               methodName.contains("query") ||
               methodName.contains("select") ||
               methodName.contains("list") ||
               methodName.contains("count") ||
               methodName.contains("exists") ||
               methodName.contains("check");
    }
} 