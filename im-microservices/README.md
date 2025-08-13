# IM 微服务系统

## 项目简介

IM（即时通讯）微服务系统是一个基于Spring Boot 3和微服务架构的现代化即时通讯平台。系统采用三服务架构设计，通过NATS事件总线实现服务间通信，支持高并发、高可用的消息传输。

## 系统架构

### 服务架构
- **前端业务服务 (Business Service)**: 处理用户管理、社交管理、内容管理等业务逻辑
- **通信服务 (Communication Service)**: 处理实时通信、消息路由、推送等
- **后台管理服务 (Admin Service)**: 处理系统监控、运维管理、配置管理等
- **公共模块 (Common)**: 提供基础设施、工具、安全等公共组件

### 技术栈
- **基础环境**: JDK 17 LTS
- **框架**: Spring Boot 3.2.8 + Spring WebFlux 6.0
- **数据库**: MySQL 8.0 (主从架构)
- **缓存**: Redis 7.0
- **消息总线**: NATS 2.9 (JetStream)
- **对象存储**: MinIO 2023
- **ORM**: MyBatis-Plus 3.5.5
- **安全**: Spring Security 6.0 + JWT
- **监控**: Micrometer + Prometheus

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+
- Docker 24.0+
- Docker Compose 2.20+

### 1. 启动基础设施服务
```bash
cd docker
docker-compose up -d
```

启动的服务包括：
- MySQL主从数据库 (3306, 3307)
- Redis缓存服务 (6379)
- NATS消息总线 (4222, 8222)
- MinIO对象存储 (9000, 9001)
- Redis管理界面 (8081)
- MySQL管理界面 (8082)

### 2. 初始化数据库
数据库初始化脚本已放置在 `docker/sql/` 目录下，MySQL容器启动时会自动执行。

### 3. 编译项目
```bash
mvn clean compile
```

### 4. 启动应用服务
```bash
# 启动后台管理服务
cd admin-service
mvn spring-boot:run

# 启动前端业务服务
cd business-service
mvn spring-boot:run

# 启动通信服务
cd communication-service
mvn spring-boot:run
```

## 项目结构

```
im-microservices/
├── pom.xml                                    # 父POM
├── common/                                    # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/acme/im/common/
│       ├── infrastructure/                    # 基础设施
│       ├── utils/                             # 工具组件
│       └── security/                          # 安全组件
├── business-service/                           # 前端业务服务
│   ├── pom.xml
│   └── src/main/java/com/acme/im/business/
│       ├── user/                              # 用户管理
│       ├── social/                            # 社交管理
│       ├── content/                           # 内容管理
│       └── search/                            # 搜索服务
├── communication-service/                      # 通信服务
│   ├── pom.xml
│   └── src/main/java/com/acme/im/communication/
│       ├── message/                           # 消息处理
│       ├── realtime/                          # 实时通信
│       ├── routing/                           # 消息路由
│       └── push/                              # 推送服务
├── admin-service/                              # 后台管理服务
│   ├── pom.xml
│   └── src/main/java/com/acme/im/admin/
│       ├── monitoring/                        # 系统监控
│       ├── operations/                        # 运维管理
│       ├── configuration/                     # 配置管理
│       └── security/                          # 安全审计
├── docker/                                    # Docker配置
│   ├── docker-compose.yml                     # 服务编排
│   ├── mysql-master.cnf                      # MySQL主库配置
│   ├── mysql-slave.cnf                       # MySQL从库配置
│   └── sql/                                  # 数据库脚本
├── docs/                                      # 项目文档
└── scripts/                                   # 部署脚本
```

## 开发计划

### 第一阶段：项目基础搭建 (Week 1-2)
- [x] 创建多模块Maven项目结构
- [x] 配置父POM依赖管理
- [x] 创建四个核心模块
- [x] 配置Docker Compose环境

### 第二阶段：公共模块开发 (Week 3-4)
- [ ] 基础设施组件
- [ ] 工具组件
- [ ] 安全组件
- [ ] 通用组件

### 第三阶段：核心服务开发 (Week 5-10)
- [ ] 前端业务服务
- [ ] 通信服务
- [ ] 后台管理服务

### 第四阶段：集成测试与优化 (Week 11-14)
- [ ] 服务集成测试
- [ ] 性能优化
- [ ] 系统调优

### 第五阶段：生产部署 (Week 15-16)
- [ ] 生产环境部署
- [ ] 监控告警配置
- [ ] 系统上线

## 配置说明

### 数据库配置
- **主库**: localhost:3306 (im_system)
- **从库**: localhost:3307 (im_system_copy)
- **用户名**: root
- **密码**: 123456

### Redis配置
- **主机**: localhost
- **端口**: 6379
- **密码**: 无

### NATS配置
- **服务器**: nats://localhost:4222
- **管理端口**: 8222
- **JetStream**: 已启用

### MinIO配置
- **端点**: http://localhost:9000
- **控制台**: http://localhost:9001
- **访问密钥**: minioadmin
- **秘密密钥**: minioadmin

## 管理界面

- **Redis管理**: http://localhost:8081
- **MySQL管理**: http://localhost:8082
- **MinIO控制台**: http://localhost:9001

## 开发规范

### 代码规范
- 遵循阿里巴巴Java开发手册
- 使用统一的代码格式化配置
- 编写完整的单元测试
- 添加详细的代码注释

### 提交规范
- 使用语义化的提交信息
- 每个功能点独立提交
- 提交前进行代码审查

### 分支管理
- `main`: 主分支，用于生产环境
- `develop`: 开发分支，用于集成测试
- `feature/*`: 功能分支，用于新功能开发
- `hotfix/*`: 热修复分支，用于紧急修复

## 监控与运维

### 应用监控
- Spring Boot Actuator健康检查
- Micrometer指标收集
- Prometheus指标存储
- Grafana可视化展示

### 日志管理
- 结构化日志输出
- 日志级别动态调整
- 日志文件轮转管理

### 告警机制
- 系统资源告警
- 业务指标告警
- 异常情况告警

## 性能指标

### 目标指标
- **用户规模**: 总用户50,000，日活10,000
- **群组规模**: 群组上限5,000，活跃同时2,000
- **响应时间**: 端到端P95 < 200ms，系统响应P95 < 100ms
- **可用性**: 系统可用性 > 99.9%

### 性能优化
- 数据库查询优化
- 缓存策略优化
- 消息推送优化
- 负载均衡优化

## 安全特性

### 认证授权
- JWT无状态认证
- 基于角色的权限控制
- 多端设备管理
- 异常登录检测

### 数据安全
- 敏感数据加密
- 传输数据加密
- 访问权限控制
- 操作审计日志

### 系统安全
- 接口限流保护
- SQL注入防护
- XSS攻击防护
- CSRF攻击防护

## 故障处理

### 常见问题
1. **服务启动失败**: 检查端口占用和依赖服务状态
2. **数据库连接失败**: 检查MySQL服务状态和网络配置
3. **Redis连接失败**: 检查Redis服务状态和网络配置
4. **NATS连接失败**: 检查NATS服务状态和网络配置

### 故障恢复
- 服务自动重启
- 数据库连接重试
- 消息重发机制
- 降级服务策略

## 贡献指南

### 参与贡献
1. Fork项目到个人仓库
2. 创建功能分支
3. 提交代码变更
4. 创建Pull Request
5. 等待代码审查

### 问题反馈
- 使用GitHub Issues报告问题
- 提供详细的错误信息和复现步骤
- 标注问题类型和优先级

## 许可证

本项目采用 [MIT License](LICENSE) 许可证。

## 联系方式

- **项目维护者**: IM开发团队
- **邮箱**: dev@acme.com
- **项目地址**: https://github.com/acme/im-microservices

## 更新日志

### v1.0.0-SNAPSHOT (2024-01-XX)
- 项目基础架构搭建
- 多模块Maven项目创建
- Docker环境配置
- 基础设施服务部署

---

**注意**: 本项目仍在开发中，部分功能可能尚未完成。如有问题，请查看Issues或联系开发团队。 