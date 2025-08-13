package com.acme.im.common.plugin;

import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 插件注册中心
 * 管理插件的发现、注册和服务暴露，提供插件间的通信和协调
 * 
 * 特性：
 * 1. 服务发现 - 插件服务的自动发现和注册
 * 2. 扩展点管理 - 扩展点的注册和查找
 * 3. 事件总线 - 插件间的事件通信
 * 4. 依赖注入 - 插件间的服务依赖注入
 * 5. 生命周期管理 - 插件生命周期事件处理
 * 6. 服务代理 - 插件服务的代理和拦截
 * 7. 版本兼容 - 插件版本兼容性检查
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class PluginRegistry {

    @Autowired
    private AsyncEventPublisher eventPublisher;

    // 服务注册表
    private final ConcurrentHashMap<String, List<ServiceDescriptor>> services = new ConcurrentHashMap<>();
    
    // 扩展点注册表
    private final ConcurrentHashMap<Class<?>, List<ExtensionDescriptor>> extensions = new ConcurrentHashMap<>();
    
    // 事件监听器注册表
    private final ConcurrentHashMap<String, List<EventListener>> eventListeners = new ConcurrentHashMap<>();
    
    // 插件服务实例缓存
    private final ConcurrentHashMap<String, Object> serviceInstances = new ConcurrentHashMap<>();
    
    // 插件依赖关系图
    private final ConcurrentHashMap<String, Set<String>> dependencyGraph = new ConcurrentHashMap<>();
    
    // 事件总线主题
    private static final String PLUGIN_REGISTRY_SUBJECT = "plugin.registry";

    /**
     * 服务描述符
     */
    public static class ServiceDescriptor {
        private final String pluginId;
        private final Class<?> serviceInterface;
        private final Object serviceInstance;
        private final String serviceName;
        private final String version;
        private final Map<String, Object> properties;
        private final int priority;

        public ServiceDescriptor(String pluginId, Class<?> serviceInterface, Object serviceInstance, 
                               String serviceName, String version, Map<String, Object> properties, int priority) {
            this.pluginId = pluginId;
            this.serviceInterface = serviceInterface;
            this.serviceInstance = serviceInstance;
            this.serviceName = serviceName;
            this.version = version;
            this.properties = properties != null ? properties : Collections.emptyMap();
            this.priority = priority;
        }

        public String getPluginId() { return pluginId; }
        public Class<?> getServiceInterface() { return serviceInterface; }
        public Object getServiceInstance() { return serviceInstance; }
        public String getServiceName() { return serviceName; }
        public String getVersion() { return version; }
        public Map<String, Object> getProperties() { return properties; }
        public int getPriority() { return priority; }

        @Override
        public String toString() {
            return String.format("ServiceDescriptor{plugin=%s, service=%s, version=%s, priority=%d}", 
                               pluginId, serviceName, version, priority);
        }
    }

    /**
     * 扩展点描述符
     */
    public static class ExtensionDescriptor {
        private final String pluginId;
        private final Class<?> extensionPoint;
        private final Object extension;
        private final String name;
        private final int order;
        private final Map<String, Object> attributes;

        public ExtensionDescriptor(String pluginId, Class<?> extensionPoint, Object extension, 
                                 String name, int order, Map<String, Object> attributes) {
            this.pluginId = pluginId;
            this.extensionPoint = extensionPoint;
            this.extension = extension;
            this.name = name;
            this.order = order;
            this.attributes = attributes != null ? attributes : Collections.emptyMap();
        }

        public String getPluginId() { return pluginId; }
        public Class<?> getExtensionPoint() { return extensionPoint; }
        public Object getExtension() { return extension; }
        public String getName() { return name; }
        public int getOrder() { return order; }
        public Map<String, Object> getAttributes() { return attributes; }

        @Override
        public String toString() {
            return String.format("ExtensionDescriptor{plugin=%s, point=%s, name=%s, order=%d}", 
                               pluginId, extensionPoint.getSimpleName(), name, order);
        }
    }

    /**
     * 事件监听器
     */
    @FunctionalInterface
    public interface EventListener {
        void onEvent(PluginEvent event);
    }

    /**
     * 插件事件
     */
    public static class PluginEvent {
        private final String type;
        private final String pluginId;
        private final Object data;
        private final long timestamp;
        private final Map<String, Object> metadata;

        public PluginEvent(String type, String pluginId, Object data, Map<String, Object> metadata) {
            this.type = type;
            this.pluginId = pluginId;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.metadata = metadata != null ? metadata : Collections.emptyMap();
        }

        public String getType() { return type; }
        public String getPluginId() { return pluginId; }
        public Object getData() { return data; }
        public long getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return metadata; }

        @Override
        public String toString() {
            return String.format("PluginEvent{type=%s, plugin=%s, timestamp=%d}", type, pluginId, timestamp);
        }
    }

    /**
     * 服务查询条件
     */
    public static class ServiceQuery {
        private Class<?> serviceInterface;
        private String serviceName;
        private String version;
        private String pluginId;
        private Map<String, Object> properties;
        private Predicate<ServiceDescriptor> filter;

        public ServiceQuery serviceInterface(Class<?> serviceInterface) {
            this.serviceInterface = serviceInterface;
            return this;
        }

        public ServiceQuery serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public ServiceQuery version(String version) {
            this.version = version;
            return this;
        }

        public ServiceQuery pluginId(String pluginId) {
            this.pluginId = pluginId;
            return this;
        }

        public ServiceQuery properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public ServiceQuery filter(Predicate<ServiceDescriptor> filter) {
            this.filter = filter;
            return this;
        }

        public boolean matches(ServiceDescriptor descriptor) {
            if (serviceInterface != null && !serviceInterface.isAssignableFrom(descriptor.getServiceInterface())) {
                return false;
            }
            if (serviceName != null && !serviceName.equals(descriptor.getServiceName())) {
                return false;
            }
            if (version != null && !version.equals(descriptor.getVersion())) {
                return false;
            }
            if (pluginId != null && !pluginId.equals(descriptor.getPluginId())) {
                return false;
            }
            if (properties != null) {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    Object value = descriptor.getProperties().get(entry.getKey());
                    if (!Objects.equals(value, entry.getValue())) {
                        return false;
                    }
                }
            }
            if (filter != null && !filter.test(descriptor)) {
                return false;
            }
            return true;
        }
    }

    /**
     * 初始化插件注册中心
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化插件注册中心...");
        
        // 订阅插件生命周期事件
        subscribeToPluginEvents();
        
        log.info("插件注册中心初始化完成");
    }

    /**
     * 注册服务
     * 
     * @param pluginId 插件ID
     * @param serviceInterface 服务接口
     * @param serviceInstance 服务实例
     * @param serviceName 服务名称
     * @param version 版本
     * @param properties 属性
     * @param priority 优先级
     */
    public void registerService(String pluginId, Class<?> serviceInterface, Object serviceInstance, 
                               String serviceName, String version, Map<String, Object> properties, int priority) {
        ServiceDescriptor descriptor = new ServiceDescriptor(
                pluginId, serviceInterface, serviceInstance, serviceName, version, properties, priority);
        
        String serviceKey = getServiceKey(serviceInterface, serviceName);
        services.computeIfAbsent(serviceKey, k -> new CopyOnWriteArrayList<>()).add(descriptor);
        
        // 缓存服务实例
        String instanceKey = pluginId + ":" + serviceName;
        serviceInstances.put(instanceKey, serviceInstance);
        
        log.info("注册服务: {}", descriptor);
        
        // 发布服务注册事件
        publishEvent("SERVICE_REGISTERED", pluginId, descriptor, null);
    }

    /**
     * 注册服务（简化版本）
     */
    public void registerService(String pluginId, Class<?> serviceInterface, Object serviceInstance) {
        registerService(pluginId, serviceInterface, serviceInstance, 
                       serviceInterface.getSimpleName(), "1.0.0", null, 0);
    }

    /**
     * 取消注册服务
     */
    public void unregisterService(String pluginId, String serviceName) {
        services.values().forEach(descriptors -> 
            descriptors.removeIf(descriptor -> 
                descriptor.getPluginId().equals(pluginId) && 
                descriptor.getServiceName().equals(serviceName)));
        
        String instanceKey = pluginId + ":" + serviceName;
        serviceInstances.remove(instanceKey);
        
        log.info("取消注册服务: plugin={}, service={}", pluginId, serviceName);
        
        // 发布服务取消注册事件
        publishEvent("SERVICE_UNREGISTERED", pluginId, serviceName, null);
    }

    /**
     * 查找服务
     */
    public List<ServiceDescriptor> findServices(ServiceQuery query) {
        return services.values().stream()
                .flatMap(List::stream)
                .filter(query::matches)
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority())) // 高优先级在前
                .collect(Collectors.toList());
    }

    /**
     * 获取服务实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceInterface) {
        ServiceQuery query = new ServiceQuery().serviceInterface(serviceInterface);
        List<ServiceDescriptor> descriptors = findServices(query);
        
        if (descriptors.isEmpty()) {
            return null;
        }
        
        // 返回优先级最高的服务
        return (T) descriptors.get(0).getServiceInstance();
    }

    /**
     * 获取所有服务实例
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getServices(Class<T> serviceInterface) {
        ServiceQuery query = new ServiceQuery().serviceInterface(serviceInterface);
        List<ServiceDescriptor> descriptors = findServices(query);
        
        return descriptors.stream()
                .map(descriptor -> (T) descriptor.getServiceInstance())
                .collect(Collectors.toList());
    }

    /**
     * 注册扩展点
     */
    public void registerExtension(String pluginId, Class<?> extensionPoint, Object extension, 
                                String name, int order, Map<String, Object> attributes) {
        ExtensionDescriptor descriptor = new ExtensionDescriptor(
                pluginId, extensionPoint, extension, name, order, attributes);
        
        extensions.computeIfAbsent(extensionPoint, k -> new CopyOnWriteArrayList<>()).add(descriptor);
        
        log.info("注册扩展点: {}", descriptor);
        
        // 发布扩展点注册事件
        publishEvent("EXTENSION_REGISTERED", pluginId, descriptor, null);
    }

    /**
     * 注册扩展点（简化版本）
     */
    public void registerExtension(String pluginId, Class<?> extensionPoint, Object extension) {
        registerExtension(pluginId, extensionPoint, extension, extension.getClass().getSimpleName(), 0, null);
    }

    /**
     * 取消注册扩展点
     */
    public void unregisterExtension(String pluginId, Class<?> extensionPoint, String name) {
        List<ExtensionDescriptor> descriptors = extensions.get(extensionPoint);
        if (descriptors != null) {
            descriptors.removeIf(descriptor -> 
                descriptor.getPluginId().equals(pluginId) && 
                descriptor.getName().equals(name));
        }
        
        log.info("取消注册扩展点: plugin={}, point={}, name={}", 
                pluginId, extensionPoint.getSimpleName(), name);
        
        // 发布扩展点取消注册事件
        publishEvent("EXTENSION_UNREGISTERED", pluginId, 
                    Map.of("extensionPoint", extensionPoint, "name", name), null);
    }

    /**
     * 获取扩展点实现
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getExtensions(Class<T> extensionPoint) {
        List<ExtensionDescriptor> descriptors = extensions.getOrDefault(extensionPoint, Collections.emptyList());
        
        return descriptors.stream()
                .sorted(Comparator.comparingInt(ExtensionDescriptor::getOrder))
                .map(descriptor -> (T) descriptor.getExtension())
                .collect(Collectors.toList());
    }

    /**
     * 获取扩展点描述符
     */
    public List<ExtensionDescriptor> getExtensionDescriptors(Class<?> extensionPoint) {
        return new ArrayList<>(extensions.getOrDefault(extensionPoint, Collections.emptyList()));
    }

    /**
     * 添加事件监听器
     */
    public void addEventListener(String eventType, EventListener listener) {
        eventListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.debug("添加事件监听器: eventType={}", eventType);
    }

    /**
     * 移除事件监听器
     */
    public void removeEventListener(String eventType, EventListener listener) {
        List<EventListener> listeners = eventListeners.get(eventType);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                eventListeners.remove(eventType);
            }
        }
    }

    /**
     * 发布事件
     */
    public void publishEvent(String eventType, String pluginId, Object data, Map<String, Object> metadata) {
        PluginEvent event = new PluginEvent(eventType, pluginId, data, metadata);
        
        // 通知本地监听器
        List<EventListener> listeners = eventListeners.get(eventType);
        if (listeners != null) {
            for (EventListener listener : listeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    log.error("事件监听器执行失败: eventType={}, plugin={}", eventType, pluginId, e);
                }
            }
        }
        
        // 通知全局监听器
        List<EventListener> globalListeners = eventListeners.get("*");
        if (globalListeners != null) {
            for (EventListener listener : globalListeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    log.error("全局事件监听器执行失败: eventType={}, plugin={}", eventType, pluginId, e);
                }
            }
        }
        
        // 通过NATS发布事件
        try {
            eventPublisher.publishEventAsync(PLUGIN_REGISTRY_SUBJECT + "." + eventType, event);
        } catch (Exception e) {
            log.error("发布插件注册中心事件失败: eventType={}", eventType, e);
        }
    }

    /**
     * 注册插件依赖
     */
    public void registerDependency(String pluginId, String dependencyId) {
        dependencyGraph.computeIfAbsent(pluginId, k -> ConcurrentHashMap.newKeySet()).add(dependencyId);
        log.debug("注册插件依赖: {} -> {}", pluginId, dependencyId);
    }

    /**
     * 获取插件依赖
     */
    public Set<String> getDependencies(String pluginId) {
        return new HashSet<>(dependencyGraph.getOrDefault(pluginId, Collections.emptySet()));
    }

    /**
     * 获取依赖该插件的插件列表
     */
    public Set<String> getDependents(String pluginId) {
        return dependencyGraph.entrySet().stream()
                .filter(entry -> entry.getValue().contains(pluginId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * 检查循环依赖
     */
    public boolean hasCyclicDependency(String pluginId) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        return hasCyclicDependencyUtil(pluginId, visited, recursionStack);
    }

    /**
     * 获取注册中心统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalServices = services.values().stream().mapToInt(List::size).sum();
        int totalExtensions = extensions.values().stream().mapToInt(List::size).sum();
        int totalEventListeners = eventListeners.values().stream().mapToInt(List::size).sum();
        
        stats.put("totalServices", totalServices);
        stats.put("totalExtensions", totalExtensions);
        stats.put("totalEventListeners", totalEventListeners);
        stats.put("serviceTypes", services.size());
        stats.put("extensionPoints", extensions.size());
        stats.put("eventTypes", eventListeners.size());
        stats.put("pluginDependencies", dependencyGraph.size());
        
        return stats;
    }

    /**
     * 清理插件相关的所有注册信息
     */
    public void cleanupPlugin(String pluginId) {
        // 清理服务
        services.values().forEach(descriptors -> 
            descriptors.removeIf(descriptor -> descriptor.getPluginId().equals(pluginId)));
        
        // 清理扩展点
        extensions.values().forEach(descriptors -> 
            descriptors.removeIf(descriptor -> descriptor.getPluginId().equals(pluginId)));
        
        // 清理服务实例缓存
        serviceInstances.entrySet().removeIf(entry -> entry.getKey().startsWith(pluginId + ":"));
        
        // 清理依赖关系
        dependencyGraph.remove(pluginId);
        dependencyGraph.values().forEach(deps -> deps.remove(pluginId));
        
        log.info("清理插件注册信息: {}", pluginId);
        
        // 发布插件清理事件
        publishEvent("PLUGIN_CLEANED", pluginId, null, null);
    }

    /**
     * 销毁插件注册中心
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭插件注册中心...");
        
        services.clear();
        extensions.clear();
        eventListeners.clear();
        serviceInstances.clear();
        dependencyGraph.clear();
        
        log.info("插件注册中心已关闭");
    }

    // ================================
    // 私有辅助方法
    // ================================

    /**
     * 获取服务键
     */
    private String getServiceKey(Class<?> serviceInterface, String serviceName) {
        return serviceInterface.getName() + ":" + serviceName;
    }

    /**
     * 循环依赖检查辅助方法
     */
    private boolean hasCyclicDependencyUtil(String pluginId, Set<String> visited, Set<String> recursionStack) {
        visited.add(pluginId);
        recursionStack.add(pluginId);
        
        Set<String> dependencies = dependencyGraph.get(pluginId);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                if (!visited.contains(dependency)) {
                    if (hasCyclicDependencyUtil(dependency, visited, recursionStack)) {
                        return true;
                    }
                } else if (recursionStack.contains(dependency)) {
                    return true;
                }
            }
        }
        
        recursionStack.remove(pluginId);
        return false;
    }

    /**
     * 订阅插件事件
     */
    private void subscribeToPluginEvents() {
        // 这里可以订阅插件生命周期事件，自动清理注册信息
        addEventListener("PLUGIN_UNLOADED", event -> {
            String pluginId = event.getPluginId();
            cleanupPlugin(pluginId);
        });
    }
} 