package com.acme.im.business.module.common.event;

import com.acme.im.common.infrastructure.nats.dto.BaseEvent;

/**
 * 业务层事件处理器接口
 * 定义所有需要处理的事件类型和处理方法
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public interface EventProcessor {

    /**
     * 获取处理器名称
     */
    String getProcessorName();

    /**
     * 获取支持的事件主题
     */
    String getSupportedSubject();

    /**
     * 处理事件
     * 
     * @param event 事件对象
     * @return 处理结果，如果需要响应则返回响应事件，否则返回null
     */
    BaseEvent<?> processEvent(BaseEvent<?> event);

    /**
     * 获取处理器优先级（数值越小优先级越高）
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 检查是否支持该事件
     * 
     * @param event 事件对象
     * @return 是否支持
     */
    default boolean supportsEvent(BaseEvent<?> event) {
        return getSupportedSubject().equals(event.getSubject());
    }
} 