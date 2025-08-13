package com.acme.im.common.plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 插件元数据
 * 包含插件的基本信息和配置
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class PluginMetadata {
    
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final String author;
    private final String mainClass;
    private final List<String> dependencies;
    private final String jarPath;
    private final long minSystemVersion;
    private final boolean enabled;

    public PluginMetadata(String id, String name, String version, String description, 
                         String author, String mainClass, List<String> dependencies, 
                         String jarPath, long minSystemVersion, boolean enabled) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.author = author;
        this.mainClass = mainClass;
        this.dependencies = dependencies != null ? dependencies : Collections.emptyList();
        this.jarPath = jarPath;
        this.minSystemVersion = minSystemVersion;
        this.enabled = enabled;
    }

    /**
     * 从Properties创建插件元数据
     */
    public static PluginMetadata fromProperties(Properties props, String jarPath) {
        String id = props.getProperty("plugin.id");
        String name = props.getProperty("plugin.name", id);
        String version = props.getProperty("plugin.version", "1.0.0");
        String description = props.getProperty("plugin.description", "");
        String author = props.getProperty("plugin.author", "Unknown");
        String mainClass = props.getProperty("plugin.main-class");
        
        String dependenciesStr = props.getProperty("plugin.dependencies", "");
        List<String> dependencies = dependenciesStr.isEmpty() ? 
                Collections.emptyList() : 
                Arrays.asList(dependenciesStr.split(","));
        
        long minSystemVersion = Long.parseLong(props.getProperty("plugin.min-system-version", "1"));
        boolean enabled = Boolean.parseBoolean(props.getProperty("plugin.enabled", "true"));

        if (id == null || mainClass == null) {
            throw new IllegalArgumentException("插件ID和主类不能为空");
        }

        return new PluginMetadata(id, name, version, description, author, mainClass, 
                                dependencies, jarPath, minSystemVersion, enabled);
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public String getMainClass() { return mainClass; }
    public List<String> getDependencies() { return dependencies; }
    public String getJarPath() { return jarPath; }
    public long getMinSystemVersion() { return minSystemVersion; }
    public boolean isEnabled() { return enabled; }

    @Override
    public String toString() {
        return String.format("PluginMetadata{id='%s', name='%s', version='%s', author='%s'}", 
                           id, name, version, author);
    }
} 