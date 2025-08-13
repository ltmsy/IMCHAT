package com.acme.im.common.plugin;

/**
 * 插件基础接口
 * 定义插件的标准生命周期方法
 * 
 * 生命周期：
 * 1. initialize() - 插件初始化
 * 2. start() - 插件启动
 * 3. stop() - 插件停止
 * 4. destroy() - 插件销毁
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public interface Plugin {

    /**
     * 插件初始化
     * 在插件加载后立即调用，用于初始化插件资源
     * 
     * @param context 插件上下文
     * @throws PluginException 初始化异常
     */
    void initialize(PluginContext context) throws PluginException;

    /**
     * 插件启动
     * 在插件初始化成功后调用，开始提供服务
     * 
     * @throws PluginException 启动异常
     */
    void start() throws PluginException;

    /**
     * 插件停止
     * 停止插件服务，但保留资源
     * 
     * @throws PluginException 停止异常
     */
    void stop() throws PluginException;

    /**
     * 插件销毁
     * 清理插件资源，释放内存
     * 
     * @throws PluginException 销毁异常
     */
    void destroy() throws PluginException;

    /**
     * 获取插件状态
     * 
     * @return 插件状态描述
     */
    default String getStatus() {
        return "Unknown";
    }

    /**
     * 获取插件健康状态
     * 
     * @return true表示健康，false表示异常
     */
    default boolean isHealthy() {
        return true;
    }
} 