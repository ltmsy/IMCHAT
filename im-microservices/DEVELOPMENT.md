# IM系统开发指南

## 🚀 快速开始

### 环境准备
1. **JDK 17**: 确保已安装JDK 17或更高版本
2. **Maven 3.8+**: 确保已安装Maven 3.8或更高版本
3. **Docker**: 确保Docker和Docker Compose已安装并运行

### 设置JAVA_HOME
```bash
# macOS/Linux
export JAVA_HOME=/path/to/your/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# Windows
set JAVA_HOME=C:\path\to\your\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
```

### 项目编译
```bash
# 编译整个项目
mvn clean compile

# 编译特定模块
mvn clean compile -pl common
mvn clean compile -pl business-service
mvn clean compile -pl communication-service
mvn clean compile -pl admin-service
```

## 🏗️ 项目结构

```
im-microservices/
├── pom.xml                                    # 父POM
├── common/                                    # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/acme/im/common/
│       ├── infrastructure/                    # 基础设施
│       │   ├── database/                      # 数据库访问
│       │   ├── redis/                         # Redis客户端
│       │   ├── minio/                         # MinIO客户端
│       │   └── nats/                          # NATS客户端
│       ├── utils/                             # 工具组件
│       │   ├── cache/                         # 缓存管理
│       │   ├── queue/                         # 消息队列
│       │   ├── logging/                       # 日志组件
│       │   └── monitoring/                    # 监控组件
│       └── security/                          # 安全组件
│           ├── jwt/                           # JWT认证
│           ├── permission/                    # 权限控制
│           ├── encryption/                    # 加密解密
│           └── rateLimit/                     # 限流组件
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
├── scripts/                                   # 部署脚本
└── README.md                                  # 项目说明
```

## 🐳 基础设施服务

### 启动基础设施服务
```bash
# 使用脚本启动（推荐）
./scripts/start-infrastructure.sh

# 手动启动
cd docker
docker-compose up -d
```

### 停止基础设施服务
```bash
# 使用脚本停止（推荐）
./scripts/stop-infrastructure.sh

# 手动停止
cd docker
docker-compose down
```

### 服务访问地址
- **MySQL主库**: localhost:3306 (im_system)
- **MySQL从库**: localhost:3307 (im_system_copy)
- **Redis**: localhost:6379
- **NATS**: localhost:4222
- **NATS管理**: localhost:8222
- **MinIO**: localhost:9000
- **MinIO控制台**: localhost:9001
- **Redis管理**: localhost:8081
- **MySQL管理**: localhost:8082

### 数据库连接信息
- **用户名**: root
- **密码**: 123456
- **主库**: im_system
- **从库**: im_system_copy

## 🔧 开发流程

### 1. 功能开发流程
```bash
# 1. 创建功能分支
git checkout -b feature/新功能名称

# 2. 开发功能
# 在相应模块中添加代码

# 3. 编译测试
mvn clean compile
mvn test

# 4. 提交代码
git add .
git commit -m "feat: 添加新功能"

# 5. 推送分支
git push origin feature/新功能名称

# 6. 创建Pull Request
# 在GitHub上创建PR，等待代码审查
```

### 2. 代码规范
- 遵循阿里巴巴Java开发手册
- 使用统一的代码格式化配置
- 编写完整的单元测试
- 添加详细的代码注释

### 3. 提交规范
```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建过程或辅助工具的变动
```

## 🧪 测试

### 单元测试
```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl common
mvn test -pl business-service

# 运行特定测试类
mvn test -Dtest=UserServiceTest

# 运行特定测试方法
mvn test -Dtest=UserServiceTest#testCreateUser
```

### 集成测试
```bash
# 使用TestContainers进行集成测试
mvn test -Dtest=*IntegrationTest
```

### 测试覆盖率
```bash
# 生成测试覆盖率报告
mvn jacoco:report

# 查看覆盖率报告
open target/site/jacoco/index.html
```

## 📊 监控与调试

### 应用监控
- Spring Boot Actuator: http://localhost:8080/actuator
- 健康检查: http://localhost:8080/actuator/health
- 指标信息: http://localhost:8080/actuator/metrics

### 日志配置
- 日志级别: 在application.yml中配置
- 日志文件: 默认输出到控制台
- 结构化日志: 使用JSON格式输出

### 调试技巧
1. **远程调试**: 使用IDE连接远程JVM
2. **日志调试**: 调整日志级别查看详细信息
3. **数据库调试**: 使用phpMyAdmin查看数据
4. **Redis调试**: 使用Redis Commander查看缓存

## 🚀 部署

### 开发环境部署
```bash
# 1. 启动基础设施服务
./scripts/start-infrastructure.sh

# 2. 启动应用服务
cd business-service && mvn spring-boot:run &
cd communication-service && mvn spring-boot:run &
cd admin-service && mvn spring-boot:run &
```

### 生产环境部署
```bash
# 1. 打包应用
mvn clean package -DskipTests

# 2. 构建Docker镜像
docker build -t im-business-service:latest business-service/
docker build -t im-communication-service:latest communication-service/
docker build -t im-admin-service:latest admin-service/

# 3. 启动应用
docker run -d -p 8080:8080 im-business-service:latest
docker run -d -p 8081:8080 im-communication-service:latest
docker run -d -p 8082:8080 im-admin-service:latest
```

## 🔍 故障排查

### 常见问题

#### 1. 编译失败
```bash
# 检查Java版本
java -version

# 检查Maven版本
mvn -version

# 清理并重新编译
mvn clean compile
```

#### 2. 服务启动失败
```bash
# 检查端口占用
lsof -i :8080

# 查看服务日志
docker-compose logs [服务名]

# 检查依赖服务状态
docker-compose ps
```

#### 3. 数据库连接失败
```bash
# 检查MySQL服务状态
docker exec im-mysql-master mysql -uroot -p123456 -e "SELECT 1"

# 检查网络连接
docker network ls
docker network inspect im-microservices_im-network
```

#### 4. Redis连接失败
```bash
# 检查Redis服务状态
docker exec im-redis redis-cli ping

# 检查Redis配置
docker exec im-redis redis-cli config get *
```

### 性能调优

#### 1. JVM调优
```bash
# 设置JVM参数
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC"

# 启动应用
mvn spring-boot:run -Dspring-boot.run.jvmArguments="$JAVA_OPTS"
```

#### 2. 数据库调优
- 调整MySQL配置参数
- 优化SQL查询
- 添加适当的索引

#### 3. 缓存调优
- 调整Redis内存配置
- 优化缓存策略
- 监控缓存命中率

## 📚 学习资源

### 官方文档
- [Spring Boot 3.x 官方文档](https://spring.io/projects/spring-boot)
- [Spring WebFlux 官方文档](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [NATS 官方文档](https://docs.nats.io/)

### 技术博客
- Spring官方博客
- 美团技术团队博客
- 阿里技术团队博客

### 视频教程
- Spring Boot实战教程
- 微服务架构设计
- 高并发系统设计

## 🤝 贡献指南

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

### 功能建议
- 在Issues中提出功能建议
- 描述功能需求和实现思路
- 讨论技术方案和实现细节

---

**注意**: 本开发指南会随着项目发展持续更新，请关注最新版本。 