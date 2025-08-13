package com.acme.im.common.plugin;

import org.springframework.context.ApplicationContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件上下文
 * 提供插件运行时的环境信息和服务访问
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class PluginContext {
    
    private final PluginMetadata metadata;
    private final ApplicationContext applicationContext;
    private final PluginManager pluginManager;
    private final Map<String, Object> attributes;

    public PluginContext(PluginMetadata metadata, ApplicationContext applicationContext, PluginManager pluginManager) {
        this.metadata = metadata;
        this.applicationContext = applicationContext;
        this.pluginManager = pluginManager;
        this.attributes = new ConcurrentHashMap<>();
    }

    /**
     * 获取插件元数据
     */
    public PluginMetadata getMetadata() {
        return metadata;
    }

    /**
     * 获取Spring应用上下文
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 获取插件管理器
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * 获取Spring Bean
     */
    public <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    /**
     * 获取Spring Bean
     */
    public Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }

    /**
     * 设置属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 移除属性
     */
    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }

    /**
     * 检查属性是否存在
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * 获取所有属性
     */
    public Map<String, Object> getAttributes() {
        return new ConcurrentHashMap<>(attributes);
    }

    /**
     * 注册扩展点
     */
    public <T> void registerExtension(Class<T> extensionPoint, T extension) {
        pluginManager.registerExtension(extensionPoint, extension);
    }

    /**
     * 获取扩展点实现
     */
    public <T> java.util.List<T> getExtensions(Class<T> extensionPoint) {
        return pluginManager.getExtensions(extensionPoint);
    }

    /**
     * 获取其他插件
     */
    public PluginInfo getPlugin(String pluginId) {
        return pluginManager.getPluginInfo(pluginId);
    }

    /**
     * 获取所有插件
     */
    public java.util.List<PluginInfo> getAllPlugins() {
        return pluginManager.getAllPlugins();
    }
} 