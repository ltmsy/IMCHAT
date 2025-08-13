package com.acme.im.common.infrastructure.database.aspect;

import com.acme.im.common.infrastructure.database.annotation.DataSource;
import com.acme.im.common.infrastructure.database.annotation.ReadOnly;
import com.acme.im.common.infrastructure.database.config.DataSourceRouter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 数据源切换切面
 * 自动处理@DataSource和@ReadOnly注解的数据源切换
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Order(1)
@Component
public class DataSourceAspect {

    /**
     * 处理@DataSource注解
     */
    @Around("@annotation(dataSource)")
    public Object aroundDataSource(ProceedingJoinPoint point, DataSource dataSource) throws Throwable {
        String targetDataSource = determineDataSource(dataSource);
        String originalDataSource = DataSourceRouter.getDataSource();
        
        try {
            // 切换到指定数据源
            DataSourceRouter.setDataSource(targetDataSource);
            log.debug("方法 {} 切换到数据源: {} (操作类型: {})", 
                    point.getSignature().getName(), targetDataSource, dataSource.operation());
            
            // 执行原方法
            return point.proceed();
            
        } finally {
            // 恢复原数据源
            if (originalDataSource != null) {
                DataSourceRouter.setDataSource(originalDataSource);
                log.debug("方法 {} 恢复数据源: {}", 
                        point.getSignature().getName(), originalDataSource);
            } else {
                DataSourceRouter.clearDataSource();
                log.debug("方法 {} 清除数据源上下文", point.getSignature().getName());
            }
        }
    }

    /**
     * 处理@ReadOnly注解（向后兼容）
     */
    @Around("@annotation(readOnly)")
    public Object aroundReadOnly(ProceedingJoinPoint point, ReadOnly readOnly) throws Throwable {
        String dataSource = readOnly.value();
        String originalDataSource = DataSourceRouter.getDataSource();
        
        try {
            // 切换到指定数据源
            DataSourceRouter.setDataSource(dataSource);
            log.debug("方法 {} 切换到数据源: {}", 
                    point.getSignature().getName(), dataSource);
            
            // 执行原方法
            return point.proceed();
            
        } finally {
            // 恢复原数据源
            if (originalDataSource != null) {
                DataSourceRouter.setDataSource(originalDataSource);
                log.debug("方法 {} 恢复数据源: {}", 
                        point.getSignature().getName(), originalDataSource);
            } else {
                DataSourceRouter.clearDataSource();
                log.debug("方法 {} 清除数据源上下文", point.getSignature().getName());
            }
        }
    }

    /**
     * 处理类级别的@DataSource注解
     */
    @Around("@within(dataSource)")
    public Object aroundClassDataSource(ProceedingJoinPoint point, DataSource dataSource) throws Throwable {
        String targetDataSource = determineDataSource(dataSource);
        String originalDataSource = DataSourceRouter.getDataSource();
        
        try {
            // 切换到指定数据源
            DataSourceRouter.setDataSource(targetDataSource);
            log.debug("类 {} 切换到数据源: {} (操作类型: {})", 
                    point.getTarget().getClass().getSimpleName(), targetDataSource, dataSource.operation());
            
            // 执行原方法
            return point.proceed();
            
        } finally {
            // 恢复原数据源
            if (originalDataSource != null) {
                DataSourceRouter.setDataSource(originalDataSource);
                log.debug("类 {} 恢复数据源: {}", 
                        point.getTarget().getClass().getSimpleName(), originalDataSource);
            } else {
                DataSourceRouter.clearDataSource();
                log.debug("类 {} 清除数据源上下文", point.getTarget().getClass().getSimpleName());
            }
        }
    }

    /**
     * 处理类级别的@ReadOnly注解（向后兼容）
     */
    @Around("@within(readOnly)")
    public Object aroundClassReadOnly(ProceedingJoinPoint point, ReadOnly readOnly) throws Throwable {
        String dataSource = readOnly.value();
        String originalDataSource = DataSourceRouter.getDataSource();
        
        try {
            // 切换到指定数据源
            DataSourceRouter.setDataSource(dataSource);
            log.debug("类 {} 切换到数据源: {}", 
                    point.getTarget().getClass().getSimpleName(), dataSource);
            
            // 执行原方法
            return point.proceed();
            
        } finally {
            // 恢复原数据源
            if (originalDataSource != null) {
                DataSourceRouter.setDataSource(originalDataSource);
                log.debug("类 {} 恢复数据源: {}", 
                        point.getTarget().getClass().getSimpleName(), originalDataSource);
            } else {
                DataSourceRouter.clearDataSource();
                log.debug("类 {} 清除数据源上下文", point.getTarget().getClass().getSimpleName());
            }
        }
    }

    /**
     * 确定要使用的数据源
     */
    private String determineDataSource(DataSource dataSource) {
        if (dataSource.value() == DataSource.DataSourceType.AUTO) {
            // 根据操作类型自动选择
            switch (dataSource.operation()) {
                case READ:
                    return "secondary"; // 读操作使用从库
                case WRITE:
                    return "primary";   // 写操作使用主库
                case MIXED:
                default:
                    return "primary";   // 混合操作默认使用主库
            }
        } else if (dataSource.value() == DataSource.DataSourceType.PRIMARY) {
            return "primary";
        } else if (dataSource.value() == DataSource.DataSourceType.SECONDARY) {
            return "secondary";
        }
        
        return "primary"; // 默认使用主库
    }
} 