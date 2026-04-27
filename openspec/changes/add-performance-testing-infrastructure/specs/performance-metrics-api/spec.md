## ADDED Requirements

### Requirement: 系统必须提供聚合性能指标查询接口
系统 MUST 通过 `GET /api/short-link/v1/metrics/summary` 接口，在单次 HTTP 请求中返回以下全部运行时指标：今日 Redis 缓存命中次数、缓存未命中次数、缓存命中率（百分比）、布隆过滤器拦截次数（自进程启动起）、Sentinel 限流触发次数（自进程启动起）、今日重定向请求总量。接口 MUST 在 200ms 内响应。

#### Scenario: 正常运行时获取指标摘要
- **WHEN** 客户端发送 `GET /api/short-link/v1/metrics/summary` 请求
- **THEN** 系统 MUST 返回 HTTP 200，Body 为 JSON 对象，包含 `cacheHitRate`、`cacheHitCount`、`cacheMissCount`、`bloomFilterInterceptCount`、`sentinelBlockCount`、`todayRedirectTotal` 字段，所有字段 SHALL 为数值或百分比字符串

#### Scenario: 服务刚启动时指标为零
- **WHEN** 服务启动后未发生任何重定向请求
- **THEN** `cacheHitCount`、`cacheMissCount`、`bloomFilterInterceptCount`、`sentinelBlockCount` SHALL 均返回 0，`cacheHitRate` SHALL 返回 `"0.00%"`

#### Scenario: 接口响应时间满足要求
- **WHEN** 系统在正常负载下收到指标查询请求
- **THEN** 接口 MUST 在 200ms 内返回响应，不得因指标查询阻塞主业务线程

### Requirement: 系统必须暴露 Actuator 标准指标端点
系统 MUST 通过 Spring Boot Actuator 暴露 `health`、`metrics`、`prometheus`、`info` 端点，且这些端点 MUST 在无额外认证的情况下可通过 HTTP 访问（用于本地和 Docker 演示环境）。`shutdown`、`env`、`beans` 等高风险端点 MUST NOT 暴露。

#### Scenario: Actuator 健康检查端点可访问
- **WHEN** 客户端发送 `GET /actuator/health` 请求
- **THEN** 系统 MUST 返回 HTTP 200 及 `{"status":"UP"}` 响应体，并包含各组件健康状态详情

#### Scenario: Prometheus 指标端点可抓取
- **WHEN** 客户端发送 `GET /actuator/prometheus` 请求
- **THEN** 系统 MUST 返回 HTTP 200 及 Prometheus 格式的指标文本，包含 JVM、HTTP 请求延迟等基础指标

#### Scenario: 高风险端点不可访问
- **WHEN** 客户端发送 `GET /actuator/env` 或 `GET /actuator/shutdown` 请求
- **THEN** 系统 MUST 返回 HTTP 404

### Requirement: 系统必须对布隆过滤器拦截事件计数
系统 MUST 在重定向流程中，当请求的 shortUri 经布隆过滤器判断为不存在且查库确认后也不存在时，将该拦截事件计入进程内原子计数器，并通过聚合指标接口暴露该计数值。计数器 MUST 为线程安全实现。

#### Scenario: 无效短链接请求被布隆过滤器拦截并计数
- **WHEN** 客户端请求一个不存在的 shortUri，且该 URI 未在布隆过滤器中登记
- **THEN** 系统 SHALL 将 `bloomFilterInterceptCount` 计数器加 1，并将请求重定向至 `/page/notfound`

#### Scenario: 有效短链接请求不触发布隆过滤器拦截计数
- **WHEN** 客户端请求一个已存在的短链接
- **THEN** 系统 MUST NOT 增加 `bloomFilterInterceptCount` 计数器

### Requirement: 系统必须对 Sentinel 限流触发事件计数
系统 MUST 在短链接创建接口触发 Sentinel 限流时，将该限流事件计入进程内原子计数器，并通过聚合指标接口暴露该计数值。

#### Scenario: 超过 QPS 阈值触发限流并计数
- **WHEN** 客户端在 1 秒内对创建短链接接口发起超过限流阈值的请求
- **THEN** 被限流的请求 SHALL 返回限流错误响应，且 `sentinelBlockCount` 计数器 MUST 加 1

#### Scenario: 限流解除后正常请求不计入限流计数
- **WHEN** 限流窗口结束后客户端再次发起正常频率请求
- **THEN** `sentinelBlockCount` SHALL 不再增加，请求 MUST 正常处理
