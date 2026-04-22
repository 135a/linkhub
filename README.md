# LinkHub – 高性能 SaaS 短链接平台

## 🚀 项目简介

LinkHub 是一个高可用、高性能的 SaaS 级短链接平台，支持高并发访问、链路追踪、日志查询和网关监控。

### 核心特性

- ✨ **三层架构设计**：Go 网关 + Go 日志收集 + Java 业务服务
- ⚡ **高性能网关**：基于 Gin 框架，支持路由转发、限流、CORS、链路追踪
- 📊 **完整监控体系**：QPS 指标、P50/P90/P99 延迟、错误率统计
- 🔍 **链路追踪**：TraceID 全链路透传，方便问题定位
- 📝 **日志中心**：ClickHouse 海量存储，支持按服务、级别、TraceID 过滤
- 🌐 **双控制台**：短链接管理控制台 + 网关监控大盘 + 日志查询系统

---

## 🛠️ 技术栈

### 基础设施

| 组件 | 版本/技术 | 用途 |
|------|-----------|------|
| MySQL | 8.0+ | 业务数据存储 |
| Redis | latest | 缓存、限流分布式协调 |
| Nacos | v2.2.3 | 服务注册与配置中心 |
| RocketMQ | 4.9.4 | 异步消息队列 |
| ClickHouse | latest | 日志存储与分析 |

### 后端服务

| 服务 | 技术栈 | 功能 |
|------|--------|------|
| Go Gateway | Go + Gin | 网关路由、限流、监控 |
| Log Collector | Go + ClickHouse | 日志收集、查询 |
| Project Service | Java + Spring Boot 3 | 短链接核心服务 |
| Admin Service | Java + Spring Boot 3 | 短链接管理后台 |

### 前端

| 前端 | 技术栈 | 功能 |
|------|--------|------|
| Console Vue | Vue 3 + Vite + Element Plus | 短链接管理控制台 |
| Log Frontend | Vue 3 + Vite | 日志查询与监控系统 |

---

## 📦 快速启动

### 环境要求

- Docker
- Docker Compose v2+

### 两种部署模式

#### 1️⃣ Dev 模式（开发环境）

适用于本地开发，**性能优先，不限制内存**

```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

#### 2️⃣ Prod 模式（生产环境）

适用于 4G 服务器，**内存约束优化**

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### 服务访问地址

部署成功后，可以通过以下地址访问：

| 服务 | 访问地址 | 说明 |
|------|---------|------|
| 短链接控制台 | http://localhost | 短链接管理 |
| 网关监控 API | http://localhost:8000/api/v1/metrics | 性能指标查询 |
| 日志查询系统 | http://localhost:3001 | 日志查询与追踪 |
| Nacos 控制台 | http://localhost:8848/nacos | 服务注册中心 |
| Admin 服务 | http://localhost:8002 | 管理后台 API |
| Project 服务 | http://localhost:8001 | 短链接核心 API |

### 默认账号密码

- **Nacos**: `nacos` / `nacos`
- **日志系统**: 需要先注册账号

---

## 📁 项目结构

```
linkhub/
├── short-link-gateway-go/    # Go 网关
│   ├── config.yaml           # 路由配置
│   └── internal/
│       ├── config/
│       ├── middleware/
│       ├── proxy/
│       ├── handler/
│       ├── ratelimit/
│       └── metrics/
├── short-link-log-go/        # Go 日志收集
│   ├── main.go
│   ├── frontend/             # 日志查询前端
│   ├── init-mysql/
│   └── init-clickhouse/
├── short-link-java/          # Java 业务服务
│   ├── admin/
│   ├── project/
│   ├── console-vue/          # 控制台前端
│   └── shortlink-trace-starter/
├── openspec/                 # OpenSpec 变更文档
├── docker-compose.yml        # 基础配置
├── docker-compose.dev.yml    # Dev 模式配置
├── docker-compose.prod.yml   # Prod 模式配置
└── README.md
```

---

## 🔧 核心功能说明

### 1. Go 网关功能

- **路由转发**：基于路径前缀转发到对应的后端服务
- **限流保护**：令牌桶算法 + Redis 分布式限流
- **CORS 支持**：灵活的跨域配置
- **TraceID 透传**：自动生成/读取 X-Trace-ID 并注入到 MDC
- **访问日志**：记录所有请求的完整信息
- **性能监控**：QPS、P50/P90/P99、错误率统计

### 2. 日志收集系统

- **异步写入**：内存缓冲队列，批量写入 ClickHouse
- **精准查询**：按时间范围、服务名、级别、关键词、TraceID 筛选
- **用户认证**：JWT Token 认证，登录频率限制
- **链路追踪**：输入 TraceID，展示完整请求链路

### 3. Java 服务适配

- **Trace Starter**：开箱即用的 TraceID 拦截器和 JSON 日志编码器
- **MDC 注入**：自动读取/生成 TraceID 并注入 MDC
- **日志推送**：配置 Logstash 地址，自动批量推送

---

## 📈 生产部署建议（4GB 服务器）

### 内存分配策略

| 服务 | Dev (GB) | Prod (GB) |
|------|---------|----------|
| MySQL | 1.0 | 0.8 |
| Redis | 0.5 | 0.4 |
| Nacos | 1.0 | 0.6 |
| RocketMQ (Namesrv + Broker) | 1.8 | 1.0 |
| ClickHouse | 2.0 | 0.6 |
| Go Gateway | 0.3 | 0.2 |
| Log Collector | 0.3 | 0.2 |
| Project Service | 1.0 | 0.5 |
| Admin Service | 0.8 | 0.4 |
| Frontends | N/A | N/A (Nginx 静态) |

### 生产模式优化

- **Nacos**: Xms/Xmx 降为 384m
- **RocketMQ**: Xms/Xmx 降为 512m
- **ClickHouse**: 内存限制为 0.6GB
- **Java 服务**: Xms/Xmx 降低为 512m/384m

---

## 🧪 验证部署

### 健康检查

```bash
# 检查网关健康状态
curl http://localhost:8000/health

# 检查日志服务健康状态
curl http://localhost:8081/health
```

### 查看日志

```bash
# 查看所有容器日志
docker-compose logs -f

# 只查看网关日志
docker-compose logs -f gateway

# 只查看日志收集服务
docker-compose logs -f log-collector
```

### 停止服务

```bash
# Dev 模式停止
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down

# Prod 模式停止
docker-compose -f docker-compose.yml -f docker-compose.prod.yml down
```

---

## 📚 开发指南

### 更新配置

**Gateway 路由配置**：`short-link-gateway-go/config.yaml`

**Java 服务配置**：`short-link-java/{admin,project}/src/main/resources/`

---

## 🔒 安全建议

1. 生产环境务必修改 Nacos、MySQL、Redis 密码
2. 限制日志系统注册频率，防止恶意注册
3. 生产环境中 `/api/v1/metrics` 只允许内网访问
4. 定期备份 MySQL 和 ClickHouse 数据

---

## 💡 贡献指南

欢迎 Fork 项目并提交 PR！

---

## 📄 许可证

本项目采用 Apache 2.0 许可证。
