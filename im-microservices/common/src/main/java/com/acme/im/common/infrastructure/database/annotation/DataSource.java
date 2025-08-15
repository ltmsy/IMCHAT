package com.acme.im.common.infrastructure.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据源注解
 * 用于声明式地指定方法或类使用的数据源
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSource {
    
    /**
     * 数据源名称
     * 支持的值：
     * - "primary": 主数据源（写操作）
     * - "secondary": 从数据源（读操作）
     * - "auto": 自动选择（根据方法名或参数自动判断）
     */
    String value() default "auto";
    
    /**
     * 数据源类型
     */
    DataSourceType type() default DataSourceType.AUTO;
    
    /**
     * 是否强制使用指定数据源
     * 如果为true，则忽略自动判断逻辑
     */
    boolean force() default false;
    
    /**
     * 数据源类型枚举
     */
    enum DataSourceType {
        /**
         * 主数据源（写操作）
         */
        PRIMARY,
        
        /**
         * 从数据源（读操作）
         */
        SECONDARY,
        
        /**
         * 自动选择
         */
        AUTO
    }
} 