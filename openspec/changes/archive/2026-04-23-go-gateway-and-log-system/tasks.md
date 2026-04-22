## 1. 项目目录与基础设施

- [x] 1.1 创建顶层目录结构：`short-link-java/`、`short-link-gateway-go/`、`short-link-log-go/`
- [x] 1.2 编写 `docker-compose.yml`，编排 Redis、RocketMQ、ClickHouse、MySQL、Java 服务、Go 网关、Go 日志服务
- [x] 1.3 编写 ClickHouse 初始化 SQL（创建 `logs` 表，按天分区，设置 30 天 TTL）
- [x] 1.4 编写 MySQL 初始化 SQL（创建 `log_users` 表，用于日志前端用户认证）

## 2. Go 日志收集系统（short-link-log-go）

- [x] 2.1 初始化 Go 模块，引入 gin、clickhouse-go、gorm 依赖
- [x] 2.2 实现日志接收 API `POST /api/v1/logs/ingest`，支持单条和批量入库
- [x] 2.3 实现日志格式校验中间件（校验 level、timestamp、message 必填）
- [x] 2.4 实现 ClickHouse 批量写入逻辑（异步缓冲队列，失败重试）
- [x] 2.5 实现日志查询 API `GET /api/v1/logs/query`，支持时间范围、服务名、级别、关键词、TraceID 过滤
- [x] 2.6 实现分页查询参数（page、page_size，最多 100 条/页）
- [x] 2.7 实现用户认证 API（注册、登录、JWT 签发）
- [x] 2.8 实现登录频率限制（5 分钟 5 次，超限锁定 10 分钟）
- [x] 2.9 实现 `/health` 健康检查端点，返回 ClickHouse 连接状态

## 3. 日志前端（short-link-log-go/frontend）

- [x] 3.1 初始化 Vue 3 + Vite + Element Plus 前端项目
- [x] 3.2 实现登录页面（用户名/密码表单，JWT 存储到 localStorage）
- [x] 3.3 实现注册页面（用户名/密码确认表单，调用注册 API）
- [x] 3.4 实现路由守卫：未登录重定向到登录页
- [x] 3.5 实现日志查询主页面（搜索框、时间范围选择器、服务/级别过滤器）
- [x] 3.6 实现日志列表表格组件（分页、按时间倒序、关键词高亮）
- [x] 3.7 实现 TraceID 链路追踪视图（输入 TraceID，展示关联日志时间线）
- [x] 3.8 实现 Token 过期自动跳转登录页

## 4. Go 网关（short-link-gateway-go）— 核心框架

- [x] 4.1 初始化 Go 模块，引入 gin、go-redis 依赖
- [x] 4.2 定义路由 YAML 配置文件格式（路径前缀、目标地址、限流配置）
- [x] 4.3 实现 YAML 路由配置加载与解析
- [x] 4.4 实现基于路径前缀的反向代理转发逻辑
- [x] 4.5 实现 `X-Trace-ID` 生成与注入中间件（UUID v4）
- [x] 4.6 实现访问日志中间件（输出 JSON 格式：method、path、status_code、latency_ms、trace_id）
- [x] 4.7 实现 `/health` 健康检查端点

## 5. Go 网关 — 限流与跨域

- [x] 5.1 实现内存令牌桶算法（O(1) 时间复杂度）
- [x] 5.2 实现 Redis 全局限流兜底逻辑
- [x] 5.3 实现限流中间件（429 响应 + 限流日志）
- [x] 5.4 实现 CORS 中间件（白名单 Origin、预检请求处理）

## 6. Go 网关 — 性能监控

- [x] 6.1 实现内存指标收集器（QPS 计数器、P50/P90/P99 延迟直方图、错误计数器）
- [x] 6.2 实现限流状态指标（令牌桶余量、拒绝次数）
- [x] 6.3 实现 `GET /api/v1/metrics` 端点，返回 JSON 指标
- [x] 6.4 实现指标端点 IP 白名单（仅允许回环地址访问）

## 7. Java 短链接服务适配（short-link-java）

- [x] 7.1 创建 Spring Boot Starter 项目，包含 TraceID 拦截器和 JSON 日志编码器
- [x] 7.2 实现 Spring MVC Interceptor 读取 `X-Trace-ID` 并注入 MDC
- [x] 7.3 实现无 TraceID 时自动生成并注入
- [x] 7.4 实现 Logback JSON 编码器（输出统一格式：level、timestamp、trace_id、service、message、thread）
- [x] 7.5 在短链接核心服务中引入 Starter 依赖
- [x] 7.6 配置短链接服务的日志推送端点，指向 Go 日志收集 API

## 8. Console 前端集成（console-vue）

- [x] 8.1 新增网关监控大盘页面（QPS、P50/P90/P99、错误率、连接数卡片）
- [x] 8.2 实现 QPS 趋势折线图（最近 1 小时）
- [x] 8.3 实现延迟分布折线图（P50、P90、P99 三条线）
- [x] 8.4 实现每 5 秒自动刷新机制
- [x] 8.5 新增「日志中心」导航入口，跳转至日志系统前端

## 9. 集成测试与迁移

- [x] 9.1 编写 Go 网关集成测试（路由转发、限流、CORS、TraceID 注入）
- [x] 9.2 编写日志收集系统集成测试（入库、查询、过滤、分页）
- [x] 9.3 编写全链路 TraceID 透传测试（网关 → Java 服务 → 日志查询）
- [x] 9.4 Go 网关与 Spring Cloud Gateway 并行部署，小流量验证
- [x] 9.5 逐步切流至 Go 网关 100%，下线 Spring Cloud Gateway 容器
- [x] 9.6 更新部署文档和运维手册
