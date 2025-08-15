# NATS事件管理系统

## 概述

NATS事件管理系统为业务层和通信层之间的通信提供了统一的、类型安全的事件处理机制。系统包含事件常量定义、统一的事件数据结构、事件管理器等核心组件。

## 架构设计

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   通信层        │    │     NATS        │    │   业务层        │
│                 │    │                  │    │                 │
│ WebSocket       │───▶│ 事件发布/订阅   │───▶│ 用户服务       │
│ 消息处理器      │    │                  │    │ 消息服务       │
│                 │    │                  │    │ 会话服务       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 核心组件

### 1. NatsEventConstants - 事件常量

统一管理所有事件主题和类型，避免硬编码。

```java
// 认证相关事件
public static final String AUTH_VALIDATE = "auth.validate";
public static final String AUTH_RESULT = "auth.result";

// 消息相关事件
public static final String MESSAGE_CHAT = "message.chat";
public static final String MESSAGE_CONTROL = "message.control";
```

### 2. BaseEvent - 统一事件结构

所有NATS事件的基础数据结构，包含：

- 事件ID、主题、类型、状态
- 源服务和目标服务信息
- 用户上下文（用户ID、设备ID、会话ID）
- 事件数据和元数据
- 时间戳、过期时间、重试机制

```java
BaseEvent<AuthenticationRequest> event = BaseEvent.createRequest(
    NatsEventConstants.AUTH_VALIDATE, 
    authRequest
)
.fromService("communication-service", "default")
.withUser(userId, deviceId, sessionId)
.highPriority();
```

### 3. EventManager - 事件管理器

统一管理事件的发布、订阅和处理：

- **事件发布**：支持不同类型和优先级的事件
- **事件订阅**：支持处理器和响应映射器
- **请求-响应模式**：支持异步请求等待响应
- **超时和重试**：内置超时和重试机制

## 使用示例

### 发布事件

```java
@Autowired
private EventManager eventManager;

// 发布简单事件
eventManager.publishEvent(NatsEventConstants.MESSAGE_CHAT, chatMessage);

// 发布高优先级事件
eventManager.publishHighPriorityEvent(NatsEventConstants.AUTH_VALIDATE, authRequest);

// 发布通知事件
eventManager.publishNotification(NatsEventConstants.SYSTEM_NOTIFICATION, notification);
```

### 订阅事件

```java
// 订阅事件
eventManager.subscribe(NatsEventConstants.MESSAGE_CHAT, this::handleChatMessage);

// 订阅并设置响应映射器
eventManager.subscribeWithResponse(
    NatsEventConstants.AUTH_VALIDATE, 
    this::handleAuthRequest
);
```

### 发送请求并等待响应

```java
// 发送认证请求并等待响应
CompletableFuture<BaseEvent<AuthResult>> future = eventManager.sendRequest(
    NatsEventConstants.AUTH_VALIDATE, 
    authRequest, 
    AuthResult.class
);

// 处理响应
future.thenAccept(response -> {
    if (response.getStatus().equals(NatsEventConstants.EVENT_STATUS_SUCCESS)) {
        // 认证成功
        handleAuthSuccess(response.getData());
    } else {
        // 认证失败
        handleAuthFailure(response.getErrorMessage());
    }
});
```

## 事件类型

### 1. 认证事件 (auth.*)

- `auth.validate` - 验证用户token
- `auth.result` - 认证结果
- `auth.logout` - 用户登出

### 2. 消息事件 (message.*)

- `message.chat` - 聊天消息
- `message.control` - 控制消息
- `message.result` - 消息处理结果
- `message.broadcast` - 消息广播

### 3. 用户状态事件 (user.*)

- `user.online` - 用户上线
- `user.offline` - 用户离线
- `user.status.change` - 用户状态变更

### 4. 会话事件 (conversation.*)

- `conversation.create` - 会话创建
- `conversation.update` - 会话更新
- `conversation.delete` - 会话删除

### 5. 系统事件 (system.*)

- `system.notification` - 系统通知
- `health.check` - 健康检查
- `config.update` - 配置更新

## 事件优先级

- `URGENT` - 紧急事件（如系统告警）
- `HIGH` - 高优先级事件（如认证请求）
- `MEDIUM` - 中等优先级事件（如聊天消息）
- `LOW` - 低优先级事件（如统计信息）

## 事件状态

- `PENDING` - 处理中
- `SUCCESS` - 成功
- `FAILURE` - 失败
- `TIMEOUT` - 超时

## 最佳实践

### 1. 事件命名规范

- 使用小写字母和点号分隔
- 格式：`{模块}.{操作}.{类型}`
- 示例：`auth.validate`、`message.chat`、`user.online`

### 2. 事件数据结构

- 继承BaseEvent类
- 包含必要的业务数据
- 避免过大的数据负载

### 3. 错误处理

- 设置适当的超时时间
- 实现重试机制
- 记录详细的错误信息

### 4. 性能优化

- 使用异步处理
- 避免阻塞操作
- 合理设置事件优先级

## 监控和调试

### 统计信息

```java
Map<String, Object> stats = eventManager.getStatistics();
// 包含：pendingRequests, eventHandlers, responseMappers, subscriptions
```

### 日志记录

系统自动记录所有事件的发布、订阅和处理过程，便于调试和监控。

### 健康检查

定期清理过期的请求和连接，保持系统健康状态。

## 扩展性

系统设计支持：

- 新的事件类型和主题
- 自定义事件处理器
- 事件过滤和路由
- 负载均衡和故障转移
- 事件持久化和重放

## 总结

NATS事件管理系统为IM系统提供了：

1. **统一的事件定义** - 避免硬编码，便于维护
2. **类型安全的事件结构** - 减少运行时错误
3. **灵活的事件处理** - 支持多种处理模式
4. **可靠的通信机制** - 内置超时、重试、错误处理
5. **良好的扩展性** - 支持新功能的无缝集成

通过这套系统，业务层和通信层可以实现松耦合、高可靠的异步通信，为IM系统的稳定运行提供有力保障。 