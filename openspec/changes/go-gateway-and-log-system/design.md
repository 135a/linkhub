## Context

当前项目使用 Spring Cloud Gateway 作为入口网关，部署在 `shortlink-main` 中，下游连接短链接解析服务（Java/Spring Boot）、Redis 缓存层、RocketMQ 消息队列。日志体系分散在各服务中，通过本地日志文件或 SLF4J 输出，无统一采集和查询入口。现有前端 `console-vue` 提供短链接管理功能。

本变更需要跨多个模块：新建 Go 网关、新建 Go 日志收集系统 + 独立前端、适配 Java 短链接服务接入 traceID 和统一日志格式、更新 Docker Compose 部署。

**约束条件：**
- 网关对外 API 必须与 Spring Cloud Gateway 保持兼容，确保现有短链接服务无感知切换
- 日志系统前端需独立部署，含简单登录注册（不做复杂 RBAC/SSO）
- 整体架构通过 Docker Compose 编排，保持当前部署习惯

## Goals / Non-Goals

**Goals:**
- Go 网关实现路由转发、限流、鉴权、跨域，QPS 达到 Spring Cloud Gateway 的 5x 以上，P99 延迟 < 5ms
- 日志收集系统统一采集网关和后端服务日志，提供 API 查询接口
- 日志系统独立前端，支持登录注册、日志检索、按条件过滤、时间范围查询
- traceID 从网关注入，透传至所有下游服务，实现全链路追踪
- Console 前端增加网关性能监控大盘（QPS、延迟、错误率）
- Docker Compose 一键编排全部服务

**Non-Goals:**
- 不做复杂用户权限系统（RBAC/OAuth/SSO），日志前端仅需基础登录注册
- 不替代 Prometheus/Grafana 等监控生态，网关监控通过自定义 API 暴露，可后续接入
- 不处理历史日志迁移，仅面向新产生的日志
- 不做日志系统的告警通知（邮件/钉钉/企微）

## Decisions

### 1. Go 网关框架：选用 `gin` + `net/http`，不选 `fasthttp`

**决定：** 使用标准库 `net/http` 配合 `gin` 框架，而非 `fasthttp`。

**理由：** `fasthttp` 虽然性能更高，但 API 与标准库不兼容，中间件生态薄弱，迁移成本高。短链接场景的核心瓶颈在 Redis 查询而非 HTTP 解析，`gin` 已足够。后续若确有性能瓶颈再评估 `fasthttp`。

**备选方案：**
- `fasthttp`: 拒绝，生态不成熟，中间件需要重写
- `echo`: 与 `gin` 同级别，但团队对 `gin` 更熟悉

### 2. 网关路由规则：YAML 配置文件 + 启动时加载

**决定：** 路由规则通过 YAML 配置文件定义，启动时加载到内存，运行时不支持热更新。

**理由：** 短链接项目的路由规则相对稳定，不需要动态路由。YAML 可读性强，配置变更走 Git → 重启流程即可。

**备选方案：**
- Redis/Nacos 动态路由：拒绝，引入额外依赖，增加复杂度

### 3. 日志收集存储：ClickHouse 作为日志存储引擎

**决定：** 日志通过 HTTP 协议从网关和 Java 服务推送到 Go 日志收集服务，写入 ClickHouse。

**理由：** 日志查询是典型的 OLAP 场景（按时间范围、关键词、字段过滤），ClickHouse 的列式存储 + 分区能力远超 Elasticsearch 在同等硬件下的查询性能，且运维更轻量（单机即可，无需 ZooKeeper）。

**备选方案：**
- Elasticsearch: 拒绝，资源占用大，运维复杂
- PostgreSQL/MySQL: 拒绝，不适合日志的高吞吐写入和全文检索

### 4. 日志前端技术栈：Vue 3 + Element Plus + Vite

**决定：** 与现有 `console-vue` 保持一致的技术栈，新建独立项目 `log-frontend/`。

**理由：** 团队已有 Vue 3 经验，Element Plus 提供现成的表格、搜索、表单组件，减少开发量。

**登录注册方案：** JWT + 本地存储（MySQL），不引入第三方认证服务。密码 bcrypt 加密。

### 5. TraceID 方案：网关注入，HTTP Header 透传

**决定：** 网关在入口生成 `X-Trace-ID`（UUID），通过 HTTP Header 向后传递。Java 端通过 Spring MVC Interceptor 读取并注入 MDC。日志通过统一 JSON 格式（含 `trace_id` 字段）推送到 Go 日志收集服务。

**格式：** `{"level": "INFO", "timestamp": "...", "trace_id": "...", "service": "shortlink", "message": "...", "fields": {...}}`

### 6. 限流实现：令牌桶算法（内存）+ Redis 兜底

**决定：** 单机限流使用内存中的令牌桶算法，分布式场景下通过 Redis 做全局计数器兜底。

**理由：** 短链接服务的限流主要在网关入口层，内存令牌桶性能最优（O(1)），Redis 用于多实例部署时防止单实例限流失效。

### 7. 网关限流与日志系统的关系

网关的访问日志（含限流拒绝记录）统一推送到日志收集系统，日志系统不做限流判断。两者职责分离。

## Risks / Trade-offs

| 风险/权衡 | 影响 | 缓解措施 |
|---|---|---|
| ClickHouse 运维经验不足 | 部署和调优成本高 | 初期单节点部署，后续文档沉淀运维手册 |
| Go 网关替代 Spring Cloud Gateway 的兼容性 | 遗漏某些过滤器逻辑 | 对照现有 SCG 过滤器逐一映射，编写集成测试 |
| 日志收集服务成为新的性能瓶颈 | 高并发下日志丢失 | 日志推送采用异步批量 + 本地缓冲队列，失败重试 |
| 日志前端登录安全性 | 简单认证可能被暴力破解 | 加登录频率限制 + IP 白名单（可在网关层配置） |
| Java 服务适配工作量 | 需要逐个服务添加拦截器和日志格式化 | 封装为 Spring Boot Starter，Maven 依赖一行引入 |

## Migration Plan

### 部署步骤

1. **Phase 1 — 基础设施先行**
   - 部署 ClickHouse、MySQL（日志前端用户存储）
   - 启动 Go 日志收集服务
   - Java 服务接入 Spring Boot Starter，统一日志格式

2. **Phase 2 — 日志系统上线**
   - 部署日志前端，登录注册可用
   - 验证日志采集 → 存储 → 查询全链路

3. **Phase 3 — Go 网关上线**
   - Go 网关与 Spring Cloud Gateway 并行部署，不同端口
   - 切少量流量到 Go 网关，对比延迟/错误率
   - 逐步提高 Go 网关流量占比至 100%
   - 下线 Spring Cloud Gateway 容器

4. **Phase 4 — 监控大盘**
   - Console 前端接入网关性能监控 API
   - 完善日志前端查询体验

### 回滚策略

- Go 网关保留 Spring Cloud Gateway 容器配置，切换回只需修改 Docker Compose 端口映射
- 日志系统不参与请求转发，回滚不影响核心链路
- Java 服务的 Spring Boot Starter 移除即恢复原始日志格式

## Open Questions

1. **ClickHouse 是否需要集群？** 当前短链接规模预计单节点够用，需要根据实际 QPS 和数据保留周期决定
2. **日志保留周期？** 需要明确热数据（ClickHouse）保留多久，是否需要冷热分层
3. **Java 服务是否全部改造？** 建议优先改造网关和短链接解析服务，其他服务可后续接入
4. **日志前端是否需要仪表盘？** 当前仅做日志查询，是否需要加入简单的统计图表（如日志量趋势）
