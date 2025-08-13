package com.acme.im.common.plugin;

import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 插件管理器
 * 提供完整的插件生命周期管理，支持动态加载、卸载和热更新
 * 
 * 特性：
 * 1. 动态加载 - 运行时加载和卸载插件
 * 2. 生命周期管理 - 插件的初始化、启动、停止、销毁
 * 3. 依赖管理 - 插件间依赖关系处理
 * 4. 版本控制 - 插件版本管理和兼容性检查
 * 5. 热更新 - 支持插件热更新
 * 6. 事件通知 - 插件状态变化事件通知
 * 7. 安全隔离 - 插件类加载器隔离
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class PluginManager {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private AsyncEventPublisher eventPublisher;

    // 插件注册表
    private final ConcurrentHashMap<String, PluginWrapper> plugins = new ConcurrentHashMap<>();
    
    // 插件类加载器缓存
    private final ConcurrentHashMap<String, URLClassLoader> classLoaders = new ConcurrentHashMap<>();
    
    // 扩展点注册表
    private final ConcurrentHashMap<Class<?>, List<Object>> extensionPoints = new ConcurrentHashMap<>();
    
    // 读写锁，保护插件操作
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // 插件配置
    private static final String PLUGIN_DIR = "plugins";
    private static final String PLUGIN_MANIFEST = "META-INF/plugin.properties";
    private static final String PLUGIN_EVENT_SUBJECT = "plugin.lifecycle";

    /**
     * 插件包装器
     */
    public static class PluginWrapper {
        private final PluginMetadata metadata;
        private final Plugin plugin;
        private final URLClassLoader classLoader;
        private volatile PluginState state;
        private final long loadTime;
        private volatile long startTime;
        private volatile Throwable lastError;

        public PluginWrapper(PluginMetadata metadata, Plugin plugin, URLClassLoader classLoader) {
            this.metadata = metadata;
            this.plugin = plugin;
            this.classLoader = classLoader;
            this.state = PluginState.LOADED;
            this.loadTime = System.currentTimeMillis();
        }

        // Getters
        public PluginMetadata getMetadata() { return metadata; }
        public Plugin getPlugin() { return plugin; }
        public URLClassLoader getClassLoader() { return classLoader; }
        public PluginState getState() { return state; }
        public long getLoadTime() { return loadTime; }
        public long getStartTime() { return startTime; }
        public Throwable getLastError() { return lastError; }

        // State management
        public void setState(PluginState state) { 
            this.state = state; 
            if (state == PluginState.STARTED) {
                this.startTime = System.currentTimeMillis();
            }
        }
        
        public void setLastError(Throwable error) { this.lastError = error; }
    }

    /**
     * 插件状态
     */
    public enum PluginState {
        LOADED,     // 已加载
        STARTED,    // 已启动
        STOPPED,    // 已停止
        ERROR,      // 错误状态
        UNLOADED    // 已卸载
    }

    /**
     * 插件生命周期事件
     */
    public static class PluginLifecycleEvent {
        private final String pluginId;
        private final PluginState state;
        private final long timestamp;
        private final String message;

        public PluginLifecycleEvent(String pluginId, PluginState state, String message) {
            this.pluginId = pluginId;
            this.state = state;
            this.timestamp = System.currentTimeMillis();
            this.message = message;
        }

        public String getPluginId() { return pluginId; }
        public PluginState getState() { return state; }
        public long getTimestamp() { return timestamp; }
        public String getMessage() { return message; }
    }

    /**
     * 初始化插件管理器
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化插件管理器...");
        
        // 创建插件目录
        File pluginDir = new File(PLUGIN_DIR);
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
            log.info("创建插件目录: {}", pluginDir.getAbsolutePath());
        }

        // 扫描并加载插件
        scanAndLoadPlugins();
        
        log.info("插件管理器初始化完成: 已加载插件数={}", plugins.size());
    }

    /**
     * 扫描并加载插件
     */
    public void scanAndLoadPlugins() {
        File pluginDir = new File(PLUGIN_DIR);
        if (!pluginDir.exists() || !pluginDir.isDirectory()) {
            log.warn("插件目录不存在: {}", pluginDir.getAbsolutePath());
            return;
        }

        File[] jarFiles = pluginDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            log.info("未发现插件JAR文件");
            return;
        }

        log.info("发现插件文件: {}", jarFiles.length);
        
        for (File jarFile : jarFiles) {
            try {
                loadPlugin(jarFile);
            } catch (Exception e) {
                log.error("加载插件失败: {}", jarFile.getName(), e);
            }
        }
    }

    /**
     * 加载单个插件
     */
    public boolean loadPlugin(File jarFile) {
        lock.writeLock().lock();
        try {
            log.info("开始加载插件: {}", jarFile.getName());

            // 创建类加载器
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    this.getClass().getClassLoader()
            );

            // 读取插件元数据
            PluginMetadata metadata = readPluginMetadata(jarFile, classLoader);
            if (metadata == null) {
                log.error("无法读取插件元数据: {}", jarFile.getName());
                return false;
            }

            // 检查插件是否已存在
            if (plugins.containsKey(metadata.getId())) {
                log.warn("插件已存在，跳过加载: {}", metadata.getId());
                return false;
            }

            // 检查依赖
            if (!checkDependencies(metadata)) {
                log.error("插件依赖检查失败: {}", metadata.getId());
                return false;
            }

            // 实例化插件主类
            Class<?> pluginClass = classLoader.loadClass(metadata.getMainClass());
            Plugin plugin = (Plugin) pluginClass.getDeclaredConstructor().newInstance();

            // 创建插件包装器
            PluginWrapper wrapper = new PluginWrapper(metadata, plugin, classLoader);
            
            // 注册插件
            plugins.put(metadata.getId(), wrapper);
            classLoaders.put(metadata.getId(), classLoader);

            // 初始化插件
            try {
                PluginContext context = createPluginContext(metadata);
                plugin.initialize(context);
                
                log.info("插件加载成功: {} v{}", metadata.getId(), metadata.getVersion());
                publishLifecycleEvent(metadata.getId(), PluginState.LOADED, "插件加载成功");
                
                return true;
                
            } catch (Exception e) {
                log.error("插件初始化失败: {}", metadata.getId(), e);
                wrapper.setState(PluginState.ERROR);
                wrapper.setLastError(e);
                return false;
            }

        } catch (Exception e) {
            log.error("加载插件异常: {}", jarFile.getName(), e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 启动插件
     */
    public boolean startPlugin(String pluginId) {
        lock.readLock().lock();
        try {
            PluginWrapper wrapper = plugins.get(pluginId);
            if (wrapper == null) {
                log.warn("插件不存在: {}", pluginId);
                return false;
            }

            if (wrapper.getState() == PluginState.STARTED) {
                log.debug("插件已启动: {}", pluginId);
                return true;
            }

            try {
                wrapper.getPlugin().start();
                wrapper.setState(PluginState.STARTED);
                
                log.info("插件启动成功: {}", pluginId);
                publishLifecycleEvent(pluginId, PluginState.STARTED, "插件启动成功");
                
                return true;
                
            } catch (Exception e) {
                log.error("插件启动失败: {}", pluginId, e);
                wrapper.setState(PluginState.ERROR);
                wrapper.setLastError(e);
                return false;
            }

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 停止插件
     */
    public boolean stopPlugin(String pluginId) {
        lock.readLock().lock();
        try {
            PluginWrapper wrapper = plugins.get(pluginId);
            if (wrapper == null) {
                log.warn("插件不存在: {}", pluginId);
                return false;
            }

            if (wrapper.getState() == PluginState.STOPPED) {
                log.debug("插件已停止: {}", pluginId);
                return true;
            }

            try {
                wrapper.getPlugin().stop();
                wrapper.setState(PluginState.STOPPED);
                
                log.info("插件停止成功: {}", pluginId);
                publishLifecycleEvent(pluginId, PluginState.STOPPED, "插件停止成功");
                
                return true;
                
            } catch (Exception e) {
                log.error("插件停止失败: {}", pluginId, e);
                wrapper.setState(PluginState.ERROR);
                wrapper.setLastError(e);
                return false;
            }

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 卸载插件
     */
    public boolean unloadPlugin(String pluginId) {
        lock.writeLock().lock();
        try {
            PluginWrapper wrapper = plugins.get(pluginId);
            if (wrapper == null) {
                log.warn("插件不存在: {}", pluginId);
                return false;
            }

            // 先停止插件
            if (wrapper.getState() == PluginState.STARTED) {
                stopPlugin(pluginId);
            }

            try {
                // 销毁插件
                wrapper.getPlugin().destroy();
                
                // 关闭类加载器
                URLClassLoader classLoader = classLoaders.remove(pluginId);
                if (classLoader != null) {
                    classLoader.close();
                }
                
                // 移除插件
                plugins.remove(pluginId);
                wrapper.setState(PluginState.UNLOADED);
                
                log.info("插件卸载成功: {}", pluginId);
                publishLifecycleEvent(pluginId, PluginState.UNLOADED, "插件卸载成功");
                
                return true;
                
            } catch (Exception e) {
                log.error("插件卸载失败: {}", pluginId, e);
                wrapper.setState(PluginState.ERROR);
                wrapper.setLastError(e);
                return false;
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 重新加载插件
     */
    public boolean reloadPlugin(String pluginId) {
        lock.writeLock().lock();
        try {
            PluginWrapper wrapper = plugins.get(pluginId);
            if (wrapper == null) {
                log.warn("插件不存在: {}", pluginId);
                return false;
            }

            // 获取插件文件路径
            String jarPath = wrapper.getMetadata().getJarPath();
            File jarFile = new File(jarPath);
            
            // 卸载旧插件
            unloadPlugin(pluginId);
            
            // 重新加载插件
            return loadPlugin(jarFile);
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取所有插件信息
     */
    public List<PluginInfo> getAllPlugins() {
        lock.readLock().lock();
        try {
            List<PluginInfo> result = new ArrayList<>();
            for (PluginWrapper wrapper : plugins.values()) {
                result.add(new PluginInfo(wrapper));
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取插件信息
     */
    public PluginInfo getPluginInfo(String pluginId) {
        lock.readLock().lock();
        try {
            PluginWrapper wrapper = plugins.get(pluginId);
            return wrapper != null ? new PluginInfo(wrapper) : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 注册扩展点
     */
    @SuppressWarnings("unchecked")
    public <T> void registerExtension(Class<T> extensionPoint, T extension) {
        extensionPoints.computeIfAbsent(extensionPoint, k -> new ArrayList<>()).add(extension);
        log.debug("注册扩展点: {} -> {}", extensionPoint.getSimpleName(), extension.getClass().getSimpleName());
    }

    /**
     * 获取扩展点实现
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getExtensions(Class<T> extensionPoint) {
        List<Object> extensions = extensionPoints.getOrDefault(extensionPoint, Collections.emptyList());
        List<T> result = new ArrayList<>();
        for (Object extension : extensions) {
            if (extensionPoint.isInstance(extension)) {
                result.add((T) extension);
            }
        }
        return result;
    }

    /**
     * 销毁插件管理器
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭插件管理器...");
        
        lock.writeLock().lock();
        try {
            // 停止所有插件
            for (String pluginId : new ArrayList<>(plugins.keySet())) {
                try {
                    unloadPlugin(pluginId);
                } catch (Exception e) {
                    log.error("卸载插件失败: {}", pluginId, e);
                }
            }
            
            // 关闭所有类加载器
            for (URLClassLoader classLoader : classLoaders.values()) {
                try {
                    classLoader.close();
                } catch (Exception e) {
                    log.warn("关闭类加载器失败", e);
                }
            }
            
            plugins.clear();
            classLoaders.clear();
            extensionPoints.clear();
            
        } finally {
            lock.writeLock().unlock();
        }
        
        log.info("插件管理器已关闭");
    }

    // ================================
    // 私有辅助方法
    // ================================

    /**
     * 读取插件元数据
     */
    private PluginMetadata readPluginMetadata(File jarFile, URLClassLoader classLoader) {
        try (JarFile jar = new JarFile(jarFile)) {
            Properties props = new Properties();
            
            // 尝试从plugin.properties读取
            var entry = jar.getEntry(PLUGIN_MANIFEST);
            if (entry != null) {
                props.load(jar.getInputStream(entry));
            } else {
                // 尝试从MANIFEST.MF读取
                Manifest manifest = jar.getManifest();
                if (manifest != null) {
                    var attributes = manifest.getMainAttributes();
                    attributes.forEach((key, value) -> props.setProperty(key.toString(), value.toString()));
                }
            }

            if (props.isEmpty()) {
                log.error("插件元数据为空: {}", jarFile.getName());
                return null;
            }

            return PluginMetadata.fromProperties(props, jarFile.getAbsolutePath());
            
        } catch (Exception e) {
            log.error("读取插件元数据失败: {}", jarFile.getName(), e);
            return null;
        }
    }

    /**
     * 检查插件依赖
     */
    private boolean checkDependencies(PluginMetadata metadata) {
        List<String> dependencies = metadata.getDependencies();
        if (dependencies.isEmpty()) {
            return true;
        }

        for (String dependency : dependencies) {
            if (!plugins.containsKey(dependency)) {
                log.error("缺少依赖插件: {} -> {}", metadata.getId(), dependency);
                return false;
            }
        }

        return true;
    }

    /**
     * 创建插件上下文
     */
    private PluginContext createPluginContext(PluginMetadata metadata) {
        return new PluginContext(metadata, applicationContext, this);
    }

    /**
     * 发布生命周期事件
     */
    private void publishLifecycleEvent(String pluginId, PluginState state, String message) {
        try {
            PluginLifecycleEvent event = new PluginLifecycleEvent(pluginId, state, message);
            eventPublisher.publishEventAsync(PLUGIN_EVENT_SUBJECT, event);
        } catch (Exception e) {
            log.warn("发布插件生命周期事件失败: pluginId={}, state={}", pluginId, state, e);
        }
    }
} 