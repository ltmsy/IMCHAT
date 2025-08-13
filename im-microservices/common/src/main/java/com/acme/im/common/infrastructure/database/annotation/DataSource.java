package com.acme.im.common.infrastructure.database.annotation;

import java.lang.annotation.*;

/**
 * 数据源选择注解
 * 用于标记方法使用的数据源类型
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSource {
    
    /**
     * 数据源类型
     */
    DataSourceType value() default DataSourceType.AUTO;
    
    /**
     * 操作类型（用于自动选择数据源）
     */
    OperationType operation() default OperationType.READ;
    
    /**
     * 数据源类型枚举
     */
    enum DataSourceType {
        /**
         * 自动选择（根据操作类型）
         */
        AUTO,
        
        /**
         * 主库
         */
        PRIMARY,
        
        /**
         * 从库
         */
        SECONDARY
    }
    
    /**
     * 操作类型枚举
     */
    enum OperationType {
        /**
         * 读操作（默认使用从库）
         */
        READ,
        
        /**
         * 写操作（默认使用主库）
         */
        WRITE,
        
        /**
         * 混合操作（根据具体实现选择）
         */
        MIXED
    }
} 