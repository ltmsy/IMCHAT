# IM 系统分布式单体架构设计规范— 完整版

## 文档概述

本文档提供基于分布式单体架构的即时通讯（IM）系统完整设计方案，覆盖：
- 模块边界与职责划分
- 协议契约与数据模型
- 消息路径与权限校验
- 媒体处理与存储策略
- 观测运维与安全设计
- NATS消息总线通信设计
- 共享库与公共组件设计

**技术栈**: 支持 Java/Kotlin/Go/Node 等语言，示例以通用术语描述

---

## 0. 架构图与流程图总览

### 0.1 分布式单体架构总览图

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                   客户端层                                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
│  │  Web客户端   │ │  移动端APP   │ │  桌面客户端   │ │  第三方API   │ │  管理后台    │ │
│  │ (WebSocket) │ │ (WebSocket) │ │ (WebSocket) │ │   (HTTP)    │ │   (HTTP)    │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                        │
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                   负载均衡层                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                    Nginx/HAProxy + SSL终端                                    │ │
│  │  WebSocket路由 → 核心服务集群    HTTP API路由 → 业务服务集群                    │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                        │
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                分布式单体服务层                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                    im-core-service 核心单体                             │ │ │
│  │  │                                                                             │ │ │
│  │  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐               │ │ │
│  │  │  │   网关模块      │ │   消息模块      │ │   在线状态模块   │               │ │ │
│  │  │  │ (Gateway)       │ │ (Message)       │ │ (Presence)      │               │ │ │
│  │  │  │                 │ │                 │ │                 │               │ │ │
│  │  │  │ • WebSocket接入  │ │ • 会话模型管理   │ │ • 用户在线状态   │               │ │ │
│  │  │  │ • 协议编解码     │ │ • 消息落库      │ │ • 节点路由      │               │ │ │
│  │  │  │ • 心跳/ACK      │ │ • 序号分配      │ │ • 订阅关系缓存   │               │ │ │
│  │  │  │ • 限流/背压     │ │ • 消息扇出      │ │ • 故障漂移      │               │ │ │
│  │  │  │ • 快速校验      │ │ • 历史拉取      │ │ • 热点治理      │               │ │ │
│  │  │  │ • 路由转发      │ │ • 重传机制      │ │ • 快速收敛      │               │ │ │
│  │  │  └─────────────────┘ └─────────────────┘ └─────────────────┘               │ │ │
│  │  │                                                                             │ │ │
│  │  │  ┌─────────────────────────────────────────────────────────────────────────┐ │ │ │
│  │  │  │                           共享组件                                     │ │ │ │
│  │  │  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐ │ │ │ │
│  │  │  │  │ 缓存管理    │ │ 配置管理    │ │ 监控埋点    │ │ 错误处理        │ │ │ │ │
│  │  │  │  │ • Redis客户端│ │ • 配置热更新 │ │ • 指标收集  │ │ • 统一异常      │ │ │ │ │
│  │  │  │  │ • 本地缓存  │ │ • 环境配置  │ │ • 链路追踪  │ │ • 错误码        │ │ │ │ │
│  │  │  │  │ • 缓存策略  │ │ • 动态配置  │ │ • 日志记录  │ │ • 降级策略      │ │ │ │ │
│  │  │  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────────┘ │ │ │ │
│  │  │  └─────────────────────────────────────────────────────────────────────────┘ │ │ │
│  │  └─────────────────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                                 │ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                    im-business-service 业务单体                            │ │ │
│  │  │                                                                             │ │ │
│  │  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐               │ │ │
│  │  │  │   权限模块      │ │   媒体模块      │ │   定时任务模块   │               │ │ │
│  │  │  │ (Auth)          │ │ (Media)         │ │ (Scheduler)     │               │ │ │
│  │  │  │                 │ │                 │ │                 │               │ │ │
│  │  │  │ • 群成员关系     │ │ • 媒体直传签发   │ │ • 定时任务调度   │               │ │ │
│  │  │  │ • 角色权限       │ │ • 上传鉴权配额   │ │ • 数据清理      │               │ │ │
│  │  │  │ • 禁言黑名单     │ │ • 转码/缩略图    │ │ • 统计报表      │               │ │ │
│  │  │  │ • 会话权限核验   │ │ • 内容安全      │ │ • 数据同步      │               │ │ │
│  │  │  │ • 权限缓存      │ │ • 存储管理      │ │ • 监控告警      │               │ │ │
│  │  │  └─────────────────┘ └─────────────────┘ └─────────────────┘               │ │ │
│  │  │                                                                             │ │ │
│  │  │  ┌─────────────────────────────────────────────────────────────────────────┐ │ │ │
│  │  │  │                           共享组件                                     │ │ │ │
│  │  │  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐ │ │ │ │
│  │  │  │  │ 缓存管理    │ │ 配置管理    │ │ 监控埋点    │ │ 错误处理        │ │ │ │ │
│  │  │  │  │ • Redis客户端│ │ • 配置热更新 │ │ • 指标收集  │ │ • 统一异常      │ │ │ │ │
│  │  │  │  │ • 本地缓存  │ │ • 环境配置  │ │ • 链路追踪  │ │ • 错误码        │ │ │ │ │
│  │  │  │  │ • 缓存策略  │ │ • 动态配置  │ │ • 日志记录  │ │ • 降级策略      │ │ │ │ │
│  │  │  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────────┘ │ │ │ │
│  │  │  └─────────────────────────────────────────────────────────────────────────┘ │ │ │
│  │  └─────────────────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                                 │ │
│  │  ┌─────────────────┐                                                           │ │
│  │  │ im-admin-       │                                                           │ │
│  │  │ service         │                                                           │ │
│  │  │ (管理运维)       │                                                           │ │
│  │  │                 │                                                           │ │
│  │  │ • 配置管理      │                                                           │ │
│  │  │ • 限流策略      │                                                           │ │
│  │  │ • 灰度发布      │                                                           │ │
│  │  │ • 数据校验      │                                                           │ │
│  │  │ • 审计合规      │                                                           │ │
│  │  └─────────────────┘                                                           │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                        │
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                 NATS消息总线                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │ │
│  │  │ 消息主题     │ │ 状态主题     │ │ 权限主题     │ │ 媒体主题     │ │ 管理主题     │ │ │
│  │  │             │ │             │ │             │ │             │ │             │ │ │
│  │  │ • im.message│ • im.presence│ • im.auth    │ • im.media   │ • im.admin   │ │ │
│  │  │ • im.deliver│ • im.route   │ • im.member  │ • im.upload  │ • im.config  │ │ │
│  │  │ • im.ack    │ • im.status  │ • im.group   │ • im.process │ • im.monitor │ │ │
│  │  │ • im.history│ • im.subscribe│ • im.ban    │ • im.scan    │ • im.audit   │ │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                        │
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                  基础设施层                                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
│  │    MySQL    │ │    Redis    │ │  MongoDB    │ │    MinIO    │ │ Prometheus  │ │
│  │  (主从)     │ │  (Cluster)  │ │  (副本集)   │ │  对象存储    │ │   监控采集   │ │
│  │   业务数据   │ │   缓存数据   │ │   消息数据   │ │   媒体文件   │ │   指标数据   │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
│  │   Grafana   │ │     ELK     │ │  XXL-Job    │ │   NATS      │ │  共享库     │ │
│  │   监控展示   │ │   日志分析   │ │   任务调度   │ │  消息总线   │ │   公共组件   │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### 0.2 服务间通信架构图

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                NATS消息总线通信架构                                │
│                                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                            im-core-service 核心单体                            │ │
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐                   │ │
│  │  │   网关模块      │ │   消息模块      │ │   在线状态模块   │                   │ │
│  │  │                 │ │                 │ │                 │                   │ │
│  │  │ • 内部方法调用  │ │ • 内部方法调用  │ │ • 内部方法调用  │                   │ │
│  │  │ • 零网络延迟    │ │ • 零序列化开销  │ │ • 共享内存      │                   │ │
│  │  │ • 共享连接池    │ │ • 批量处理      │ │ • 快速路由      │                   │ │
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘                   │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                               │
│                                    │ NATS通信                                      │
│                                    ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                            im-business-service 业务单体                        │ │
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐                   │ │
│  │  │   权限模块      │ │   媒体模块      │ │   定时任务模块   │                   │ │
│  │  │                 │ │                 │ │                 │                   │ │
│  │  │ • 内部方法调用  │ │ • 内部方法调用  │ │ • 内部方法调用  │                   │ │
│  │  │ • 零网络延迟    │ │ • 零序列化开销  │ │ • 共享内存      │                   │ │
│  │  │ • 权限缓存      │ │ • 媒体处理      │ │ • 任务调度      │                   │ │
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘                   │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                               │
│                                    │ NATS通信                                      │
│                                    ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                              管理服务集群                                      │ │
│  │  ┌─────────────────┐                                                           │ │
│  │  │ im-admin-       │                                                           │ │
│  │  │ service         │                                                           │ │
│  │  │                 │                                                           │ │
│  │  │ • 订阅配置      │                                                           │ │
│  │  │ • 发布监控      │                                                           │ │
│  │  │ • 查询统计      │                                                           │ │
│  │  └─────────────────┘                                                           │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                     │
│  通信模式说明:                                                                       │
│  • 核心服务内部: 直接方法调用，零延迟，共享内存                                    │
│  • 业务服务内部: 直接方法调用，零延迟，共享内存                                    │
│  • 服务间通信: 通过NATS异步通信，松耦合                                           │
│  • 优势: 减少网络调用，降低延迟，提高吞吐量                                       │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### 0.7 服务部署与扩展架构图

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                服务部署与扩展架构                                  │
│                                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                            核心服务集群 (4节点) - 必须集群                      │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │ │
│  │  │  核心节点1   │ │  核心节点2   │ │  核心节点3   │ │  核心节点4   │               │ │
│  │  │  16C32G     │ │  16C32G     │ │  16C32G     │ │  16C32G     │               │ │
│  │  │ 2.5万连接   │ │ 2.5万连接   │ │ 2.5万连接   │ │ 2.5万连接   │               │ │
│  │  │ 8000 QPS    │ │ 8000 QPS    │ │ 8000 QPS    │ │ 8000 QPS    │               │ │
│  │  │ 网关+消息+状态│ │ 网关+消息+状态│ │ 网关+消息+状态│ │ 网关+消息+状态│               │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘               │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                            业务服务集群 (3节点) - 建议集群                      │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                               │ │
│  │  │  业务节点1   │ │  业务节点2   │ │  业务节点3   │                               │ │
│  │  │  8C16G      │ │  8C16G      │ │  8C16G      │                               │ │
│  │  │ 权限+媒体+任务│ │ 权限+媒体+任务│ │ 权限+媒体+任务│                               │ │
│  │  │ 端口:8085   │ │ 端口:8086   │ │ 端口:8087   │                               │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘                               │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                              管理服务 (2节点) - 主备即可                        │ │
│  │  ┌─────────────┐ ┌─────────────┐                                               │ │
│  │  │  管理节点1   │ │  管理节点2   │                                               │ │
│  │  │  4C8G       │ │  4C8G       │                                               │ │
│  │  │ 端口:8088   │ │ 端口:8089   │                                               │ │
│  │  └─────────────┘ └─────────────┘                                               │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                              基础设施层                                        │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │ │
│  │  │    NATS     │ │    Redis    │ │  MongoDB    │ │    MySQL    │               │ │
│  │  │  3节点集群   │ │ 3主3从集群  │ │ 3节点副本集  │ │  主从部署   │               │ │
│  │  │ 端口:4222   │ │ 端口:6379   │ │ 端口:27017  │ │ 端口:3306   │               │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘               │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │ │
│  │  │    MinIO    │ │ Prometheus  │ │   Grafana   │ │    ELK      │               │ │
│  │  │  4节点集群   │ │  2节点集群   │ │  2节点集群   │ │  3节点集群  │               │ │
│  │  │ 端口:9000   │ │ 端口:9090   │ │ 端口:3000   │ │ 端口:9200   │               │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘               │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                     │
│  扩展策略说明:                                                                       │
│  • 水平扩展: 核心服务和业务服务可以独立扩缩容，通过负载均衡                        │
│  • 垂直扩展: 单个节点可以增加CPU/内存资源                                        │
│  • 故障隔离: 单个服务故障不影响其他服务                                          │
│  • 灰度发布: 支持按版本标签路由，快速回滚                                        │
│  • 优势: 核心功能集中，减少网络调用，提高性能                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 1. 架构总览与设计原则

### 1.1 架构定位
- **架构模式**: 分布式单体架构
- **部署策略**: 3个独立单体服务，通过NATS消息总线通信，共享数据库避免分布式事务复杂性
- **核心优化**: 
  - 网关、消息、在线状态合并为核心单体，减少网络调用，提高性能
  - 权限、媒体、定时任务合并为业务单体，简化部署，提高资源利用率

### 1.2 核心设计原则
- **边界清晰**: 传输与业务分离、权限与会话分离、在线与分发分离
- **通信解耦**: 核心服务内部直接调用，业务服务内部直接调用，服务间通过NATS异步通信
- **性能优先**: 低延迟路径最短化，边缘缓存与就近校验，内部零网络延迟
- **可观测性**: 指标、日志、Trace 三位一体，优先可回溯性
- **单体优势**: 保持单体的简单性，具备分布式的扩展性

---

## 2. 模块划分与职责

### 2.1 核心服务模块

#### im-core-service（核心单体服务）
- **主要职责**: 
  - **网关模块**: WebSocket接入、协议编解码、心跳/ACK、限流/背压、快速校验、路由转发
  - **消息模块**: 会话模型管理、消息落库、序号seq、扇出、未读游标、历史拉取、重传
  - **在线状态模块**: 用户/设备在线状态、节点路由、订阅关系缓存、失效迁移
  - **音视频模块**: WebRTC信令交换、房间管理、媒体协商、通话状态维护
- **核心特性**: 
  - 内部模块间直接方法调用，零网络延迟
  - 共享内存和连接池，提高性能
  - 支持水平扩展，每个节点独立处理连接和消息
- **状态管理**: 维护传输层Session/Device与连接路由，支持多节点集群

##### 网关模块详细结构
```
gateway/
├── connection/                  # 连接管理
│   ├── ConnectionManager.java   # 连接管理器
│   ├── ConnectionStore.java     # 连接存储
│   └── ConnectionLifecycle.java # 连接生命周期
├── protocol/                    # 协议处理
│   ├── ProtocolHandler.java     # 协议处理器
│   ├── EnvelopeDecoder.java     # 消息解码
│   └── EnvelopeEncoder.java     # 消息编码
├── session/                     # 会话管理
│   ├── SessionManager.java      # 会话管理器
│   ├── DeviceSession.java       # 设备会话
│   └── SessionStore.java        # 会话存储
├── heartbeat/                   # 心跳管理
│   ├── HeartbeatManager.java    # 心跳管理器
│   ├── HeartbeatChecker.java    # 心跳检查
│   └── IdleConnectionScanner.java # 空闲连接扫描
├── security/                    # 安全控制
│   ├── AuthHandler.java         # 认证处理
│   ├── TokenValidator.java      # Token验证
│   └── PermissionChecker.java   # 权限检查
├── limiter/                     # 限流控制
│   ├── RateLimiter.java         # 速率限制器
│   ├── ConnectionLimiter.java   # 连接数限制
│   └── MessageSizeLimiter.java  # 消息大小限制
└── push/                        # 消息推送
    ├── PushService.java         # 推送服务
    ├── BatchPusher.java         # 批量推送
    └── PriorityQueue.java       # 优先级队列
```

##### 消息模块详细结构
```
message/
├── processor/                   # 消息处理
│   ├── MessageProcessor.java    # 消息处理器
│   ├── MessageValidator.java    # 消息验证
│   └── MessageRouter.java       # 消息路由
├── store/                       # 消息存储
│   ├── MessageStore.java        # 消息存储接口
│   ├── MongoMessageStore.java   # MongoDB实现
│   └── CachedMessageStore.java  # 缓存实现
├── sequence/                    # 序号管理
│   ├── SequenceGenerator.java   # 序号生成器
│   ├── ConversationSeq.java     # 会话序号
│   └── GlobalSeq.java           # 全局序号
├── delivery/                    # 消息投递
│   ├── MessageDelivery.java     # 消息投递
│   ├── DeliveryTracker.java     # 投递跟踪
│   └── RetryManager.java        # 重试管理
├── fanout/                      # 消息扇出
│   ├── FanoutManager.java       # 扇出管理器
│   ├── GroupFanout.java         # 群消息扇出
│   └── BatchFanout.java         # 批量扇出
├── history/                     # 历史消息
│   ├── HistoryService.java      # 历史服务
│   ├── TimelineStore.java       # 时间线存储
│   └── MessageSearcher.java     # 消息搜索
└── idempotent/                  # 幂等控制
    ├── IdempotentManager.java   # 幂等管理器
    ├── MessageDeduplicator.java # 消息去重
    └── IdempotentStore.java     # 幂等存储
```

##### 在线状态模块详细结构
```
presence/
├── manager/                     # 状态管理
│   ├── PresenceManager.java     # 在线状态管理器
│   ├── UserPresence.java        # 用户在线状态
│   └── DevicePresence.java      # 设备在线状态
├── store/                       # 状态存储
│   ├── PresenceStore.java       # 状态存储接口
│   ├── RedisPresenceStore.java  # Redis实现
│   └── LocalPresenceCache.java  # 本地缓存
├── route/                       # 路由管理
│   ├── RouteManager.java        # 路由管理器
│   ├── RouteTable.java          # 路由表
│   └── RouteStrategy.java       # 路由策略
├── subscription/                # 订阅管理
│   ├── SubscriptionManager.java # 订阅管理器
│   ├── ConversationSubs.java    # 会话订阅
│   └── SubscriptionStore.java   # 订阅存储
├── event/                       # 事件处理
│   ├── PresenceEventHandler.java # 事件处理器
│   ├── OnlineEvent.java         # 上线事件
│   └── OfflineEvent.java        # 下线事件
└── failover/                    # 故障转移
    ├── FailoverManager.java     # 故障转移管理器
    ├── NodeMonitor.java         # 节点监控
    └── ConnectionMigrator.java  # 连接迁移
```

##### 音视频模块详细结构
```
rtc/
├── signaling/                   # 信令处理
│   ├── SignalingService.java    # 信令服务
│   ├── OfferHandler.java        # Offer处理
│   └── AnswerHandler.java       # Answer处理
├── room/                        # 房间管理
│   ├── RoomManager.java         # 房间管理器
│   ├── RoomMember.java          # 房间成员
│   └── RoomStore.java           # 房间存储
├── call/                        # 通话管理
│   ├── CallManager.java         # 通话管理器
│   ├── CallSession.java         # 通话会话
│   └── CallHistory.java         # 通话历史
├── media/                       # 媒体控制
│   ├── MediaController.java     # 媒体控制器
│   ├── MediaConstraints.java    # 媒体约束
│   └── MediaStats.java          # 媒体统计
├── agora/                       # 声网集成
│   ├── AgoraTokenService.java   # 声网Token服务
│   ├── AgoraCloudRecording.java # 云端录制
│   ├── AgoraEventHandler.java   # 事件处理
│   └── AgoraConfig.java         # 声网配置
└── quality/                     # 质量监控
    ├── QualityMonitor.java      # 质量监控器
    ├── NetworkStats.java        # 网络统计
    └── QualityReporter.java     # 质量报告
```

#### im-business-service（业务单体服务）
- **主要职责**: 
  - **权限模块**: 群成员/角色、禁言/黑名单、加入/退出事件、会话权限核验、权限缓存管理
  - **媒体模块**: 媒体直传签发、上传鉴权与配额、转码/缩略图、内容安全、存储管理
  - **定时任务模块**: 定时任务调度、数据清理、统计报表、数据同步、监控告警
- **核心特性**: 
  - 内部模块间直接方法调用，零网络延迟
  - 共享缓存和配置，提高资源利用率
  - 支持水平扩展，每个节点独立处理业务请求
- **业务管理**: 统一的权限控制、媒体处理、定时任务调度

##### 权限模块详细结构
```
auth/
├── service/                     # 权限服务
│   ├── AuthService.java         # 权限服务接口
│   ├── GroupAuthService.java    # 群组权限服务
│   └── UserAuthService.java     # 用户权限服务
├── model/                       # 权限模型
│   ├── Permission.java          # 权限定义
│   ├── Role.java                # 角色定义
│   └── AuthResult.java          # 权限结果
├── member/                      # 成员管理
│   ├── MemberManager.java       # 成员管理器
│   ├── GroupMember.java         # 群组成员
│   └── MembershipStore.java     # 成员关系存储
├── rule/                        # 规则管理
│   ├── RuleManager.java         # 规则管理器
│   ├── MuteRule.java            # 禁言规则
│   └── BlacklistRule.java       # 黑名单规则
├── cache/                       # 缓存管理
│   ├── AuthCache.java           # 权限缓存
│   ├── RoleCache.java           # 角色缓存
│   └── CacheInvalidator.java    # 缓存失效
└── event/                       # 事件处理
    ├── MemberEventHandler.java  # 成员事件处理
    ├── JoinEvent.java           # 加入事件
    └── LeaveEvent.java          # 离开事件
```

##### 媒体模块详细结构
```
media/
├── upload/                      # 上传管理
│   ├── UploadService.java       # 上传服务
│   ├── UploadToken.java         # 上传凭证
│   └── QuotaManager.java        # 配额管理
├── storage/                     # 存储管理
│   ├── StorageService.java      # 存储服务
│   ├── MinioStorage.java        # MinIO存储
│   └── StoragePolicy.java       # 存储策略
├── process/                     # 处理管理
│   ├── MediaProcessor.java      # 媒体处理器
│   ├── ImageProcessor.java      # 图片处理
│   └── VideoProcessor.java      # 视频处理
├── security/                    # 安全管理
│   ├── ContentScanner.java      # 内容扫描
│   ├── SensitiveDetector.java   # 敏感检测
│   └── WatermarkManager.java    # 水印管理
├── thumbnail/                   # 缩略图管理
│   ├── ThumbnailGenerator.java  # 缩略图生成
│   ├── ImageResizer.java        # 图片缩放
│   └── ThumbnailStore.java      # 缩略图存储
└── lifecycle/                   # 生命周期管理
    ├── LifecycleManager.java    # 生命周期管理器
    ├── ExpirationPolicy.java    # 过期策略
    └── CleanupTask.java         # 清理任务
```

##### 定时任务模块详细结构
```
scheduler/
├── xxljob/                      # XXL-Job集成
│   ├── XxlJobConfig.java        # XXL-Job配置
│   ├── JobHandlerRegistry.java  # 任务处理器注册
│   └── ExecutorConfig.java      # 执行器配置
├── handler/                     # 任务处理器
│   ├── DataCleanJobHandler.java # 数据清理任务
│   ├── StatisticsJobHandler.java # 统计任务
│   └── SyncJobHandler.java      # 同步任务
├── service/                     # 任务服务
│   ├── TaskService.java         # 任务服务接口
│   ├── CleanupService.java      # 清理服务
│   └── ReportService.java       # 报表服务
├── monitor/                     # 监控管理
│   ├── JobMonitor.java          # 任务监控
│   ├── ExecutionLogger.java     # 执行日志
│   └── AlertNotifier.java       # 告警通知
└── utils/                       # 工具类
    ├── CronUtils.java           # Cron表达式工具
    ├── JobParamUtils.java       # 任务参数工具
    └── FailoverUtils.java       # 故障转移工具
```

#### im-admin-service（管理运维服务）
- **主要职责**: 配置管理、限流策略、灰度、数据校验工具、审计与合规导出
- **详细模块结构**:

```
admin/
├── config/                      # 配置管理
│   ├── ConfigManager.java       # 配置管理器
│   ├── ConfigPublisher.java     # 配置发布
│   └── ConfigHistory.java       # 配置历史
├── ratelimit/                   # 限流管理
│   ├── RateLimitManager.java    # 限流管理器
│   ├── LimitRule.java           # 限流规则
│   └── LimitPublisher.java      # 限流发布
├── gray/                        # 灰度发布
│   ├── GrayManager.java         # 灰度管理器
│   ├── GrayStrategy.java        # 灰度策略
│   └── VersionRouter.java       # 版本路由
├── validator/                   # 数据校验
│   ├── DataValidator.java       # 数据校验器
│   ├── SchemaManager.java       # 模式管理
│   └── ValidationTask.java      # 校验任务
├── audit/                       # 审计管理
│   ├── AuditManager.java        # 审计管理器
│   ├── AuditLogger.java         # 审计日志
│   └── ComplianceExporter.java  # 合规导出
├── dashboard/                   # 仪表盘
│   ├── DashboardService.java    # 仪表盘服务
│   ├── MetricsCollector.java    # 指标收集
│   └── ChartGenerator.java      # 图表生成
└── security/                    # 安全管理
    ├── AdminAuthService.java    # 管理认证服务
    ├── PermissionManager.java   # 权限管理
    └── AuditTrail.java          # 审计跟踪
```

### 2.2 共享库模块

#### shared-libs（共享库）
- **proto**: Protobuf契约与生成脚本
- **nats-common**: NATS通信封装、消息模式实现、监控与链路追踪
- **codec**: 统一Envelope、编解码器（Proto/JSON）
- **common**: 
  - **utils**: 通用工具类（时间、字符串、加密、ID生成等）
  - **error**: 错误码、重试策略、异常处理
  - **monitor**: 埋点规范、指标收集、追踪上下文
  - **config**: 配置加载与热更新
  - **cache**: 缓存策略与实现
  - **security**: 安全相关工具
- **websocket-push-api**: WebSocket推送接口与模型
- **database-common**: 数据库访问共享组件

##### proto模块详细结构
```
proto/
├── src/
│   ├── main/
│   │   ├── proto/                   # Protobuf定义文件
│   │   │   ├── common/              # 公共定义
│   │   │   │   ├── envelope.proto   # 消息包装
│   │   │   │   └── constants.proto  # 常量定义
│   │   │   ├── message/             # 消息相关
│   │   │   │   ├── message.proto    # 消息定义
│   │   │   │   └── history.proto    # 历史消息
│   │   │   ├── presence/            # 在线状态
│   │   │   │   └── presence.proto   # 状态定义
│   │   │   ├── auth/                # 权限相关
│   │   │   │   └── auth.proto       # 权限定义
│   │   │   ├── media/               # 媒体相关
│   │   │   │   └── media.proto      # 媒体定义
│   │   │   ├── rtc/                 # 音视频相关
│   │   │   │   └── signaling.proto  # 信令定义
│   │   │   └── admin/               # 管理相关
│   │   │       └── admin.proto      # 管理定义
│   │   └── java/                    # 生成的Java代码
│   └── test/                        # 测试代码
├── build.gradle                     # 构建脚本
└── README.md                        # 使用说明
```

##### nats-common模块详细结构
```
nats-common/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── core/
│   │   │   │   ├── NatsClient.java         # 基础客户端封装
│   │   │   │   ├── NatsConfig.java         # 配置类
│   │   │   │   └── NatsHealthCheck.java    # 健康检查
│   │   │   ├── messaging/
│   │   │   │   ├── MessagePublisher.java   # 消息发布接口
│   │   │   │   ├── MessageSubscriber.java  # 消息订阅接口
│   │   │   │   └── MessageHandler.java     # 消息处理接口
│   │   │   ├── codecs/
│   │   │   │   ├── ProtobufCodec.java      # Protobuf编解码
│   │   │   │   └── JsonCodec.java          # JSON编解码
│   │   │   ├── patterns/
│   │   │   │   ├── RequestResponse.java    # 请求响应模式
│   │   │   │   ├── PubSub.java             # 发布订阅模式
│   │   │   │   └── QueueGroup.java         # 队列组模式
│   │   │   └── monitoring/
│   │   │       ├── NatsMetrics.java        # 监控指标
│   │   │       └── NatsTracing.java        # 链路追踪
│   │   └── resources/
│   │       └── nats-default.yml            # 默认配置
│   └── test/                                # 测试代码
├── build.gradle                             # 构建脚本
└── README.md                                # 使用说明
```

##### codec模块详细结构
```
codec/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── envelope/
│   │   │   │   ├── EnvelopeEncoder.java    # 消息包装编码
│   │   │   │   ├── EnvelopeDecoder.java    # 消息包装解码
│   │   │   │   └── EnvelopeFactory.java    # 消息包装工厂
│   │   │   ├── protobuf/
│   │   │   │   ├── ProtobufCodec.java      # Protobuf编解码
│   │   │   │   └── ProtobufUtils.java      # Protobuf工具类
│   │   │   ├── json/
│   │   │   │   ├── JsonCodec.java          # JSON编解码
│   │   │   │   └── JsonUtils.java          # JSON工具类
│   │   │   └── compression/
│   │   │       ├── CompressionCodec.java   # 压缩编解码
│   │   │       └── CompressionUtils.java   # 压缩工具类
│   │   └── resources/
│   │       └── codec.properties            # 编解码配置
│   └── test/                                # 测试代码
├── build.gradle                             # 构建脚本
└── README.md                                # 使用说明
```

##### common模块详细结构
```
common/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── utils/
│   │   │   │   ├── time/
│   │   │   │   │   ├── TimeUtils.java      # 时间工具类
│   │   │   │   │   └── DateFormatter.java  # 日期格式化
│   │   │   │   ├── string/
│   │   │   │   │   ├── StringUtils.java    # 字符串工具类
│   │   │   │   │   └── TextFormatter.java  # 文本格式化
│   │   │   │   ├── crypto/
│   │   │   │   │   ├── CryptoUtils.java    # 加密工具类
│   │   │   │   │   └── HashUtils.java      # 哈希工具类
│   │   │   │   ├── id/
│   │   │   │   │   ├── IdGenerator.java    # ID生成器
│   │   │   │   │   └── SnowflakeId.java    # 雪花算法ID
│   │   │   │   └── validation/
│   │   │   │       ├── Validator.java      # 验证器
│   │   │   │       └── ValidationUtils.java # 验证工具类
│   │   │   ├── error/
│   │   │   │   ├── ErrorCode.java          # 错误码定义
│   │   │   │   ├── BusinessException.java  # 业务异常
│   │   │   │   ├── SystemException.java    # 系统异常
│   │   │   │   └── retry/
│   │   │   │       ├── RetryStrategy.java  # 重试策略
│   │   │   │       └── RetryExecutor.java  # 重试执行器
│   │   │   ├── monitor/
│   │   │   │   ├── metrics/
│   │   │   │   │   ├── MetricsCollector.java # 指标收集器
│   │   │   │   │   └── MetricsExporter.java # 指标导出器
│   │   │   │   ├── trace/
│   │   │   │   │   ├── TraceContext.java   # 追踪上下文
│   │   │   │   │   └── SpanManager.java    # 跨度管理器
│   │   │   │   └── logger/
│   │   │   │       ├── LoggerFactory.java  # 日志工厂
│   │   │   │       └── LogFormatter.java   # 日志格式化
│   │   │   ├── config/
│   │   │   │   ├── ConfigLoader.java       # 配置加载器
│   │   │   │   ├── ConfigWatcher.java      # 配置监听器
│   │   │   │   └── ConfigValidator.java    # 配置验证器
│   │   │   ├── cache/
│   │   │   │   ├── CacheManager.java       # 缓存管理器
│   │   │   │   ├── LocalCache.java         # 本地缓存
│   │   │   │   ├── RedisCache.java         # Redis缓存
│   │   │   │   └── CacheEvictionPolicy.java # 缓存淘汰策略
│   │   │   └── security/
│   │   │       ├── TokenManager.java       # 令牌管理器
│   │   │       ├── PasswordEncoder.java    # 密码编码器
│   │   │       └── SecurityUtils.java      # 安全工具类
│   │   └── resources/
│   │       └── common.properties           # 通用配置
│   └── test/                                # 测试代码
├── build.gradle                             # 构建脚本
└── README.md                                # 使用说明
```

##### websocket-push-api模块详细结构
```
websocket-push-api/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── model/
│   │   │   │   ├── PushMessage.java        # 推送消息模型
│   │   │   │   ├── PushTarget.java         # 推送目标
│   │   │   │   └── PushResult.java         # 推送结果
│   │   │   ├── service/
│   │   │   │   ├── PushService.java        # 推送服务接口
│   │   │   │   └── PushCallback.java       # 推送回调接口
│   │   │   ├── constants/
│   │   │   │   ├── PushType.java           # 推送类型
│   │   │   │   └── PushPriority.java       # 推送优先级
│   │   │   └── exception/
│   │   │       └── PushException.java      # 推送异常
│   │   └── resources/
│   │       └── push-api.properties         # 推送API配置
│   └── test/                                # 测试代码
├── build.gradle                             # 构建脚本
└── README.md                                # 使用说明
```

##### database-common模块详细结构
```
database-common/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── mongo/
│   │   │   │   ├── MongoConfig.java        # MongoDB配置
│   │   │   │   ├── MongoClientFactory.java # 客户端工厂
│   │   │   │   └── MongoHealthCheck.java   # 健康检查
│   │   │   ├── mysql/
│   │   │   │   ├── MySQLConfig.java        # MySQL配置
│   │   │   │   ├── DataSourceFactory.java  # 数据源工厂
│   │   │   │   └── MySQLHealthCheck.java   # 健康检查
│   │   │   ├── redis/
│   │   │   │   ├── RedisConfig.java        # Redis配置
│   │   │   │   ├── RedisClientFactory.java # 客户端工厂
│   │   │   │   └── RedisHealthCheck.java   # 健康检查
│   │   │   └── common/
│   │   │       ├── ConnectionPoolConfig.java # 连接池配置
│   │   │       ├── TransactionManager.java # 事务管理器
│   │   │       └── DatabaseMetrics.java    # 数据库指标
│   │   └── resources/
│   │       └── database-default.yml        # 默认配置
│   └── test/                                # 测试代码
├── build.gradle                             # 构建脚本
└── README.md                                # 使用说明
```

### 2.3 项目目录结构
```
im-distributed/
├── shared-libs/
│   ├── proto/          # Protobuf定义
│   ├── nats-common/    # NATS通信封装
│   ├── codec/          # 编解码器
│   └── common/         # 公共组件
├── im-core-service/     # 核心单体服务（网关+消息+在线状态）
├── im-business-service/ # 业务单体服务（权限+媒体+定时任务）
└── im-admin-service/    # 管理与运维服务
```

### 2.4 服务间通信与依赖关系

#### 服务间依赖
- **im-core-service**:
  - 依赖shared-libs中的所有模块
  - 通过NATS请求im-business-service进行权限校验和媒体处理
  - 接收im-admin-service的配置更新
- **im-business-service**:
  - 依赖shared-libs中的所有模块
  - 通过NATS请求im-core-service获取消息数据和在线状态
  - 接收im-admin-service的配置更新
- **im-admin-service**:
  - 依赖shared-libs中的所有模块
  - 通过NATS向im-core-service和im-business-service发布配置
  - 通过NATS接收各服务的监控数据

#### 数据库访问职责
- **im-core-service**:
  - 直接访问MongoDB进行消息存储和查询
  - 直接访问Redis进行在线状态和路由管理
  - 不直接访问MySQL
- **im-business-service**:
  - 直接访问MySQL进行业务数据管理
  - 直接访问Redis进行权限缓存和媒体处理状态管理
  - 不直接访问MongoDB
- **im-admin-service**:
  - 直接访问MySQL进行配置和审计数据管理
  - 直接访问Redis进行配置缓存
  - 可以通过NATS请求访问MongoDB中的消息数据（用于审计）

---

## 3. NATS消息总线通信架构

### 3.1 通信模式设计

#### 核心服务内部通信
- **直接方法调用**: 网关、消息、在线状态模块间直接方法调用
- **共享内存**: 连接信息、用户状态、会话数据在内存中共享
- **零网络延迟**: 内部通信无网络开销，响应更快

#### 业务服务内部通信
- **直接方法调用**: 权限、媒体、定时任务模块间直接方法调用
- **共享缓存**: 权限缓存、媒体配置、任务状态在内存中共享
- **零网络延迟**: 内部通信无网络开销，响应更快

#### 服务间通信
- **发布/订阅模式**: 消息广播、事件通知、状态变更
- **请求/响应模式**: 权限校验、配置查询等需要即时响应的场景
- **队列组模式**: 负载分担，多个相同服务实例组成队列组

### 3.2 NATS主题设计规范

#### 主题命名规则
```
im.{service}.{action}.{target}
```

#### 核心主题列表
```
# 消息相关
im.message.send              # 发送消息
im.message.deliver           # 消息投递
im.message.ack               # 消息确认
im.message.history           # 历史查询

# 状态相关  
im.presence.online           # 用户上线
im.presence.offline          # 用户下线
im.presence.route            # 路由查询
im.presence.status           # 状态更新

# 权限相关
im.auth.check                # 权限校验
im.auth.member               # 成员关系
im.auth.group                # 群组权限

# 媒体相关
im.media.upload              # 媒体上传
im.media.process             # 媒体处理
im.media.scan                # 内容扫描

# 管理相关
im.admin.config              # 配置更新
im.admin.monitor             # 监控数据
im.admin.audit               # 审计日志
```

### 3.3 服务间通信示例

#### 核心服务内部通信
```java
// 网关模块调用消息模块
@Autowired
private MessageService messageService;

// 消息模块调用在线状态模块
@Autowired
private PresenceService presenceService;

// 在线状态模块调用网关模块
@Autowired
private GatewayService gatewayService;

// 内部方法调用，零延迟
public void handleMessage(MessageRequest request) {
    // 直接调用，无网络开销
    AuthResult auth = authService.checkPermission(request);
    if (auth.isAllowed()) {
        messageService.processMessage(request);
    }
}
```

#### 业务服务内部通信
```java
// 权限模块调用媒体模块
@Autowired
private MediaService mediaService;

// 媒体模块调用定时任务模块
@Autowired
private SchedulerService schedulerService;

// 定时任务模块调用权限模块
@Autowired
private AuthService authService;

// 内部方法调用，零延迟
public void processMediaUpload(MediaUploadRequest request) {
    // 直接调用，无网络开销
    AuthResult auth = authService.checkMediaPermission(request);
    if (auth.isAllowed()) {
        mediaService.processUpload(request);
        schedulerService.scheduleCleanup(request.getMediaId());
    }
}
```

#### 服务间通信
```java
// 核心服务与业务服务通信
nats.publish("im.auth.check", authData);

// 业务服务与管理服务通信
nats.subscribe("im.admin.config", (msg) -> {
    ConfigData data = ConfigData.parseFrom(msg.getData());
    updateConfig(data);
});
```

### 3.4 共享通信模块设计

#### nats-common模块结构
```
shared-libs/
└── nats-common/
    ├── core/
    │   ├── NatsClient.java         # 基础客户端封装
    │   ├── NatsConfig.java         # 配置类
    │   └── NatsHealthCheck.java    # 健康检查
    ├── messaging/
    │   ├── MessagePublisher.java   # 消息发布接口
    │   ├── MessageSubscriber.java  # 消息订阅接口
    │   └── MessageHandler.java     # 消息处理接口
    ├── codecs/
    │   ├── ProtobufCodec.java      # Protobuf编解码
    │   └── JsonCodec.java          # JSON编解码
    ├── patterns/
    │   ├── RequestResponse.java    # 请求响应模式
    │   ├── PubSub.java             # 发布订阅模式
    │   └── QueueGroup.java         # 队列组模式
    └── monitoring/
        ├── NatsMetrics.java        # 监控指标
        └── NatsTracing.java        # 链路追踪
```

#### 核心功能
- **连接管理**: 连接建立、重连、健康检查
- **消息模式**: 发布/订阅、请求/响应、队列组
- **编解码**: Protobuf和JSON编解码
- **监控**: 指标收集、链路追踪
- **高级特性**: 自动重连、熔断器、限流、批量处理

#### 使用示例
```java
// 在核心服务中使用
@Service
public class MessageService {
    private final NatsClient natsClient;
    
    @Autowired
    public MessageService(NatsClient natsClient) {
        this.natsClient = natsClient;
    }
    
    public void sendAuthCheck(AuthCheckRequest request) {
        // 异步请求权限校验
        natsClient.request("im.auth.check", request, AuthCheckResponse.class, Duration.ofSeconds(5))
            .thenAccept(response -> {
                // 处理响应
                if (response.isAllowed()) {
                    // 继续处理消息
                } else {
                    // 拒绝消息
                }
            })
            .exceptionally(ex -> {
                // 处理异常
                log.error("权限校验失败", ex);
                return null;
            });
    }
}
```

### 3.5 NATS可靠性与一致性保障

#### JetStream持久化策略
- **需启用JetStream的关键主题**:
  - `im.auth.*`: 权限相关事件，确保权限变更不丢失
  - `im.admin.config`: 配置更新，确保配置变更可靠送达
  - `im.message.ack`: 消息确认，支持重放与恢复
  - `im.audit.*`: 审计日志，满足合规要求

- **Stream配置**:
  ```
  Stream: im-critical
  Subjects: [im.auth.*, im.admin.config, im.audit.*]
  Retention: Limits
  Storage: File
  Replicas: 3
  MaxAge: 7d
  ```

#### NATS主题配置统一清单

##### 权限与配置主题
| 主题名称 | 持久化 | 重放支持 | 最大重试 | 队列组 | 副本数 | 说明 |
|---------|--------|----------|----------|--------|--------|------|
| im.auth.check | 否 | 否 | 0 | 是 | - | 权限校验，不持久化 |
| im.auth.invalidate | JetStream | 是 | 5 | 是 | 3 | 权限失效通知，必须可靠 |
| im.auth.update | JetStream | 是 | 3 | 是 | 3 | 权限更新，必须可靠 |
| im.admin.config | JetStream | 是 | 5 | 是 | 3 | 配置变更，必须可靠 |
| im.admin.monitor | 否 | 否 | 0 | 是 | - | 监控数据，可丢失 |

##### 消息处理主题
| 主题名称 | 持久化 | 重放支持 | 最大重试 | 队列组 | 副本数 | 说明 |
|---------|--------|----------|----------|--------|--------|------|
| im.message.send | 否 | 否 | 0 | 是 | - | 消息发送，不持久化 |
| im.message.ack | JetStream | 是 | 3 | 是 | 3 | 消息确认，必须可靠 |
| im.message.deliver | 否 | 否 | 0 | 是 | - | 消息投递，不持久化 |
| im.message.history | 否 | 否 | 0 | 是 | - | 历史查询，不持久化 |

##### 状态与通知主题
| 主题名称 | 持久化 | 重放支持 | 最大重试 | 队列组 | 副本数 | 说明 |
|---------|--------|----------|----------|--------|--------|------|
| im.presence.update | 否 | 否 | 0 | 是 | - | 在线状态，可丢失 |
| im.presence.route | 否 | 否 | 0 | 是 | - | 路由信息，可丢失 |
| im.notification.push | JetStream | 是 | 5 | 是 | 3 | 推送通知，必须可靠 |
| im.audit.log | JetStream | 是 | 3 | 是 | 3 | 审计日志，必须可靠 |

#### 消费者配置规范
1. **权限变更消费者**：
   ```
   Consumer: auth-changes
   DeliverPolicy: All
   AckPolicy: Explicit
   MaxDeliver: 5
   AckWait: 30s
   FilterSubject: im.auth.*
   ```

2. **配置更新消费者**：
   ```
   Consumer: config-updates
   DeliverPolicy: All
   AckPolicy: Explicit
   MaxDeliver: 5
   AckWait: 60s
   FilterSubject: im.admin.config
   ```

3. **消息确认消费者**：
   ```
   Consumer: message-ack
   DeliverPolicy: All
   AckPolicy: Explicit
   MaxDeliver: 3
   AckWait: 15s
   FilterSubject: im.message.ack
   ```

#### 重试与降级策略
1. **重试策略**：
   - 权限/配置：指数退避，最大重试5次
   - 消息确认：线性退避，最大重试3次
   - 审计日志：固定间隔，最大重试3次

2. **降级策略**：
   - 权限服务不可用：使用本地缓存，记录告警
   - 配置服务不可用：使用默认配置，记录告警
   - 消息确认失败：异步重试，不影响正常发送

3. **监控指标**：
   - 重试率：按主题分类统计
   - 处理延迟：P50、P95、P99
   - 失败率：按错误类型分类统计

#### 消费语义与重复处理
- **消费模式**:
  - 权限/配置: 至少一次(At-Least-Once)
  - 消息投递: 最多一次(At-Most-Once)

- **消费者配置**:
  ```
  // 权限变更消费者
  Consumer: auth-changes
  DeliverPolicy: All
  AckPolicy: Explicit
  MaxDeliver: 5
  AckWait: 30s
  ```

- **去重策略**:
  - 消息ID: `{publisher}-{timestamp}-{sequence}`
  - 去重窗口: 使用Redis Sorted Set存储5分钟
  - 处理逻辑: 收到重复消息ID时忽略处理

#### 消费者并发与监控
- **并发配置**:
  - 队列组大小: 分片数 × 1.5
  - 每节点消费者线程: CPU核心数 × 0.7

- **监控指标**:
  - `nats_jetstream_messages_count`: 消息总量
  - `nats_consumer_ack_pending`: 待确认消息数
  - `nats_consumer_redelivered`: 重传消息数
  - `nats_consumer_processing_time`: 处理时间

- **告警阈值**:
  - 待确认消息积压: >1000条
  - 重传率: >1%
  - 处理延迟: >500ms

---

## 4. 协议与数据契约

### 3.1 Protobuf契约规范

#### 版本管理
- **位置**: `shared-libs/proto`
- **版本策略**: 语义化版本 vMAJOR.MINOR.PATCH
- **兼容性**: 新增字段仅append，不删除/重命名
- **枚举处理**: 预留保留位，未知值按兼容策略降级处理

#### 核心消息结构
```protobuf
// Envelope - 统一消息包装
message Envelope {
  Header header = 1;
  oneof payload {
    SendMessageReq send = 10;
    Ack ack = 11;
    Heartbeat hb = 12;
    Receipt read = 13;
    Typing typing = 14;
  }
}

// Header - 消息头
message Header {
  string traceId = 1;
  string clientMsgId = 2;
  int64 timestamp = 3;
  string codecVersion = 4;
  string deviceId = 5;
  int64 userId = 6;
}

// SendMessageReq - 发送消息请求
message SendMessageReq {
  string conversationId = 1;
  string clientMsgId = 2;
  MessageType type = 3;
  oneof content {
    string textContent = 4;
    MediaRef mediaRef = 5;
  }
  map<string, string> meta = 6;
}

// MediaRef - 媒体引用
message MediaRef {
  string mediaId = 1;
  string url = 2;
  int64 size = 3;
  string md5 = 4;
  string mime = 5;
  int32 duration = 6;
  int32 width = 7;
  int32 height = 8;
  string thumbnailUrl = 9;
}
```

### 3.2 WebSocket传输规范

#### 帧格式
- **承载方式**: 二进制帧承载Protobuf Envelope
- **分帧策略**: 采用长度前缀（varint）分帧
- **心跳机制**: 客户端定期Ping，服务端心跳窗口内未收到则关闭连接

#### ACK机制
- **上行确认**: 成功回执 `Ack{clientMsgId, serverMsgId, seq, status}`
- **状态流转**: SENT → STORED → DELIVERED → READ

### 3.3 HTTP/REST接口

#### 媒体上传接口
- **小文件直传**: `POST /media/upload`
- **大文件预签名**: `POST /media/sts`

#### 历史拉取接口
- **分页查询**: `GET /history?conversationId&fromSeq&limit&direction`

#### 管理接口
- **权限查询**: `GET /conv/{id}/members`
- **角色查询**: `GET /conv/{id}/role/me`
- **操作接口**: `POST /conv/{id}/mute|/blacklist`

---

## 4. 聊天会话模型

### 4.1 会话类型与策略

#### 会话分类
- **单聊（P2P）**: 参与者固定为两方，逻辑上可去重（A-B与B-A）
- **群聊（Group）**: 多成员，支持角色（owner/admin/member）、禁言、黑名单

#### ID生成策略
- **conversationId**: 全局唯一，雪花算法或随机+类型前缀
- **单聊映射**: `convId = hash(sorted(userA, userB))`，保留独立元数据表便于扩展

### 4.2 数据模型设计

#### 核心表结构
```sql
-- 会话表
CREATE TABLE conversation (
  id BIGINT PRIMARY KEY,
  conversation_id VARCHAR(64) UNIQUE NOT NULL,
  type TINYINT NOT NULL COMMENT '1:单聊 2:群聊',
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  last_msg_seq BIGINT DEFAULT 0,
  last_msg_at TIMESTAMP,
  ext JSON COMMENT '扩展字段'
);

-- 会话成员表
CREATE TABLE conversation_member (
  id BIGINT PRIMARY KEY,
  conversation_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  role TINYINT DEFAULT 0 COMMENT '0:成员 1:管理员 2:群主',
  mute_until TIMESTAMP NULL,
  join_at TIMESTAMP NOT NULL,
  last_read_seq BIGINT DEFAULT 0,
  last_ack_at TIMESTAMP,
  ext JSON,
  UNIQUE KEY uk_conv_user (conversation_id, user_id)
);

-- 消息表
CREATE TABLE message (
  id BIGINT PRIMARY KEY,
  conversation_id VARCHAR(64) NOT NULL,
  seq BIGINT NOT NULL,
  from_user_id BIGINT NOT NULL,
  type TINYINT NOT NULL,
  content TEXT,
  blob_ref VARCHAR(255),
  meta JSON,
  status TINYINT DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  shard_key VARCHAR(32) NOT NULL,
  UNIQUE KEY uk_conv_seq (conversation_id, seq),
  KEY idx_conv_time (conversation_id, created_at)
);
```

#### 索引策略
- **唯一索引**: `conversation_member(conversation_id, user_id)`
- **排序索引**: `message(conversation_id, seq)`、`message(conversation_id, created_at)`

### 4.3 序号与顺序保证

#### 消息序号分配与顺序保证

##### 序号分配策略
1. **全局序号生成**：
   - 使用Redis INCRBY生成全局递增序号
   - 按conversationId分片，避免热点
   - 序号格式：`{conversationId}:{timestamp}:{sequence}`
2. **本地序号缓存**：
   - 每个节点维护本地序号池
   - 批量预分配序号，减少Redis调用
   - 序号池大小：1000个，低于200时自动补充

##### 消息顺序保证机制
1. **唯一编号分配**：
   - 每条消息分配唯一编号：`{conversationId}-{seq}-{timestamp}`
   - 编号在消息落库前分配，确保顺序性
   - 编号作为消息的唯一标识，用于去重和排序
2. **顺序检查策略**：
   - 落库时检查序号连续性
   - 发现序号跳跃时记录告警
   - 序号重复时直接丢弃，记录日志

##### 消息去重与重复处理
1. **去重策略**：
   - 使用clientMsgId作为去重键
   - Redis TTL：7天，避免内存泄漏
   - 分片存储：按clientMsgId哈希分片
2. **重复消息处理**：
   - 收到重复clientMsgId时，返回已存在消息的状态
   - 不重复落库，避免数据冗余
   - 记录重复次数，用于监控分析

#### 消息处理时序图
```
┌─────────┐          ┌─────────┐          ┌─────────────┐          ┌─────────────┐
│ 客户端   │          │ 网关模块  │          │ 消息模块     │          │ 存储层       │
└────┬────┘          └────┬────┘          └──────┬──────┘          └──────┬──────┘
     │                    │                      │                        │
     │   发送消息请求       │                      │                        │
     │───────────────────>│                      │                        │
     │                    │                      │                        │
     │                    │   快速校验通过        │                        │
     │                    │─────────────────────>│                        │
     │                    │                      │                        │
     │                    │                      │   分配序号              │
     │                    │                      │───────────────────────>│
     │                    │                      │                        │
     │                    │                      │   序号分配成功          │
     │                    │                      │<───────────────────────│
     │                    │                      │                        │
     │                    │                      │   检查重复消息          │
     │                    │                      │───────────────────────>│
     │                    │                      │                        │
     │                    │                      │   无重复，开始落库      │
     │                    │                      │<───────────────────────│
     │                    │                      │                        │
     │                    │                      │   消息落库              │
     │                    │                      │───────────────────────>│
     │                    │                      │                        │
     │                    │                      │   落库成功              │
     │                    │                      │<───────────────────────│
     │                    │                      │                        │
     │                    │                      │   更新缓存              │
     │                    │                      │───────────────────────>│
     │                    │                      │                        │
     │                    │                      │   缓存更新成功          │
     │                    │                      │<───────────────────────│
     │                    │                      │                        │
     │  ACK(STORED)       │                      │                        │
     │<───────────────────│<─────────────────────│                        │
     │                    │                      │                        │
     │                    │                      │   开始消息扇出          │
     │                    │                      │                        │
```

#### 异常场景处理
1. **序号分配失败**：
   - 记录错误日志，返回FAILED状态
   - 客户端显示"发送失败"，提供重试选项
2. **重复消息检测**：
   - 返回已存在消息的状态
   - 客户端更新为对应状态
3. **序号跳跃处理**：
   - 记录告警，不影响正常消息处理
   - 定期检查序号连续性，发现问题及时修复

### 4.4 未读与游标
- **未读计数**: `lastReadSeq` per user per conversation
- **大群优化**: 仅计总未读或分层计数，精确未读按需计算

---

## 5. 消息发送路径与权限校验

### 5.1 完整消息流程

#### 上行消息路径
```
用户 → 核心单体(网关模块) → 核心单体(消息模块) → 业务单体(权限模块) → 其他在线成员
```

#### 详细步骤

**1) 连接期鉴权（im-core-service网关模块）**
- 验证Token（JWT/签名），绑定userId/deviceId/scopes
- 建立路由：在线状态模块记录userId→nodeId, deviceId→nodeId

**2) 网关快速校验**
- 字段有效性（type、大小、必填）
- 频控/限流（用户级、会话级、来源IP/设备）
- 会话存在性本地缓存校验（短TTL）
- 黑名单/禁言快速判断

**3) 业务权限校验（通过NATS与业务服务通信）**
- 核验是否为会话成员、角色权限、禁言状态
- 配额策略（如每天图片条数）
- 缓存策略：网关/分发侧本地缓存+Redis，TTL 30~120s

**4) 分发与存储（im-core-service消息模块）**
- 幂等处理：clientMsgId去重
- 序号分配：获取并分配conversation seq
- 消息落库：批量/异步落库，按分片批量写
- 消息扇出：在线路由查询在线状态模块，推送给在线成员

**5) 回执与终态**
- 成功：Ack返回serverMsgId、seq、status=OK
- 失败：返回明确错误码，可提示重试

### 5.2 关键设计要点

#### 快速拒绝策略
- 快速拒绝在网关层，权限事实以业务服务权限校验为准
- 幂等与顺序在消息模块处理
- 多媒体采用"直传+引用"的双通道

#### 缓存策略
- 网关层：短TTL缓存，快速校验
- 消息层：中等TTL缓存，减少重复查询
- 权限层：长TTL缓存，主动失效机制

#### 内部调用优势
- **零网络延迟**: 网关、消息、在线状态模块间直接方法调用
- **共享内存**: 连接信息、用户状态、会话数据在内存中共享
- **批量处理**: 消息处理和状态更新可以批量进行
- **减少序列化**: 内部对象传递，避免Protobuf序列化开销

#### 服务间通信优化
- **权限校验**: 通过NATS与业务服务异步通信，避免阻塞
- **媒体处理**: 通过NATS与业务服务异步通信，支持异步处理
- **配置更新**: 通过NATS与管理服务异步通信，支持热更新

#### 5.3 权限校验优化策略

##### 本地权限快照与双通道设计
- **本地权限快照**: 核心服务维护权限数据的本地快照缓存
  - 缓存内容: 会话成员关系、角色、禁言状态、黑名单
  - 缓存TTL: 普通会话300s，高频会话600s，敏感会话60s
  - 存储方式: 本地缓存(Caffeine) + Redis二级缓存
  - 容量控制: LRU淘汰策略，最大缓存项10万

##### 变更事件主动失效
- **事件驱动**: 权限变更时通过NATS主动推送失效事件
  - 主题: `im.auth.invalidate`，使用JetStream持久化
  - 事件内容: `{conversationId, userId, type, timestamp}`
  - 处理策略: 收到事件立即清除本地缓存，强制重新获取

##### 快速拒绝策略
- **布隆过滤器**: 维护禁言/黑名单用户的布隆过滤器
  - 更新周期: 60s或权限变更事件触发
  - 误判处理: 允许假阳性(宁可误拒绝)，禁止假阴性
  - 内存占用: 每100万用户约1MB内存

##### 超时与降级策略
- **超时控制**: 权限校验请求超时设置为300ms
- **降级策略**:
  - 超时时默认拒绝敏感操作(如群管理)
  - 普通消息可配置为"超时后放行+异步审计"
  - 记录降级事件并触发告警

---

## 6. 媒体消息处理策略

### 6.1 推荐处理模式

#### HTTP直传+WebSocket引用
**步骤流程**:
1. 获取上传凭证：`POST /media/sts`或`/media/upload`
2. 文件上传至对象存储
3. 回调/轮询获取转码/缩略图结果
4. 通过WS发送SendMessageReq，body为MediaRef

### 6.2 鉴权与配额管理

#### 上传前校验
- Token验证与配额检查
- 按用户级别限制大小与日额度
- URL短期有效（STS临时URL或签名URL）

#### 内容安全
- 异步扫描：文本敏感词、图像/音视频涉敏模型
- 违规处理：标记消息状态或撤回
- 客户端适配：状态变更为违规时可变灰或不可见

### 6.3 优化策略

#### 断点续传
- 大文件分片上传
- WebSocket仅传引用，避免占用接入链路

#### 小体积优化
- 表情/短语音可直接走WS二进制帧
- 推荐走直传以复用CDN、审计与续传

---

## 7. 在线状态与路由管理

### 7.1 数据模型设计

#### Redis键设计
```
presence:user:{userId} → {
  devices: [
    {deviceId, nodeId, lastSeen}
  ],
  lastActiveAt
}

route:device:{deviceId} → nodeId
subscribers:conv:{conversationId} → online userIds
```

### 7.2 读写路径

#### 连接管理
- **建立/断开**: 更新route与presence
- **心跳更新**: 更新lastSeen
- **路由查询**: 消息模块根据userId/deviceId找nodeId

#### 热点治理
- 热门大群禁用完整订阅集合
- 采用"在线查询+批量聚合推送"
- 本地缓存+短TTL，超热点键使用随机过期

### 7.3 故障处理

#### 故障漂移
- 节点下线时，将route失效
- 客户端重连到其他网关
- presence进行快速收敛

### 7.4 大群扇出优化策略

#### 扇出模式切换阈值
- **逐人推送阈值**:
  - 在线成员数 ≤ 1000: 直接推送给所有在线成员
  - 处理时间 ≤ 200ms: 直接推送

- **提示+拉取阈值**:
  - 在线成员数 > 1000: 切换为提示+拉取模式
  - 处理时间 > 200ms: 切换为提示+拉取模式
  - 消息大小 > 10KB: 切换为提示+拉取模式

- **动态调整**:
  - 监控扇出耗时，超过阈值自动调整策略
  - 支持按会话ID配置固定策略

#### 优先级队列设计
- **优先级定义**:
  - P0(最高): 系统通知、控制消息
  - P1(高): 单聊消息、小群消息
  - P2(中): 大群普通消息
  - P3(低): 大群媒体消息、历史同步

- **队列配置**:
  ```
  // 每个优先级的队列长度与处理比例
  P0: 1000条, 40%处理时间
  P1: 5000条, 30%处理时间
  P2: 10000条, 20%处理时间
  P3: 20000条, 10%处理时间
  ```

- **背压策略**:
  - 队列水位线监控: 70%/85%/95%
  - 70%水位线: 开始拒绝P3请求
  - 85%水位线: 开始拒绝P2请求
  - 95%水位线: 只接受P0请求

#### 批量处理优化
- **批量大小**:
  - 时间窗口: 5-10ms
  - 最大批量: 200条消息

- **分组策略**:
  - 按接收节点分组批量投递
  - 共享连接复用批量推送

- **监控指标**:
  - `fanout_batch_size`: 平均批量大小
  - `fanout_time_cost`: 扇出耗时
  - `fanout_queue_length`: 队列长度

### 7.5 Presence与路由一致性保障

#### 黏性路由策略
- **路由缓存**:
  - 本地缓存最近访问的1000个用户路由
  - 缓存TTL: 30秒
  - 更新策略: 成功路由时更新，失败立即失效

- **路由选择算法**:
  ```
  function getRoute(userId, deviceId) {
    // 1. 先查本地缓存
    route = localCache.get(userId + deviceId)
    if (route && !isExpired(route)) {
      return route
    }
    
    // 2. 查询Redis
    route = redisCache.get("route:device:" + deviceId)
    if (route) {
      localCache.put(userId + deviceId, route)
      return route
    }
    
    // 3. 广播查询
    return broadcastQuery(userId, deviceId)
  }
  ```

#### 快速收敛算法
- **抖动合并窗口**:
  - 合并窗口: 3秒内的上下线事件
  - 状态判定: 窗口结束时的最终状态

- **幂等事件处理**:
  - 事件ID: `{userId}-{deviceId}-{timestamp}-{nodeId}`
  - 事件有序性: 按timestamp排序，忽略过期事件

- **冲突解决策略**:
  - 多节点竞争: 使用分布式锁 `presence:lock:{userId}`
  - 锁超时: 200ms
  - 冲突处理: 以最新timestamp为准

#### 故障检测与迁移
- **节点健康检查**:
  - 心跳间隔: 5秒
  - 失败阈值: 3次连续失败判定节点下线

- **连接迁移流程**:
  1. 检测到节点下线
  2. 获取该节点的用户列表
  3. 标记这些用户为"待迁移"状态
  4. 等待客户端重连到新节点
  5. 新节点接收连接后更新路由

- **指标监控**:
  - `node_failure_count`: 节点故障次数
  - `route_migration_count`: 路由迁移次数
  - `route_inconsistency_count`: 路由不一致次数

---

## 8. 网关实现要点

### 8.1 连接管理

#### 性能目标
- 支持10万级长连接/节点
- 使用事件驱动网络库（Netty/epoll/Kqueue）

#### 内存优化
- 零拷贝/池化ByteBuf
- 限制临时对象
- 使用批量写（write coalescing）

### 8.2 限流与背压

#### 限流策略
- 用户级、会话级QPS限制
- 队列长度与消息大小限制

#### 背压处理
- 下游阻塞时应用背压
- 优先丢弃低优先级包或返回BUSY

### 8.3 编解码与校验

#### 编解码
- WS二进制帧映射Envelope
- 严格长度与字段检查
- 异常帧直接断开

#### 快速校验
- 字段、大小、黑名单、禁言、速率
- 尽量不落地日志（仅采样），避免放大器效应

---

## 9. 消息分发与历史实现

### 9.1 分片策略

#### 分片机制
- conversationId哈希分片
- 路由到对应分片队列/执行器
- 保证会话内串行

### 9.2 幂等与顺序

#### 幂等处理
- 幂等表：clientMsgId→serverMsgId, seq
- 写前查询或利用唯一索引冲突回读

#### 顺序保证
- 单会话执行器串行化
- 跨分片独立互不影响

### 9.3 存储设计

#### 冷热分层
- 热存储：最近消息（Redis/写优化引擎）
- 冷存储：长期归档（Mongo/ClickHouse/LSM-Tree引擎）

#### 批量优化
- 按分片/时间窗聚合写
- 写入前后打点

### 9.4 扇出策略

#### 群聊优化
- 小群：每成员直接路由推送
- 大群：按在线成员批量聚合，必要时走拉取提示

#### 回执处理
- 生成seq后立即ACK
- 落库失败有界重试
- 超过阈值标记DELAYED

### 9.5 数据库职责分配

#### MongoDB（消息数据）
- **维护方**: im-core-service（消息模块）
- **主要存储**: 
  - 消息内容
  - 会话时间线
  - 消息索引
- **访问方式**: 
  - 业务层（im-business-service）通过NATS请求im-core-service获取消息数据
  - 业务层不直接连接MongoDB

#### MySQL（业务数据）
- **维护方**: im-business-service（权限模块）
- **主要存储**:
  - 用户信息
  - 群组信息
  - 成员关系
  - 权限配置
- **访问方式**:
  - im-core-service通过NATS请求im-business-service进行权限校验
  - im-core-service不直接连接MySQL

#### Redis（缓存数据）
- **维护方**: 各服务独立维护自己的缓存空间
  - im-core-service: 在线状态、路由信息、消息缓存
  - im-business-service: 权限缓存、媒体处理状态
  - im-admin-service: 配置缓存
- **命名空间隔离**:
  - 使用前缀区分不同服务的缓存: `core:`、`business:`、`admin:`
- **共享实例**: 所有服务共享Redis集群，但使用不同的命名空间

### 9.6 MongoDB消息存储设计

#### 集合设计策略
- **分片集合设计**:
  ```
  message_{shard_id}  # 按conversationId哈希分片，32个分片
  timeline_{shard_id} # 用户时间线分片，32个分片
  ```

- **分片键选择**:
  - message集合: `{ conversationId: "hashed" }`
  - timeline集合: `{ userId: "hashed" }`

- **索引设计**:
  ```
  // 消息查询索引
  { conversationId: 1, seq: 1 }        # 会话内按序号查询
  { conversationId: 1, createdAt: -1 } # 会话内按时间查询
  { clientMsgId: 1 }                   # 幂等性检查
  
  // 时间线索引
  { userId: 1, conversationId: 1, seq: 1 }  # 用户会话消息
  { userId: 1, createdAt: -1 }              # 用户全局时间线
  ```

#### 冷热分层策略
- **热数据层**:
  - 最近30天消息存储在主集合
  - Redis缓存最近会话的最新100条消息
  - 读取路径: Redis缓存 → MongoDB主集合

- **冷数据层**:
  - 超过30天的消息迁移至归档集合 `message_archive_{YYYYMM}`
  - 归档集合添加TTL索引，默认保留180天
  - 可选配置按用户级别延长保留期

- **查询路由策略**:
  - 时间范围内查询自动路由至对应集合
  - 跨冷热边界查询合并结果
  - 冷数据查询支持异步任务模式

#### 容量规划
- **存储预估**:
  - 每条消息平均大小: 1KB
  - 日消息量: 2000万条 = 20GB/日
  - 30天热数据: 600GB
  - 索引空间: 约200GB
  - 总存储需求: 约1TB(含冗余)

- **性能目标**:
  - 热数据查询P99: <50ms
  - 冷数据查询P99: <500ms
  - 写入吞吐: 5000 ops/s/节点

### 9.7 幂等与去重设计

#### 幂等存储策略
- **存储选型**:
  - 小规模系统: Redis(带TTL)
  - 大规模系统: 分表存储 + Redis缓存热点键

- **Redis实现**:
  ```
  // 键设计
  idempotent:{shard}:{clientMsgId} -> {serverMsgId, seq, timestamp}
  
  // TTL设置
  SETEX idempotent:{shard}:{clientMsgId} 604800 {value}  // 7天过期
  ```

- **分表实现**:
  ```sql
  -- 按日期分表
  CREATE TABLE idempotent_YYYYMMDD (
    client_msg_id VARCHAR(64) PRIMARY KEY,
    server_msg_id BIGINT NOT NULL,
    seq BIGINT NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_conv_id (conversation_id)
  )
  ```

#### clientMsgId规范
- **格式定义**: `{userId}-{deviceId}-{localSeq}-{timestamp}`
- **示例**: `10086-ios12345-789-1612345678`
- **生成规则**:
  - userId: 用户ID
  - deviceId: 设备唯一标识
  - localSeq: 客户端本地递增序号
  - timestamp: 毫秒级时间戳

#### 容量与清理策略
- **容量估算**:
  - 每条记录大小: 约100字节
  - 日消息量: 2000万条 = 2GB/日
  - 7天总容量: 约14GB

- **清理策略**:
  - Redis: 自动TTL过期
  - 分表: 按日期定期删除过期表
  - 保留策略: 默认7天，可按需调整

#### 热点缓解策略
- **分片算法**: `shard = hash(clientMsgId) % SHARD_COUNT`
- **本地缓存**: 最近处理的10万条消息ID
- **写入优化**: 批量写入，定期刷盘

---

## 10. 错误码、限流与安全

### 10.1 错误码规范

#### 标准错误码
```
1001 AUTH_FAILED        # Token无效或过期
1003 FORBIDDEN          # 无权限/被禁言/被拉黑
1005 RATE_LIMITED       # 频控触发
1007 PAYLOAD_INVALID    # 字段或大小非法
1010 MEDIA_REQUIRED     # 媒体未上传或引用无效
1012 IDEMPOTENT_CONFLICT # 幂等冲突
1099 SERVER_BUSY        # 背压
1999 INTERNAL_ERROR     # 内部错误
```

### 10.2 限流策略

#### 限流维度
- 用户级、会话级、IP/设备级、类型级
- 管理侧在线变更限流规则
- 滑动窗口+令牌桶算法

### 10.3 安全机制

#### 基础安全
- Token签名与短期有效
- 重要操作二次校验
- 内容安全与审计日志

#### 防重放攻击
- clientMsgId+时间窗校验
- 时间戳与签名验证

---

## 11. 观测与运维体系

### 11.1 监控指标

#### 核心服务指标
- `connections`: 连接数
- `heartbeat_drop_rate`: 心跳丢失率
- `inbound/outbound_qps`: 入站/出站QPS
- `p99_write_latency`: P99写入延迟
- `backpressure_events`: 背压事件数
- `internal_call_latency`: 内部模块调用延迟
- `memory_usage`: 内存使用率

#### 业务服务指标
- `auth_check_qps`: 权限校验QPS
- `media_upload_qps`: 媒体上传QPS
- `xxl_job_success_rate`: XXL-Job任务成功率
- `xxl_job_execution_time`: XXL-Job任务执行时间
- `internal_call_latency`: 内部模块调用延迟
- `memory_usage`: 内存使用率

#### 消息分发指标
- `assign_seq_latency`: 序号分配延迟
- `batch_write_size`: 批量写入大小
- `retry_count`: 重试次数
- `fanout_qps`: 扇出QPS
- `p99_history_fetch`: P99历史查询延迟

#### 在线状态指标
- `route_lookup_qps`: 路由查询QPS
- `hotspot_key_rate`: 热键率
- `failover_time`: 故障转移时间

### 11.2 日志规范

#### 关键事件日志
- 鉴权失败原因
- 拒绝原因采样
- 异常回执
- 写失败与重试
- 上传失败

#### 隐私控制
- 敏感字段脱敏
- 遵循合规要求

### 11.3 链路追踪

#### Trace设计
- traceId注入Envelope.header
- 贯穿网关→消息→在线状态→存储→扇出
- 推送事件：deliverTs、ackTs、storeLatencyMs、fanoutCount

### 11.4 告警规则

#### 告警维度
- SLA违反（ACK延迟、丢消息率）
- 队列积压
- 热键告警
- 连接抖动
- 媒资异常

---

## 12. 数据保留与合规

### 12.1 数据保留策略

#### 分层存储
- **热数据**: 近7~30天
- **冷数据**: 归档存储，查询走异步任务或延迟响应

### 12.2 合规要求

#### 数据删除
- 用户删除请求的数据处理
- 软删标记+定期物理清理

#### 审计导出
- 按会话/时间窗口导出可验证日志
- 数据加密与访问控制

### 12.3 备份与恢复

#### 备份策略
- 周期性快照
- 跨可用区/区域复制
- 演练恢复流程

---

## 13. 客户端交互约定

### 13.1 连接与重连

#### 连接策略
- 首次连接带token与设备信息
- 断线重连携带上次per-conversation lastSeq

### 13.2 消息提交

#### 提交流程
- 先媒体上传（如需要），再提交WS SendMessageReq
- 期待在超时内收到ACK
- 收到ACK后更新本地映射clientMsgId→serverMsgId, seq

### 13.3 拉取策略

#### 历史拉取
- 打开会话先拉取最近N条
- 有缺口则按seq拉取
- 大群默认只拉摘要或分页加载

#### 回执处理
- 阅读时发送read-receipt
- 包含conversationId, readSeq
- 服务端更新未读计数

---

## 14. 升级与兼容策略

### 14.1 契约变更流程

#### 升级顺序
- 先升级消费者（忽略未知字段）
- 再升级生产者（开始发送新字段）
- 枚举新值灰度：先静默发送到小流量

### 14.2 网关灰度

#### 灰度策略
- 按客户端版本标签路由
- 出现不兼容快速回滚

### 14.3 数据迁移

#### 迁移原则
- 表结构只增不删
- 删除字段走shadow字段与双写过渡

---

## 15. 压测与容量规划

### 15.1 压测场景

#### 连接压测
- 长连接保活：10万/节点
- 心跳30s，掉线率<0.5%

#### 消息压测
- 发送峰值：单会话5k msg/s顺序保障
- 全站扇出：100万msg/s

#### 查询压测
- 历史拉取：P99<100ms（缓存命中）
- 冷查询：<800ms

### 15.2 压测工具

#### 工具选择
- 自研WS压测器（多连接/多会话混合流）
- 人工注入热点会话、热键、网卡/CPU限流测试

### 15.3 性能优化抓手

#### 关键优化点
- 批量写、零拷贝、协议压缩
- 热点分片、只读缓存、异步扇出

---

## 16. 分布式架构实施路线

### 16.1 实施阶段

#### 阶段1：基础架构搭建
- 搭建NATS消息总线集群
- 建立共享数据库和缓存
- 配置基础监控和日志

#### 阶段2：核心服务开发
- 开发im-core-service核心单体（网关+消息+在线状态+音视频）
- 开发im-business-service业务单体（权限+媒体+XXL-Job任务）
- 开发im-admin-service管理服务
- 实现NATS通信接口

#### 阶段3：集成测试与优化
- 端到端功能测试
- 性能测试和压力测试
- 监控告警体系完善

#### 阶段4：生产环境部署
- 灰度发布和流量切换
- 生产环境监控和运维
- 持续优化和迭代

---

## 17. 分布式架构优势与特点

### 17.1 相比微服务的优势

#### 部署运维优势
- **部署简单**: 核心服务一个JAR包，业务服务一个JAR包，无需复杂的容器编排
- **运维友好**: 不需要服务发现、熔断、链路追踪等复杂组件
- **资源管理**: 每个服务独立管理资源，避免资源竞争

#### 开发效率优势
- **共享数据库**: 避免分布式事务的复杂性
- **技术栈统一**: 所有服务使用相同的技术栈，降低学习成本
- **代码复用**: 共享库和公共组件，减少重复开发

#### 性能优势
- **内部零延迟**: 核心服务内部直接方法调用，无网络开销
- **共享内存**: 连接信息、用户状态、会话数据在内存中共享
- **批量处理**: 支持消息批量处理，提高吞吐量

### 17.2 相比传统单体的优势

#### 扩展性优势
- **独立扩展**: 每个服务可以独立扩缩容
- **故障隔离**: 单个服务故障不影响其他服务
- **技术选型**: 不同服务可以使用最适合的技术栈

#### 团队协作优势
- **并行开发**: 多个团队可以并行开发不同服务
- **职责清晰**: 每个服务职责明确，边界清晰
- **独立测试**: 每个服务可以独立测试和部署

### 17.3 架构特点总结

#### 核心特征
- **分布式**: 3个服务分布式部署，通过NATS通信
- **单体**: 核心服务和业务服务内部保持单体架构的简单性
- **共享**: 共享数据库、缓存、配置等基础设施
- **异步**: 基于消息的异步通信，松耦合设计
- **高性能**: 核心服务和业务服务内部零网络延迟，共享内存

### 17.4 共享库优势

#### 代码复用与一致性
- **统一标准**: 所有服务使用相同的通信模式、错误处理、监控方式
- **减少重复**: 避免各服务重复实现通用功能
- **版本控制**: 统一管理依赖版本，避免版本不一致问题

#### 开发效率提升
- **专注业务**: 开发人员可以专注于业务逻辑，而不是基础设施
- **快速上手**: 新团队成员可以快速理解和使用标准组件
- **质量保证**: 共享组件经过全面测试和优化

#### 潜在挑战与对策
- **依赖耦合**: 所有服务依赖同一个common模块，版本升级需要协调
  - 对策: 严格的向后兼容性保证、语义化版本控制
- **抽象开销**: 额外的抽象层可能引入少量性能开销
  - 对策: 关键路径优化、提供直接访问原生客户端的方法
- **特殊需求**: 特定服务的特殊需求可能难以在通用模块中满足
  - 对策: 提供扩展点和插件机制，允许定制行为

---

## 18. 配置与运行参数

### 18.1 核心服务配置

#### 基础参数
- 心跳超时：90s
- 最大消息体：文本32KB，二进制256KB
- 写合并窗口：2~5ms
- 每连接发送队列：上限100条或1MB

#### 内部模块配置
- 网关模块：最大连接数2.5万，心跳间隔30s
- 消息模块：批量写入大小200条，分片数32
- 在线状态模块：缓存TTL 120s，故障检测间隔5s

### 18.2 业务服务配置

#### 基础参数
- 权限模块：权限缓存TTL 300s，权限校验超时5s
- 媒体模块：上传文件大小限制100MB，转码超时300s
- XXL-Job模块：执行器端口9999，注册方式自动注册，任务线程池大小20

#### 内部模块配置
- 权限模块：权限缓存大小1000条，权限校验QPS 10000
- 媒体模块：并发上传数100，转码并发数50
- XXL-Job模块：日志保留天数30，失败重试次数3，任务超时时间600s

### 18.3 消息分发配置

#### 性能参数
- 分片数：按核心数×2起步
- 批量写：50~200条/次或5~20ms
- 幂等窗口：7天
- 冷热分界：30天

### 18.4 在线状态配置

#### 缓存参数
- TTL：路由120s，在线状态滑动窗口2×心跳周期
- 会话与成员事实：TTL 60~120s，变更主动失效

### 18.5 限流配置

#### 限流规则
- 文本：用户级30 msg/min
- 媒体：10 item/min
- 大群会话级：500 msg/s

---

## 19. 典型时序流程

### 19.1 发送消息流程

#### 详细步骤
1. 客户端→核心服务网关模块：SendMessageReq（或先媒资上传）
2. 网关模块：快速校验通过→内部调用消息模块；否则NACK
3. 消息模块：通过NATS调用业务服务权限校验→分配seq→落库→内部调用在线状态模块查询在线成员
4. 网关模块：向发送方回Ack；向接收方推送消息

### 19.2 断线重连流程

#### 重连策略
1. 客户端：带上lastSeq清单
2. 网关模块：鉴权通过→查询缺口→指示增量拉取或服务端主动补发

---

## 20. 代码与生成规范

### 20.1 Protobuf规范

#### 代码生成
- 统一生成脚本
- CI校验生成物与proto一致性
- 禁止手改生成代码
- 只在shared-libs提交

### 20.2 依赖管理

#### 版本控制
- shared-libs发布版本号
- 服务按版本依赖
- 禁止跨模块源码引用

### 20.3 质量门槛

#### 测试要求
- 单元测试覆盖率门槛
- 端到端测试覆盖发送/拉取/断线/权限/媒体路径

### 20.4 安全扫描

#### 安全检查
- 依赖漏洞扫描
- 敏感信息泄露检查
- 证书与密钥轮换策略

---

## 21. 风险与对策清单

### 21.1 技术风险

#### 大群热点风险
- **风险**: Redis热键导致性能下降
- **对策**: 随机过期、局部缓存、分层计数、限制即时未读精度

#### 扇出放大风险
- **风险**: 队列积压影响系统性能
- **对策**: 批量处理、优先级队列、消息降采样（提示+拉取）

#### 幂等表膨胀
- **风险**: 幂等表数据量过大
- **对策**: TTL清理、冷热分层、按天分表

### 21.2 业务风险

#### 升级兼容风险
- **风险**: 协议升级导致不兼容
- **对策**: 先消费后生产、灰度流量小范围验证、回滚预案

#### 媒体滥用风险
- **风险**: 媒体直传被恶意利用
- **对策**: STS最小权限与短时效、大小与频率限流、审计告警

---

## 22. 最小可行实现（MVP）

### 22.1 核心功能

#### 基础消息
- 支持文本、图片引用消息
- 单聊和小型群聊

#### 连接管理
- WS连接、心跳、ACK
- 历史拉取与未读游标

#### 权限控制
- 基础权限（成员关系、禁言）
- 媒体直传与内容安全占位

### 22.2 观测能力

#### 监控指标
- 基础指标+采样日志+Trace
- 压测脚本与回归用例

---

## 附录

### 附录A：错误码对照表
| 错误码 | 错误名称 | 说明 |
|--------|----------|------|
| 1001 | AUTH_FAILED | Token无效或过期 |
| 1003 | FORBIDDEN | 无权限/被禁言/被拉黑 |
| 1005 | RATE_LIMITED | 频控触发 |
| 1007 | PAYLOAD_INVALID | 字段或大小非法 |
| 1010 | MEDIA_REQUIRED | 媒体未上传或引用无效 |
| 1012 | IDEMPOTENT_CONFLICT | 幂等冲突 |
| 1099 | SERVER_BUSY | 背压 |
| 1999 | INTERNAL_ERROR | 内部错误 |

### 附录B：观测字段建议
- **Envelope.header**: traceId、clientMsgId、deviceId、codecVersion、sendTs
- **推送事件**: deliverTs、ackTs、storeLatencyMs、fanoutCount

### 附录C：数据保密与合规建议
- PII脱敏、数据最小化、日志留存周期、访问审计、导出加密

---

## 文档版本信息

- **版本**: 4.2.0
- **最后更新**: 2024年
- **维护团队**: IM架构组
- **架构模式**: 分布式单体架构（核心服务+业务服务合并优化）
- **通信方式**: 服务内部直接调用 + NATS消息总线 + 共享库
- **审核状态**: 已审核

---

## 5. 消息发送路径与权限校验

### 5.4 权限校验超时决策流程

#### 权限校验时序图
```
┌─────────┐          ┌─────────┐          ┌─────────────┐          ┌─────────────┐
│ 客户端   │          │ 网关模块  │          │ 消息模块     │          │ 权限模块     │
└────┬────┘          └────┬────┘          └──────┬──────┘          └──────┬──────┘
     │                    │                      │                        │
     │   发送消息请求       │                      │                        │
     │───────────────────>│                      │                        │
     │                    │                      │                        │
     │                    │   快速校验(本地缓存)    │                        │
     │                    │─────────────────────>│                        │
     │                    │                      │                        │
     │                    │                      │    权限校验请求          │
     │                    │                      │───────────────────────>│
     │                    │                      │                        │
     │                    │                      │                        │
     │                    │                      │                        │
     │                    │                      │        超时(300ms)      │
     │                    │                      │<───────────────────────│
     │                    │                      │                        │
     │                    │                      │   查询消息/会话类型       │
     │                    │                      │───────────────────────>│
     │                    │                      │                        │
     │                    │                      │   应用默认策略矩阵        │
     │                    │                      │<──────────────────────>│
     │                    │                      │                        │
     │                    │                      │   决策: 拒绝/放行+审计    │
     │                    │                      │                        │
     │   返回结果           │                      │                        │
     │<───────────────────│<─────────────────────│                        │
     │                    │                      │                        │
     │                    │                      │   异步审计记录(如放行)    │
     │                    │                      │───────────────────────>│
     │                    │                      │                        │
```

#### 安全网策略矩阵

##### 敏感场景超时拒绝策略
| 场景类型 | 超时阈值 | 默认决策 | 审计级别 | 风险等级 |
|---------|---------|---------|---------|---------|
| 大群(>1000人) | 100ms | 拒绝 | 高 | 高 |
| 公司/学校管控群 | 150ms | 拒绝 | 高 | 高 |
| 公开群/官方群 | 200ms | 拒绝 | 中 | 中 |
| 普通群(<100人) | 300ms | 放行+审计 | 中 | 低 |
| 单聊 | 300ms | 放行+审计 | 低 | 低 |

##### 超时分级处理策略
1. **一级超时(100-150ms)**: 敏感场景直接拒绝，记录告警
2. **二级超时(150-200ms)**: 管控群拒绝，普通群放行+审计
3. **三级超时(200-300ms)**: 应用默认策略矩阵
4. **超时(>300ms)**: 触发熔断，应用保守策略

#### 默认决策策略
1. **超时判定**：权限校验请求超过300ms未返回视为超时
2. **降级触发**：
   - 单次超时：应用默认策略矩阵
   - 连续超时>3次：触发熔断，应用保守策略
   - 超时率>5%持续1分钟：触发全局降级，记录告警
3. **恢复策略**：
   - 半开状态：60秒后尝试恢复正常校验
   - 成功率>95%持续2分钟：完全恢复

#### 异常补偿措施
1. **审计记录**：所有降级决策记录详细日志，包括请求ID、用户ID、会话ID、消息类型、决策结果
2. **事后校验**：对放行的消息进行异步权限校验，发现违规时：
   - 标记消息状态为REVOKED
   - 向发送方推送撤回通知
   - 向接收方推送消息不可见通知
3. **告警升级**：超时率>10%持续5分钟触发人工介入告警

#### 限流与熔断策略
1. **限流策略**：
   - 敏感场景：超时率>3%时触发限流，降低QPS至50%
   - 普通场景：超时率>5%时触发限流，降低QPS至80%
2. **熔断策略**：
   - 连续超时>5次：熔断30秒
   - 超时率>10%持续2分钟：熔断5分钟
   - 熔断期间：敏感场景拒绝，普通场景放行+审计
3. **灰度策略**：
   - 按群组类型逐步放开超时放行策略
   - 按用户等级逐步放开超时放行策略
   - 支持动态配置调整策略参数

---

## 23. 默认策略与SLO矩阵

### 23.1 权限校验默认策略矩阵

| 会话类型 | 消息类型 | 通道状态:正常 | 通道状态:降级 | 通道状态:超时 | 审计级别 |
|---------|---------|-------------|-------------|-------------|---------|
| 单聊(P2P) | 文本消息 | 校验通过放行 | 本地缓存+放行 | 放行+异步审计 | 低 |
| 单聊(P2P) | 媒体消息 | 校验通过放行 | 本地缓存+放行 | 放行+异步审计 | 中 |
| 单聊(P2P) | 控制消息 | 校验通过放行 | 本地缓存+放行 | 拒绝 | 低 |
| 群聊(普通) | 文本消息 | 校验通过放行 | 本地缓存+放行 | 放行+异步审计 | 中 |
| 群聊(普通) | 媒体消息 | 校验通过放行 | 本地缓存+放行 | 拒绝 | 高 |
| 群聊(普通) | 控制消息 | 校验通过放行 | 本地缓存+放行 | 拒绝 | 中 |
| 群聊(敏感) | 文本消息 | 校验通过放行 | 严格校验+放行 | 拒绝 | 高 |
| 群聊(敏感) | 媒体消息 | 校验通过放行 | 严格校验+放行 | 拒绝 | 高 |
| 群聊(敏感) | 控制消息 | 校验通过放行 | 严格校验+放行 | 拒绝 | 高 |
| 系统通知 | 所有类型 | 校验通过放行 | 严格校验+放行 | 放行+全量审计 | 高 |

**说明**：
- **通道状态:正常**：权限服务响应时间<100ms，正常校验
- **通道状态:降级**：权限服务响应时间100-300ms或错误率1-5%，采用本地缓存辅助决策
- **通道状态:超时**：权限服务响应时间>300ms或错误率>5%，采用默认策略
- **敏感群组**：由管理员标记的需要特殊控制的群组，如大型公开群、官方群等
- **审计级别**：决定异步审计的优先级和详细程度

### 23.2 ACK与消息状态机

#### 消息状态定义
- **SENDING**：客户端已发送，服务端未确认
- **ACCEPTED**：服务端已接收，已入队待处理
- **STORED**：已分配序号并确认存储
- **DELIVERED**：已投递给接收方
- **READ**：接收方已读取
- **REVOKED**：消息已撤回或被系统删除
- **FAILED**：发送失败

#### 状态转换与失败处理
```
┌─────────┐                              ┌─────────────┐
│ 客户端   │                              │ 服务端       │
└────┬────┘                              └──────┬──────┘
     │                                          │
     │  发送消息(SENDING)                        │
     │─────────────────────────────────────────>│
     │                                          │ 接收消息
     │                                          │ 快速校验通过
     │                                          │ 消息入队
     │  ACK(clientMsgId, status=ACCEPTED)       │
     │<─────────────────────────────────────────│
     │                                          │
     │  更新UI状态(ACCEPTED)                     │ 分配序号
     │  (显示为"发送中")                         │ 存储消息
     │                                          │
     │  ACK(serverMsgId, seq, status=STORED)    │
     │<─────────────────────────────────────────│
     │                                          │
     │  更新UI状态(STORED)                       │
     │  (显示为"已发送")                         │
     │                                          │ 开始扇出
     │                                          │
     │  消息投递通知(可选)                        │
     │<─────────────────────────────────────────│
     │                                          │
     │  更新UI状态(DELIVERED)                    │
     │  (显示为"已送达")                         │
```

#### 存储失败处理机制

##### 存储失败状态回滚
1. **存储失败检测**：
   - 数据库写入失败
   - 序号分配失败
   - 缓存更新失败
2. **失败回滚流程**：
   - 向客户端推送 `ACK(status=FAILED, errorCode, errorMsg)`
   - 客户端收到失败ACK后，将消息状态回滚至SENDING
   - 客户端显示"发送失败"状态，提供重试按钮
3. **自动重试策略**：
   - 服务端自动重试3次，间隔递增(1s, 3s, 5s)
   - 重试成功后推送 `ACK(status=STORED)`
   - 重试失败后推送 `ACK(status=FAILED)`

##### 客户端自我修复流程
1. **断网重连检测**：
   - 检测到网络恢复后，主动查询所有SENDING状态消息
   - 向服务端发送 `QueryMessageStatus(clientMsgId)` 请求
2. **状态同步**：
   - 服务端返回消息实际状态
   - 客户端根据返回状态更新本地UI
   - 对于ACCEPTED状态消息，等待STORED确认
3. **失败消息处理**：
   - 对于FAILED状态消息，显示重试选项
   - 对于REVOKED状态消息，显示撤回通知
   - 对于DELIVERED状态消息，更新为已送达状态

#### 消息状态一致性保障
1. **幂等性保证**：
   - 使用clientMsgId作为幂等键
   - 重复的clientMsgId直接返回已存在的状态
2. **状态同步机制**：
   - 客户端定期同步消息状态
   - 服务端主动推送状态变更通知
3. **异常恢复策略**：
   - 服务端重启后，从数据库恢复消息状态
   - 客户端重启后，从服务端同步消息状态
   - 网络异常后，自动重连并同步状态

---

## 24. 高可用与容灾设计

### 24.1 NATS集群拓扑与高可用

#### 集群拓扑设计
```
                  ┌─────────────────────────────────────────────┐
                  │               负载均衡层                     │
                  └───────────────────┬─────────────────────────┘
                                      │
┌─────────────────────────┬───────────┴──────────┬─────────────────────────┐
│                         │                      │                         │
▼                         ▼                      ▼                         ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  NATS节点1      │ │  NATS节点2      │ │  NATS节点3      │ │  NATS节点4      │
│  (AZ-1)         │ │  (AZ-1)         │ │  (AZ-2)         │ │  (AZ-2)         │
└─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘
       │                   │                   │                   │
       └───────────────────┴───────────────────┴───────────────────┘
                                      │
                                      ▼
┌─────────────────────────┬───────────┴──────────┬─────────────────────────┐
│                         │                      │                         │
▼                         ▼                      ▼                         ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  JetStream      │ │  JetStream      │ │  JetStream      │ │  JetStream      │
│  存储节点1       │ │  存储节点2       │ │  存储节点3       │ │  存储节点4       │
│  (AZ-1)         │ │  (AZ-1)         │ │  (AZ-2)         │ │  (AZ-2)         │
└─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘
```

#### 跨可用区部署策略
- **节点分布**：至少跨两个可用区部署，每个可用区至少2个节点
- **仲裁策略**：采用N/2+1仲裁机制，确保单可用区故障时集群仍可用
- **JetStream存储**：数据复制因子为3，确保数据可靠性
- **路由节点**：每个可用区部署专用路由节点，优化跨区流量

#### 客户端连接策略
- **连接池设计**：
  - 初始连接数：CPU核心数 × 0.75
  - 最大连接数：CPU核心数 × 1.5
  - 连接存活检测：30秒心跳
  - 空闲超时：5分钟

- **优先就近连接**：
  - 客户端优先连接同可用区NATS节点
  - 连接信息包含区域标识，用于路由决策
  - 故障时自动切换到其他可用区节点

- **断线重连策略**：
  - 初始重试间隔：100ms
  - 最大重试间隔：30s
  - 指数退避因子：1.5
  - 抖动因子：0.2
  - 最大重试次数：无限（持续尝试）

#### 发布确认与流控
- **发布确认(PubAck)**：
  - 关键消息启用发布确认
  - 确认超时：2秒
  - 未确认处理：重试或降级

- **背压处理(Backpressure)**：
  - 发布侧缓冲区：1MB或1000条消息
  - 缓冲区满策略：阻塞或拒绝
  - 背压指标监控：`nats_client_pending_bytes`、`nats_client_pending_msgs`

#### 请求/响应模式优化
- **超时控制**：
  - 默认请求超时：2秒
  - 权限校验请求：300ms
  - 配置查询请求：500ms
  - 历史查询请求：1秒

- **重试策略**：
  - 快速失败请求：不重试（如权限校验）
  - 幂等请求：最多重试2次，间隔100ms
  - 非幂等请求：不自动重试，返回错误由业务层处理

- **幂等处理**：
  - 请求ID格式：`{serviceId}-{timestamp}-{sequence}`
  - 幂等窗口：60秒
  - 幂等存储：Redis Sorted Set，按时间戳淘汰

### 24.2 Redis热键与容量风险管理

#### 热键识别与防御
- **热键定义**：
  - 访问频率：>1000次/秒
  - 响应时间：P99 >1ms
  - 内存占用：单键>1MB

- **热键识别方法**：
  - 定期采样：使用`redis-cli --hotkeys`
  - 实时监控：使用`MONITOR`命令采样分析
  - 自动化工具：Redis热键分析脚本
  ```bash
  redis-cli -h $HOST -p $PORT --hotkeys -i 0.1 -d 5
  ```

- **热键防御策略**：
  ```
  ┌─────────────────┐
  │  应用层         │
  │  本地缓存       │──┐
  └─────────────────┘  │ 缓存未命中
                       ▼
  ┌─────────────────┐  │ 首次查询
  │  Redis主节点    │◄─┘
  └─────────────────┘
          │
          │ 复制
          ▼
  ┌─────────────────┐
  │  Redis从节点    │
  │  只读查询       │
  └─────────────────┘
  ```

#### 大群数据结构优化
- **在线成员存储优化**：
  ```
  # 传统方式(可能成为热键)
  subscribers:conv:{conversationId} → Set(userId1, userId2, ...)
  
  # 优化方式(分桶)
  subscribers:conv:{conversationId}:bucket:{bucketId} → Set(userId1, userId2, ...)
  ```

- **分桶策略**：
  - 桶数量：成员数/1000，至少10个桶
  - 分桶算法：`bucketId = hash(userId) % bucketCount`
  - 查询策略：并行查询多个桶，合并结果

- **数据结构选择**：
  - 小群(<100人)：使用SET
  - 中群(100-1000人)：使用ZSET，按活跃度排序
  - 大群(>1000人)：分桶+BITMAP

#### Redis Cluster槽位优化
- **键名设计**：
  - 使用`{}`定义哈希标签：`presence:{userId}`
  - 确保相关键在同一槽位：`route:{deviceId}`和`presence:{userId}`使用相同标签

- **槽位分布监控**：
  - 定期检查槽位分布均衡性
  - 热点槽位识别：`redis-cli --bigkeys`结合`CLUSTER NODES`
  - 自动均衡脚本示例：
  ```bash
  for slot in $(redis-cli cluster slots | grep "hot-slot"); do
    redis-cli cluster setslot $slot migrating $TARGET_NODE_ID
  done
  ```

#### 容量规划与扩容策略
- **容量指标**：
  - 内存使用率阈值：70%
  - 连接数阈值：每节点10000
  - CPU使用率阈值：65%

- **扩容触发条件**：
  - 内存使用率>60%持续7天
  - 连接数>8000持续3天
  - CPU使用率>50%持续5天

- **扩容步骤**：
  1. 添加新节点到集群
  2. 重新分片，迁移槽位
  3. 监控迁移进度和性能影响
  4. 更新客户端配置

- **热点迁移演练**：
  1. 识别热点键和槽位
  2. 创建影子键，双写测试
  3. 切换读流量到影子键
  4. 完全迁移并清理原键

### 24.3 Presence路由一致性与跨节点广播

#### 路由一致性目标
- **收敛时间目标**：
  - 单区域：<500ms
  - 跨区域：<2000ms

- **一致性级别**：
  - 默认：最终一致性
  - 关键操作：读写一致性（通过锁或版本控制）

- **校验机制**：
  - 定期校验任务：每10分钟运行一次
  - 不一致修复：自动同步Redis与本地缓存

#### 跨节点广播优化
- **广播查询优化**：
  ```java
  // 优化前
  function broadcastQuery(userId) {
    // 向所有节点广播查询
    return natsClient.request("im.presence.query", userId);
  }
  
  // 优化后
  function broadcastQuery(userId) {
    // 1. 计算可能的节点范围(一致性哈希)
    List<String> possibleNodes = consistentHash.getPossibleNodes(userId);
    
    // 2. 并行查询可能节点，限制范围
    CompletableFuture<?>[] futures = possibleNodes.stream()
      .map(node -> natsClient.request("im.presence.query." + node, userId))
      .toArray(CompletableFuture<?>[]::new);
    
    // 3. 等待第一个成功响应或全部失败
    return CompletableFuture.anyOf(futures)
      .orTimeout(200, TimeUnit.MILLISECONDS);
  }
  ```

- **广播效率提升**：
  - 一致性哈希限制查询范围
  - 并行查询与首个响应优先
  - 超时控制避免长尾延迟

#### 连接迁移与优雅摘流
- **节点下线步骤**：
  1. 标记节点为"准备下线"状态
  2. 停止接受新连接
  3. 通知负载均衡器停止转发流量
  4. 向现有连接发送CODE_RECONNECT指令
  5. 等待连接数降至阈值(如总数的10%)或超时(2分钟)
  6. 强制关闭剩余连接
  7. 完全下线节点

- **优雅摘流流程**：
  ```
  ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
  │ 负载均衡器   │          │ 待下线节点   │          │ 活跃节点     │
  └──────┬──────┘          └──────┬──────┘          └──────┬──────┘
         │                        │                        │
         │                        │ 1. 发送准备下线信号      │
         │                        │───────────────────────>│
         │                        │                        │
         │ 2. 更新节点权重(0)      │                        │
         │<───────────────────────│                        │
         │                        │                        │
         │ 3. 停止新连接转发       │                        │
         │─────────────────────────────────────────────────>│
         │                        │                        │
         │                        │ 4. 向客户端发送重连指令   │
         │                        │───────────────────────>│
         │                        │                        │
         │                        │ 5. 等待连接迁移         │
         │                        │<──────────────────────>│
         │                        │                        │
         │                        │ 6. 通知迁移完成         │
         │                        │───────────────────────>│
         │                        │                        │
         │ 7. 节点完全下线         │                        │
         │<───────────────────────│                        │
         │                        │                        │
  ```

- **滚动升级策略**：
  - 按区域依次升级，每次不超过1/3的节点
  - 每个节点升级后验证健康状态
  - 升级间隔：至少5分钟，确保系统稳定
  - 回滚触发条件：错误率上升>0.5%或延迟上升>50%

#### 路由数据校验任务
- **校验周期**：每10分钟执行一次
- **校验范围**：随机抽样1%的活跃用户
- **校验流程**：
  1. 对比Redis路由数据与各节点本地缓存
  2. 检测孤立路由（用户已下线但路由仍存在）
  3. 检测缺失路由（用户在线但路由不存在）
  4. 自动修复不一致数据
- **校验指标**：
  - `route_inconsistency_rate`: 路由不一致率
  - `route_repair_count`: 修复次数
  - `orphaned_route_count`: 孤立路由数

### 24.4 安全与合规边界细化

#### 数据域合规责任分界
- **消息内容**：
  - 存储位置：MongoDB
  - 保留期限：普通消息30天热存储+150天冷存储
  - 责任主体：平台方负责存储安全，内容合规由用户负责
  - 审计能力：支持按会话ID、用户ID、时间范围、关键词检索

- **媒体文件**：
  - 存储位置：MinIO/对象存储
  - 保留期限：默认90天，重要媒体365天
  - 责任主体：平台方负责存储和基础内容安全扫描
  - 审计能力：支持按上传者、文件类型、大小、时间检索

- **音视频记录**：
  - 存储位置：声网云存储
  - 保留期限：默认30天，标记重要可延长至90天
  - 责任主体：平台方与声网共同负责，需明确告知用户录制行为
  - 法务要求：录制前必须获得用户明确同意，界面显示录制状态

#### 数据安全保障措施
- **数据加密**：
  - 传输加密：TLS 1.3
  - 存储加密：AES-256-GCM
  - 密钥管理：使用KMS，自动轮换

- **密钥轮换周期**：
  - 传输密钥：30天
  - 存储密钥：90天
  - 签名密钥：180天

- **跨境传输控制**：
  - 数据本地化：用户数据存储在用户所属区域
  - 区域隔离：不同区域的数据不互通
  - 跨境审批：需要特殊授权才能进行跨境数据访问

#### 客户端鉴权与会话安全
- **Token设计**：
  ```
  {
    "alg": "HS256",
    "typ": "JWT"
  }
  {
    "sub": "userId",
    "deviceId": "deviceUniqueId",
    "iat": 1516239022,
    "exp": 1516242622,
    "scopes": ["message:send", "presence:update"],
    "nonce": "randomString",
    "ver": "1.0"
  }
  ```

- **Token策略**：
  - 有效期：1小时
  - 自动续期：剩余时间<15分钟时刷新
  - 最大续期：7天，之后需重新登录
  - 设备绑定：Token与设备ID绑定

- **Token撤销机制**：
  - 主动撤销：用户登出或管理员操作
  - 被动撤销：密码变更、异常登录
  - 撤销传播：通过Redis发布订阅+NATS广播

- **设备黑名单**：
  - 存储位置：Redis集合
  - 判定条件：短时间内多次认证失败、异常行为检测
  - 解除策略：24小时后自动解除或管理员手动解除
  - 黑名单同步：实时同步到所有网关节点

#### 审计与合规查询
- **审计日志**：
  - 关键操作：登录、发送敏感消息、权限变更、管理操作
  - 日志字段：操作类型、用户ID、IP、设备信息、时间戳、操作结果
  - 存储位置：专用审计日志库，独立访问控制

- **合规检索能力**：
  - 最大检索时间范围：180天
  - 检索响应时间：
    - 热数据(30天内)：<10秒
    - 冷数据(30-180天)：<5分钟
  - 导出格式：CSV、JSON、PDF(带数字签名)

- **访问控制**：
  - 基于角色：审计员、合规官、管理员
  - 操作记录：所有检索操作留痕
  - 敏感导出：需二次授权和双人审批

### 24.5 RTC模块资源与QoS管理

#### 并发会议限制
- **系统级限制**：
  - 最大并发会议数：5000
  - 单会议最大参与者：50
  - 单用户最大并发会议：3

- **资源控制**：
  - Token生成QPS上限：5000/秒
  - 房间创建QPS上限：500/秒
  - 信令处理QPS上限：10000/秒

#### Token生成性能优化
- **生成策略**：
  - 批量预生成：高峰期前预生成Token池
  - 缓存复用：相同参数的Token短时间内复用
  - 异步生成：非阻塞Token生成

- **性能指标**：
  - 单Token生成时间：<5ms
  - 批量生成(100个)：<200ms
  - 内存占用：每1000个缓存Token约2MB

#### 回调处理与幂等性
- **回调接收设计**：
  - 接收限流：每节点1000 QPS
  - 处理超时：2秒
  - 队列缓冲：最大10000条

- **幂等处理**：
  - 幂等键：`{eventType}:{resourceId}:{timestamp}`
  - 幂等窗口：5分钟
  - 重复事件处理：忽略或更新最新状态

#### 防刷策略
- **限制规则**：
  - 单用户创建会议：10次/小时
  - 单IP创建会议：50次/小时
  - 加入会议尝试：20次/分钟

- **异常检测**：
  - 短时间内多次加入/退出：标记可疑
  - 频繁切换设备：要求额外验证
  - 跨地域快速登录：触发安全检查

#### 网络分区与弱网策略
- **网络质量分级**：
  - 优良：RTT<100ms，丢包<1%
  - 良好：RTT<200ms，丢包<3%
  - 一般：RTT<500ms，丢包<5%
  - 较差：RTT<1000ms，丢包<10%
  - 极差：RTT>1000ms或丢包>10%

- **自动降级策略**：
  - 网络较差时：降低视频分辨率，优先保证音频
  - 网络极差时：仅保留音频，关闭视频
  - 完全断连：保持会议状态5分钟，等待重连

- **弱网优化**：
  - 丢包补偿：FEC前向纠错
  - 自适应码率：根据网络状况动态调整
  - 优先级控制：音频>共享屏幕>视频
  - 重连策略：指数退避，最长保持会话10分钟

---

## 文档版本信息

- **版本**: 5.0.0
- **最后更新**: 2024年
- **维护团队**: IM架构组
- **架构模式**: 分布式单体架构（核心服务+业务服务合并优化）
- **通信方式**: 服务内部直接调用 + NATS消息总线 + 共享库
- **审核状态**: 已审核

## 附录A：版本变更历史

| 版本   | 日期      | 主要变更                                          |
|-------|-----------|--------------------------------------------------|
| 5.0.0 | 2024年    | 增加高可用设计、一致性策略、热键防御、安全合规细节 |
| 4.4.0 | 2024年    | 增加声网集成方案、通话流程、录制与回放            |
| 4.3.0 | 2024年    | 增加默认策略与SLO矩阵、权限校验决策流程           |
| 4.2.0 | 2024年    | 完善数据库职责、NATS可靠性、幂等设计              |
| 4.1.0 | 2024年    | 增加共享库模块设计                                |
| 4.0.0 | 2024年    | 合并为三个主要服务：核心、业务、管理               |
| 3.0.0 | 2024年    | 合并网关、消息、在线状态为核心服务                 |
| 2.0.0 | 2024年    | 初始分布式单体架构，六个独立服务                   |
| 1.0.0 | 2024年    | 初始架构设计草案                                  |

## 25. 音视频服务集成方案

### 25.1 声网(Agora)集成架构

#### 集成模式
- **云服务模式**: 使用声网云服务进行音视频传输，本地系统负责信令和业务逻辑
- **服务端集成**: 通过声网服务端API进行Token生成、录制控制、事件回调
- **客户端集成**: 客户端通过声网SDK直接连接声网服务

#### 系统职责划分
- **im-core-service职责**:
  - 房间创建与管理
  - 通话邀请与信令
  - Token生成与分发
  - 通话状态同步
  - 通话记录存储
  - 质量数据收集

- **声网服务职责**:
  - 音视频数据传输
  - 媒体服务器调度
  - 音视频编解码
  - 网络质量保障
  - 云端录制(可选)

#### 接口设计
```java
// Token生成接口
public interface AgoraTokenService {
    /**
     * 生成RTC Token
     * @param channelName 频道名
     * @param uid 用户ID
     * @param role 角色(发布者/订阅者)
     * @param expireTime 过期时间(秒)
     * @return RTC Token
     */
    String generateRtcToken(String channelName, int uid, String role, int expireTime);
    
    /**
     * 生成RTM Token
     * @param userId 用户ID
     * @param expireTime 过期时间(秒)
     * @return RTM Token
     */
    String generateRtmToken(String userId, int expireTime);
}

// 云端录制接口
public interface AgoraCloudRecording {
    /**
     * 开始云端录制
     * @param channelName 频道名
     * @param uid 录制用户ID
     * @param storageConfig 存储配置
     * @return 录制ID
     */
    String startRecording(String channelName, int uid, StorageConfig storageConfig);
    
    /**
     * 停止云端录制
     * @param channelName 频道名
     * @param recordingId 录制ID
     * @return 录制文件信息
     */
    RecordingInfo stopRecording(String channelName, String recordingId);
}
```

#### 配置参数
```yaml
agora:
  appId: "your_app_id"
  appCertificate: "your_app_certificate"
  customerId: "your_customer_id"
  customerSecret: "your_customer_secret"
  recording:
    bucket: "recording-bucket"
    accessKey: "your_access_key"
    secretKey: "your_secret_key"
    region: "ap-northeast-1"
  callback:
    url: "https://your-domain.com/api/v1/agora/callback"
    authKey: "your_callback_auth_key"
```

### 25.2 通话流程

#### 一对一通话流程
```
┌─────────┐          ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
│ 发起方   │          │ im-core-service │      │ 接收方       │          │ 声网服务     │
└────┬────┘          └──────┬──────┘          └──────┬──────┘          └──────┬──────┘
     │                      │                        │                        │
     │   发起通话请求         │                        │                        │
     │─────────────────────>│                        │                        │
     │                      │                        │                        │
     │                      │   创建房间              │                        │
     │                      │───────────────────────>│                        │
     │                      │                        │                        │
     │                      │   生成Token             │                        │
     │                      │────────────────────────────────────────────────>│
     │                      │                        │                        │
     │                      │   Token返回             │                        │
     │                      │<────────────────────────────────────────────────│
     │                      │                        │                        │
     │   返回Token和房间信息   │                        │                        │
     │<─────────────────────│                        │                        │
     │                      │                        │                        │
     │                      │   发送通话邀请           │                        │
     │                      │───────────────────────>│                        │
     │                      │                        │                        │
     │                      │   接受通话              │                        │
     │                      │<───────────────────────│                        │
     │                      │                        │                        │
     │                      │   生成Token             │                        │
     │                      │────────────────────────────────────────────────>│
     │                      │                        │                        │
     │                      │   Token返回             │                        │
     │                      │<────────────────────────────────────────────────│
     │                      │                        │                        │
     │                      │   返回Token和房间信息    │                        │
     │                      │───────────────────────>│                        │
     │                      │                        │                        │
     │   加入声网频道          │                        │   加入声网频道          │
     │────────────────────────────────────────────────────────────────────────>│
     │                      │                        │                        │
     │                      │                        │                        │
     │                      │                        │                        │
     │   实时音视频通话(P2P)   │                        │                        │
     │<───────────────────────────────────────────────────────────────────────>│
     │                      │                        │                        │
```

#### 群组通话流程
```
┌─────────┐          ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
│ 发起方   │          │ im-core-service │      │ 多个接收方    │          │ 声网服务     │
└────┬────┘          └──────┬──────┘          └──────┬──────┘          └──────┬──────┘
     │                      │                        │                        │
     │   创建群组通话         │                        │                        │
     │─────────────────────>│                        │                        │
     │                      │                        │                        │
     │                      │   创建房间              │                        │
     │                      │───────────────────────>│                        │
     │                      │                        │                        │
     │                      │   生成Token             │                        │
     │                      │────────────────────────────────────────────────>│
     │                      │                        │                        │
     │                      │   Token返回             │                        │
     │                      │<────────────────────────────────────────────────│
     │                      │                        │                        │
     │   返回Token和房间信息   │                        │                        │
     │<─────────────────────│                        │                        │
     │                      │                        │                        │
     │                      │   发送群组通话邀请        │                        │
     │                      │───────────────────────>│                        │
     │                      │                        │                        │
     │   加入声网频道          │                        │                        │
     │────────────────────────────────────────────────────────────────────────>│
     │                      │                        │                        │
     │                      │                        │   接受通话(多个成员)      │
     │                      │<───────────────────────│                        │
     │                      │                        │                        │
     │                      │   为每个成员生成Token     │                        │
     │                      │────────────────────────────────────────────────>│
     │                      │                        │                        │
     │                      │   Token返回(批量)        │                        │
     │                      │<────────────────────────────────────────────────│
     │                      │                        │                        │
     │                      │   返回Token和房间信息    │                        │
     │                      │───────────────────────>│                        │
     │                      │                        │                        │
     │                      │                        │   加入声网频道(多个成员)   │
     │                      │                        │───────────────────────>│
     │                      │                        │                        │
     │                      │                        │                        │
     │   实时音视频通话(多人会议) │                      │                        │
     │<───────────────────────────────────────────────────────────────────────>│
     │                      │                        │                        │
```

### 25.3 录制与回放

#### 录制方案
- **触发条件**: 
  - 管理员开启录制功能
  - 特定会话类型自动录制(如公开会议)
  - 用户手动开启录制

- **存储策略**:
  - 录制文件存储在MinIO或云存储
  - 元数据(时长、参与者、时间)存储在MySQL
  - 录制文件链接与权限控制存储在Redis

- **录制流程**:
  1. 通过AgoraCloudRecording接口启动录制
  2. 录制过程中监控状态和存储空间
  3. 通话结束后停止录制
  4. 接收录制完成回调
  5. 更新录制元数据
  6. 生成访问链接

#### 回放权限控制
- **访问控制**:
  - 基于角色的权限控制
  - 时效性访问令牌
  - 防盗链和域名白名单
  - 水印和追溯标记

- **生命周期管理**:
  - 默认保留期: 30天
  - 重要录制: 180天
  - 自动清理策略
  - 手动归档功能

### 25.4 监控与质量保障

#### 监控指标
- **声网平台指标**:
  - 频道并发数
  - 带宽使用率
  - 通话质量分布
  - 异常掉线率

- **自定义指标**:
  - `rtc_call_success_rate`: 通话成功率
  - `rtc_call_duration_avg`: 平均通话时长
  - `rtc_token_generation_time`: Token生成时间
  - `rtc_join_channel_delay`: 加入频道延迟

#### 质量保障策略
- **预警机制**:
  - 通话质量低于阈值时告警
  - 频道异常波动监测
  - 区域性网络问题识别

- **降级策略**:
  - 视频降级为音频
  - 分辨率自动调整
  - 帧率动态控制
  - 弱网络补偿机制

---

## 文档版本信息

- **版本**: 5.0.0
- **最后更新**: 2024年
- **维护团队**: IM架构组
- **架构模式**: 分布式单体架构（核心服务+业务服务合并优化）
- **通信方式**: 服务内部直接调用 + NATS消息总线 + 共享库
- **审核状态**: 已审核

### 25.5 缓存治理与热点防护

#### 热门群缓存策略
1. **群成员列表分块存储**：
   - 大群(>500人)：按500人一块分片存储
   - 超大群(>5000人)：按1000人一块分片存储
   - 分块键格式：`group:{groupId}:members:{chunkId}`
   - 分块策略：`chunkId = memberIndex / chunkSize`

2. **超大群推送优化**：
   - 实时推送：仅推送"有新消息"通知
   - 详细拉取：用户点击进入时再拉取消息内容
   - 缓存策略：消息内容按需缓存，避免内存浪费

##### 缓存雪崩防护
1. **随机过期时间策略**：
   - 基础TTL：300秒
   - 随机偏移：±30秒，避免同时过期
   - 实现方式：`TTL = baseTTL + random(-30, 30)`

2. **热点键分散策略**：
   - 热门群：按群ID哈希分散到不同Redis节点
   - 用户会话：按userId哈希分散
   - 消息缓存：按conversationId哈希分散

##### 缓存容量控制
1. **内存限制策略**：
   - Redis最大内存：物理内存的80%
   - 淘汰策略：LRU + TTL混合
   - 监控告警：内存使用率>85%时告警

2. **热点数据治理**：
   - 自动识别：监控访问频率，识别热点键
   - 主动分散：将热点键分散到多个节点
   - 降级策略：热点键过多时，降低缓存命中率要求

#### 缓存性能优化
1. **批量操作优化**：
   - 群成员查询：使用Redis Pipeline批量获取
   - 消息缓存：批量写入，减少网络往返
   - 会话状态：批量更新，提高吞吐量

2. **本地缓存策略**：
   - 热点数据：在应用层维护本地缓存
   - 缓存同步：通过NATS事件同步本地缓存
   - 过期策略：本地缓存TTL < Redis TTL，避免数据不一致

### 25.6 重试风暴防护与熔断策略

#### 重试规则统一策略
1. **只允许一边重试原则**：
   - 前端重试：仅对网络错误进行重试，业务错误不重试
   - 后端重试：仅对系统内部错误进行重试，用户错误不重试
   - 避免双方同时重试，防止重试风暴

2. **重试策略配置**：
   - 权限校验：不重试，超时直接降级
   - 消息发送：前端不重试，后端自动重试3次
   - 媒体上传：前端重试3次，后端不重试
   - 配置查询：前端不重试，后端重试2次

#### 退避策略与限流
1. **指数退避策略**：
   - 第一次重试：延迟1秒
   - 第二次重试：延迟2秒
   - 第三次重试：延迟4秒
   - 最大重试间隔：30秒

2. **限流策略**：
   - 单用户重试频率：每分钟最多10次
   - 单服务重试频率：每分钟最多1000次
   - 全局重试频率：每分钟最多10000次

#### 熔断机制
1. **熔断触发条件**：
   - 错误率>50%持续1分钟：触发熔断
   - 响应时间>5秒持续2分钟：触发熔断
   - 连续失败>10次：触发熔断

2. **熔断状态管理**：
   - 关闭状态：正常处理请求
   - 开启状态：快速失败，不处理请求
   - 半开状态：允许少量请求尝试，成功后关闭熔断

3. **熔断恢复策略**：
   - 熔断开启后等待60秒进入半开状态
   - 半开状态下成功率>80%持续30秒，关闭熔断
   - 半开状态下失败率>50%，重新开启熔断

#### 监控与告警
1. **重试监控指标**：
   - 重试次数：按服务、接口、用户分类统计
   - 重试成功率：重试后成功的比例
   - 重试延迟：重试请求的响应时间

2. **告警阈值**：
   - 重试率>20%持续5分钟：触发告警
   - 熔断开启：立即告警
   - 重试风暴：重试次数突增>100%时告警



















---

*文档结束*


*文档结束*

