package com.acme.im.common.plugin;

/**
 * 插件信息
 * 用于外部查询插件状态和基本信息
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class PluginInfo {
    
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final String author;
    private final PluginManager.PluginState state;
    private final long loadTime;
    private final long startTime;
    private final String status;
    private final boolean healthy;
    private final String errorMessage;

    public PluginInfo(PluginManager.PluginWrapper wrapper) {
        PluginMetadata metadata = wrapper.getMetadata();
        this.id = metadata.getId();
        this.name = metadata.getName();
        this.version = metadata.getVersion();
        this.description = metadata.getDescription();
        this.author = metadata.getAuthor();
        this.state = wrapper.getState();
        this.loadTime = wrapper.getLoadTime();
        this.startTime = wrapper.getStartTime();
        
        // 获取插件状态和健康信息
        String tempStatus;
        boolean tempHealthy;
        try {
            tempStatus = wrapper.getPlugin().getStatus();
            tempHealthy = wrapper.getPlugin().isHealthy();
        } catch (Exception e) {
            tempStatus = "Error";
            tempHealthy = false;
        }
        this.status = tempStatus;
        this.healthy = tempHealthy;
        
        // 获取错误信息
        Throwable error = wrapper.getLastError();
        this.errorMessage = error != null ? error.getMessage() : null;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public PluginManager.PluginState getState() { return state; }
    public long getLoadTime() { return loadTime; }
    public long getStartTime() { return startTime; }
    public String getStatus() { return status; }
    public boolean isHealthy() { return healthy; }
    public String getErrorMessage() { return errorMessage; }

    /**
     * 获取运行时间（毫秒）
     */
    public long getUptime() {
        if (state == PluginManager.PluginState.STARTED && startTime > 0) {
            return System.currentTimeMillis() - startTime;
        }
        return 0;
    }

    /**
     * 检查插件是否正在运行
     */
    public boolean isRunning() {
        return state == PluginManager.PluginState.STARTED;
    }

    @Override
    public String toString() {
        return String.format("PluginInfo{id='%s', name='%s', version='%s', state=%s, healthy=%s}", 
                           id, name, version, state, healthy);
    }
} 