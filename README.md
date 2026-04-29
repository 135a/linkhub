# Linkhub-SaaS 高性能全栈短链接管理平台




---

## 📖 项目简介

**Linkhub-SaaS** 是一款企业级、高性能的短链接管理平台。它不仅提供基础的 URL 缩短功能，更通过**多级缓存**、**异步统计**和**分库分表**等先进技术，支撑千万级高并发重定向与海量访问数据的深度挖掘。

### ✨ 核心亮点
- **极速跳转**：利用 Redis 高级数据结构实现毫秒级响应，内置布隆过滤器防御恶意攻击。
- **海量支撑**：基于 Apache ShardingSphere 实现透明化分库分表，轻松应对亿级数据。
- **深度洞察**：全方位的访客数据分析，包括 PV/UV、地理位置、设备类型及操作系统。
- **高可用架构**：基于 RocketMQ 实现统计业务完全异步化，主流程零延迟。

---

## 🛠️ 技术栈

### 后端 (Backend)
| 技术 | 说明 |
| :--- | :--- |
| **Spring Boot 3.0.7** | 核心开发框架 |
| **MyBatis-Plus** | ORM 增强工具 |
| **ShardingSphere-JDBC** | 数据库水平扩展与分库分表 |
| **Redis & Redisson** | 多级缓存与分布式锁 |
| **RocketMQ** | 异步消息队列，解耦统计业务 |
| **Sentinel** | 流量控制与熔断降级 |
| **Actuator & Micrometer** | 实时运行指标监控与 Prometheus 暴露 |
| **JMeter** | 完整的性能压测套件 (位于 test/jmeter) |
| **Hutool** | 全能 Java 工具类库 |

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
在 `test/jmeter/` 目录下提供了一站式压测资源：
- **create-qps-test.jmx**: 演示分布式锁 vs 非分布式锁在创建短链时的 QPS 对比。
- **redirect-cache-test.jmx**: 演示高并发重定向下的毫秒级响应与缓存预热效果。
- **[面试演示手册](test/jmeter/README.md)**: 包含详细的演示脚本、话术及预期数值基准。

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

# 3. 启动全栈环境
docker-compose up -d --build
```
> 启动后访问：[http://localhost](http://localhost) (账号: `admin` / `admin123`)

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
