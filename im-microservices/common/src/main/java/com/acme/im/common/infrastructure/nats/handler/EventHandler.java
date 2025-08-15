package com.acme.im.common.infrastructure.nats.handler;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.lang.reflect.Method;

/**
 * 事件处理器封装类
 * 封装事件处理的目标对象、方法和配置信息
 * 
 * @author acme
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
public class EventHandler {
    
    /**
     * 处理事件的Bean对象
     */
    private Object targetBean;
    
    /**
     * 处理事件的方法
     */
    private Method targetMethod;
    
    /**
     * 事件主题
     */
    private String subject;
    
    /**
     * 处理优先级
     */
    private int priority;
    
    /**
     * 是否异步处理
     */
    private boolean async;
    
    /**
     * 执行事件处理
     * 
     * @param event 事件对象
     * @return 处理结果
     */
    public Object execute(Object event) throws Exception {
        // 设置方法可访问
        targetMethod.setAccessible(true);
        
        // 调用目标方法
        return targetMethod.invoke(targetBean, event);
    }
    
    /**
     * 获取处理器标识
     */
    public String getHandlerId() {
        return targetBean.getClass().getSimpleName() + "." + targetMethod.getName();
    }
} 