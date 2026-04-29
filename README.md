# Linkhub-SaaS 高性能全栈短链接管理平台




---

## 📖 项目简介

**Linkhub-SaaS** 是一款企业级、高性能的短链接管理平台。它不仅提供基础的 URL 缩短功能，更通过**多级缓存**、**异步统计**和**分库分表**等先进技术，支撑千万级高并发重定向与海量访问数据的深度挖掘。

### ✨ 核心亮点
- **极速跳转**：首创 Caffeine (L1) + Redis (L2) 多级缓存架构，热点短链重定向 P99 < 2ms，内置 Redisson 布隆过滤器彻底防御缓存穿透恶意攻击。
- **海量支撑**：基于 Apache ShardingSphere 实现透明化分库分表，轻松应对亿级短链映射数据存储。
- **深度洞察**：引入 ClickHouse 列式数据库专属存储海量访问明细数据，千万级数据量下 PV/UV 等多维报表毫秒级聚合查询。
- **高并发与高可用**：基于 RocketMQ 将重定向主链路与统计写入流程彻底异步解耦；结合 Sentinel 针对不同场景定制限流策略（读操作快速失败 / 写操作匀速排队），系统稳如磐石。

---

## 🛠️ 技术栈

### 后端 (Backend)
| 技术 | 说明 |
| :--- | :--- |
| **Spring Boot 3.0.7** | 核心开发框架 |
| **MyBatis-Plus** | ORM 增强工具 |
| **ShardingSphere-JDBC** | 数据库水平扩展与分库分表 |
| **Caffeine & Redis** | L1本地缓存 + L2分布式多级缓存架构 |
| **RocketMQ** | 异步消息队列，解耦统计业务实现流量削峰 |
| **ClickHouse** | OLAP 分析型数据库，海量统计数据存储与秒级聚合 |
| **Sentinel** | 接口级流量控制与熔断降级保护 |
| **Actuator & Micrometer** | 实时运行指标监控与 Prometheus 暴露 |
| **JMeter** | 完整的性能压测套件 (位于 test/jmeter) |
| **Nginx & Docker** | 容器化集群部署与多域名路由统一网关 |

### 前端 (Frontend)
| 技术 | 说明 |
| :--- | :--- |
| **Vue 3.3 (Composition API)** | 现代化前端框架 |
| **Vite 4** | 极速构建工具 |
| **Vuex 4** | 状态管理 |
| **Element Plus** | 企业级 UI 组件库 |
| **ECharts 4.8** | 核心数据可视化引擎 |
| **Vanta.js** | 炫酷的 3D 交互背景 |

---

## 📈 性能压测与面试演示

本项目专为面试场景深度优化，内置了完善的性能压测套件与实时指标监控，方便在面试现场展示系统的硬核实力。

### 1. 实时性能看板
系统提供了聚合指标接口 `GET /api/short-link/v1/metrics/summary`，支持实时查看：
- **缓存命中率**: 实时监控多级缓存（Redis/JVM）的效率。
- **布隆过滤器**: 展示对非法请求的拦截次数，演示**防缓存穿透**机制。
- **Sentinel 限流**: 实时展示触发流控的请求次数，验证**高可用保护**。

### 2. JMeter 压测套件
在 `test/jmeter/` 目录下提供了一站式压测资源，涵盖 6 大核心场景：
- **redirect-cache-test.jmx**: 演示高并发重定向毫秒级响应、布隆过滤器拦截与缓存命中率预热。
- **stats-concurrency-test.jmx**: 演示高并发重定向与后台报表查询的「读写分离与异步解耦」性能。
- **batch-create-test.jmx / bulk-delete-test.jmx**: 演示批量创建吞吐量与高并发批量删除全链路优化。
- **create-qps-test.jmx**: 演示单条创建时分布式锁防碰撞机制与 Sentinel 动态限流保护。
- **page-query-test.jmx**: 演示海量数据下复杂聚合分页查询的性能调优对比。
- **[面试演示实战手册](test/jmeter/README.md)**: 包含详尽的压测步骤、面试引导话术及预期数值基准。

---

## 🚀 快速启动

### 1. 环境要求
- **JDK 17+**
- **Node.js 16+**
- **Docker & Docker Compose** (推荐)
- **MySQL 8.0 / Redis 7.0 / RocketMQ 4.9**

### 2. Docker 一键启动 (推荐)
```bash
# 1. 克隆并进入目录
cd shortlink-main

# 2. 配置环境变量 (可选，默认已提供 .env.example)
cp .env.example .env

# 3. 启动全栈环境 (Nginx 网关、应用、MySQL、Redis、RocketMQ、ClickHouse 等)
docker-compose up -d --build
```
> 💡 **访问说明**：系统已接入多域名 Nginx 网关。请在本地修改 Host 文件（添加 `127.0.0.1 shortlink.nym.asia`），然后通过浏览器访问：[http://shortlink.nym.asia](http://shortlink.nym.asia) (默认账号: `admin` / `admin123`)

### 3. 本地开发模式
#### 后端启动：
1. 导入 `mysql/database/link.sql` 到数据库。
2. 修改 `shortlink-main/main/src/main/resources/application-dev.yaml` 中的中间件地址。
3. 运行 `ShortLinkApplication` 主类。

#### 前端启动：
```bash
cd shortlink-main/console-vue
npm install
npm run dev
```

---

## 📂 项目结构

```text
shortlink-main
├── main                # Spring Boot 后端核心模块
│   ├── src/main/java   # 业务逻辑代码
│   └── Dockerfile      # 后端容器化构建
├── console-vue         # Vue 3 前端控制台
│   ├── src/api         # 接口定义
│   └── Dockerfile      # 前端容器化构建
├── mysql               # 数据库初始化脚本
└── docker-compose.yml  # 多容器编排文件
```

---

## 📊 预览

> [!TIP]
> 进入管理后台后，您可以创建分组、生成短链接并实时查看访问统计图表。

![Dashboard Preview](https://img.shields.io/badge/Preview-Dashboard-blueviolet)

---

## 📄 许可证
本项目遵循 [MIT License](LICENSE) 开源协议。
