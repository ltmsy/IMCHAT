package com.acme.im.common.infrastructure.nats.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * NATS事件处理器注解
 * 用于标记事件处理方法，支持自动注册和路由
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NatsEventHandler {

    /**
     * 事件主题，支持通配符
     * 例如：im.message.*, im.user.online, im.*.created
     */
    String value();

    /**
     * 事件类型，可选
     * 例如：REQUEST, RESPONSE, NOTIFICATION, BROADCAST
     */
    String eventType() default "";

    /**
     * 处理优先级，数字越小优先级越高
     * 默认值为0
     */
    int priority() default 0;

    /**
     * 是否异步处理
     * 默认false，同步处理
     */
    boolean async() default false;

    /**
     * 重试次数
     * 默认0，不重试
     */
    int retryCount() default 0;

    /**
     * 超时时间（毫秒）
     * 默认5000ms
     */
    long timeout() default 5000;

    /**
     * 是否启用
     * 默认true
     */
    boolean enabled() default true;

    /**
     * 描述信息
     */
    String description() default "";
} 