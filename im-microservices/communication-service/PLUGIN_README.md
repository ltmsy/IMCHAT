# IM通信层插件系统

## 🚀 概述

IM通信层插件系统是一个基于公共模块扩展点管理器的可插拔架构，为通信服务提供了灵活、可扩展的功能扩展能力。

## ✨ 核心特性

- **可插拔架构** - 支持动态加载和卸载插件
- **扩展点管理** - 统一的扩展点定义和执行机制
- **钩子机制** - 支持前置、后置、环绕、异常等钩子类型
- **优先级控制** - 插件执行优先级管理
- **热插拔** - 运行时动态启用/禁用插件
- **类型安全** - 强类型的插件接口定义

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                    通信层插件系统架构                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐ │
│  │   扩展点管理器    │    │   插件注册中心    │    │  插件管理器  │ │
│  │                 │    │                 │    │             │ │
│  │ • 扩展点定义     │    │ • 服务发现      │    │ • 生命周期   │ │
│  │ • 钩子执行      │    │ • 服务注册      │    │ • 依赖管理   │ │
│  │ • 执行策略      │    │ • 事件总线      │    │ • 版本控制   │ │
│  │ • 结果聚合      │    │ • 服务代理      │    │ • 热更新     │ │
│  └─────────────────┘    └─────────────────┘    └─────────────┘ │
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐ │
│  │   消息验证插件   │    │   连接认证插件   │    │  消息路由插件 │ │
│  │                 │    │                 │    │             │ │
│  │ • 内容审核      │    │ • JWT验证       │    │ • 智能路由   │ │
│  │ • 敏感词检测    │    │ • OAuth2认证    │    │ • 负载均衡   │ │
│  │ • 格式验证      │    │ • 多因子认证    │    │ • 故障转移   │ │
│  │ • 长度检查      │    │ • 设备认证      │    │ • 地理位置   │ │
│  └─────────────────┘    └─────────────────┘    └─────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 🔌 已实现的插件

### 1. 消息验证插件 (MessageValidator)

**功能**：消息内容验证、敏感词检测、格式检查等

**特性**：
- 消息长度限制（默认1000字符）
- 敏感词检测和过滤
- 内容格式验证
- 可配置的验证规则

**使用场景**：
- 内容审核
- 垃圾消息过滤
- 合规性检查

### 2. 连接认证插件 (ConnectionAuthenticator)

**功能**：用户身份认证、设备认证、权限管理等

**特性**：
- JWT Token认证
- 用户名密码认证
- OAuth2认证
- 多因子认证支持
- 设备指纹识别

**使用场景**：
- 用户登录认证
- 设备管理
- 权限控制
- 安全审计

### 3. 消息路由插件 (MessageRouter)

**功能**：消息智能路由、负载均衡、故障转移等

**特性**：
- 基于消息类型的智能路由
- 负载均衡策略
- 故障转移机制
- 性能优化路由

**使用场景**：
- 消息分发
- 服务负载均衡
- 高可用性保障
- 性能优化

## 🎯 扩展点定义

### 消息处理扩展点

```java
// 消息验证扩展点
"message.validate" - 消息验证扩展点，支持自定义消息验证规则

// 消息过滤扩展点  
"message.filter" - 消息过滤扩展点，支持自定义消息过滤逻辑

// 消息处理扩展点
"message.process" - 消息处理扩展点，支持自定义消息处理逻辑

// 消息路由扩展点
"message.route" - 消息路由扩展点，支持自定义消息路由逻辑

// 连接认证扩展点
"connection.authenticate" - 连接认证扩展点，支持自定义认证逻辑
```

### WebSocket钩子扩展点

```java
// WebSocket消息处理钩子
"websocket.message.before.process" - 消息处理前钩子
"websocket.message.after.process" - 消息处理后钩子  
"websocket.message.on.error" - 消息处理异常钩子
```

## 🚀 快速开始

### 1. 创建自定义插件

```java
@Component
public class CustomMessageValidator implements MessageValidator {
    
    @Override
    public ValidationResult validateMessage(Object message, ValidationContext context) {
        // 自定义验证逻辑
        if (containsCustomRule(message)) {
            return ValidationResult.failure("custom_validation", "违反自定义规则");
        }
        return ValidationResult.success("custom_validation");
    }
    
    @Override
    public String getValidatorName() {
        return "CustomMessageValidator";
    }
    
    @Override
    public int getPriority() {
        return 50; // 高优先级
    }
    
    @Override
    public boolean supportsMessageType(String messageType) {
        return "text".equals(messageType);
    }
    
    @Override
    public List<String> getSupportedValidationTypes() {
        return List.of("custom_validation");
    }
}
```

### 2. 注册扩展点

```java
@Configuration
public class CustomPluginConfig {
    
    @Autowired
    private ExtensionPointManager extensionPointManager;
    
    @PostConstruct
    public void registerCustomExtensionPoints() {
        extensionPointManager.defineExtensionPoint(
            "custom.validation",
            "自定义验证扩展点",
            CustomValidator.class,
            ExecutionStrategy.ALL,
            false,
            50,
            Map.of("category", "custom", "version", "1.0")
        );
    }
}
```

### 3. 使用扩展点

```java
@Service
public class MessageService {
    
    @Autowired
    private ExtensionPointManager extensionPointManager;
    
    public void processMessage(Message message) {
        // 执行自定义验证扩展点
        extensionPointManager.executeExtensionPoint("custom.validation", 
            new Object[]{message, createContext()});
    }
}
```

## 🧪 测试

### 运行单元测试

```bash
mvn test -Dtest=PluginUnitTest
```

### 运行演示程序

```bash
mvn test-compile
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
     com.acme.im.communication.plugin.PluginDemo
```

## 📊 性能特性

- **异步执行** - 支持异步扩展点执行
- **批量处理** - 支持批量消息处理
- **缓存机制** - 插件结果缓存
- **监控统计** - 扩展点执行性能统计
- **熔断保护** - 插件执行异常保护

## 🔧 配置说明

### 插件配置

```yaml
# application.yml
plugin:
  system:
    enabled: true
    auto-discovery: true
    hot-reload: true
    max-execution-time: 5000ms
    circuit-breaker:
      enabled: true
      failure-threshold: 5
      recovery-timeout: 30s
```

### 扩展点配置

```yaml
# 扩展点优先级配置
extension-points:
  message.validate:
    default-priority: 100
    execution-strategy: ALL
    required: false
  connection.authenticate:
    default-priority: 200
    execution-strategy: UNTIL_SUCCESS
    required: true
```

## 🚀 部署说明

### 1. 开发环境

```bash
# 克隆项目
git clone <repository-url>
cd im-microservices/communication-service

# 编译项目
mvn clean compile

# 运行测试
mvn test

# 启动服务
mvn spring-boot:run
```

### 2. 生产环境

```bash
# 打包
mvn clean package -DskipTests

# 运行
java -jar target/communication-service-1.0.0-SNAPSHOT.jar
```

## 🔍 监控和调试

### 扩展点统计

```java
@RestController
public class PluginMonitorController {
    
    @Autowired
    private ExtensionPointManager extensionPointManager;
    
    @GetMapping("/plugin/stats")
    public Map<String, Object> getPluginStats() {
        return extensionPointManager.getAllExecutionStats();
    }
}
```

### 日志配置

```yaml
# logback-spring.xml
<logger name="com.acme.im.common.plugin" level="DEBUG"/>
<logger name="com.acme.im.communication.plugin" level="DEBUG"/>
```

## 🤝 贡献指南

### 开发新插件

1. 实现相应的插件接口
2. 添加单元测试
3. 更新文档
4. 提交Pull Request

### 代码规范

- 遵循Java编码规范
- 添加完整的JavaDoc注释
- 确保测试覆盖率 > 80%
- 使用Lombok简化代码

## 📚 相关文档

- [公共模块插件系统](../common/README.md)
- [扩展点管理器API文档](../common/src/main/java/com/acme/im/common/plugin/ExtensionPointManager.java)
- [插件注册中心API文档](../common/src/main/java/com/acme/im/common/plugin/PluginRegistry.java)

## 🆘 常见问题

### Q: 如何添加新的扩展点类型？
A: 在`CommunicationPluginConfig`中定义新的扩展点，实现相应的接口，然后注册到扩展点管理器。

### Q: 插件执行失败怎么办？
A: 检查插件日志，确认插件配置正确，验证扩展点是否正确注册。

### Q: 如何提高插件执行性能？
A: 使用异步执行、结果缓存、批量处理等优化策略。

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情。

## 👥 团队

- **架构设计** - IM开发团队
- **插件开发** - IM开发团队
- **测试验证** - IM开发团队

---

**版本**: 1.0.0  
**最后更新**: 2025-08-13  
**维护者**: IM开发团队 