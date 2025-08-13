package com.acme.im.common.infrastructure.database.annotation;

import java.lang.annotation.*;

/**
 * 只读数据源注解
 * 标记使用从库的方法，用于读操作
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnly {
    
    /**
     * 数据源名称，默认为从库
     */
    String value() default "secondary";
    
    /**
     * 是否强制使用指定数据源
     */
    boolean force() default false;
} 