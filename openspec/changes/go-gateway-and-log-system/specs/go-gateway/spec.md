## ADDED Requirements

### Requirement: 路由转发
网关 SHALL 根据 YAML 配置的路由规则将请求转发到下游服务。路由匹配基于路径前缀和 Host 头。

#### Scenario: 短链接跳转请求路由
- **WHEN** 请求路径匹配 `/s/*` 且 Host 为配置的短链接域名
- **THEN** 请求转发至短链接解析服务

#### Scenario: 未匹配路由返回 404
- **WHEN** 请求不匹配任何已配置路由
- **THEN** 网关返回 404 状态码和 JSON 错误体

### Requirement: 限流保护
网关 SHALL 对每个路由实施令牌桶限流。当令牌耗尽时，返回 429 Too Many Requests。

#### Scenario: 正常请求通过限流
- **WHEN** 请求到达时令牌桶中有可用令牌
- **THEN** 请求被放行并消耗一个令牌

#### Scenario: 超限时返回 429
- **WHEN** 请求到达时令牌桶中无可用令牌
- **THEN** 网关返回 429 状态码

### Requirement: 跨域处理
网关 SHALL 支持 CORS 配置，允许指定 Origin、Method 和 Header 的跨域请求。

#### Scenario: 预检请求处理
- **WHEN** 收到 OPTIONS 预检请求且 Origin 在白名单中
- **THEN** 返回 204 和正确的 CORS 响应头

#### Scenario: 非法 Origin 拒绝
- **WHEN** 请求 Origin 不在白名单中
- **THEN** 不返回任何 CORS 头

### Requirement: TraceID 注入
网关 SHALL 为每个入站请求生成唯一的 `X-Trace-ID`（UUID v4），并通过 HTTP Header 向下游传递。

#### Scenario: 新请求生成 TraceID
- **WHEN** 请求到达网关且未携带 `X-Trace-ID` 头
- **THEN** 网关生成 UUID v4 作为 TraceID 并注入到请求头

#### Scenario: 透传已有 TraceID
- **WHEN** 请求已携带 `X-Trace-ID` 头
- **THEN** 网关保留该 TraceID 并向下游透传

### Requirement: 访问日志
网关 SHALL 为每个请求记录结构化 JSON 访问日志，包含时间戳、方法、路径、状态码、延迟、TraceID。

#### Scenario: 请求完成记录日志
- **WHEN** 请求处理完成
- **THEN** 网关输出包含 method、path、status_code、latency_ms、trace_id 的 JSON 日志行

### Requirement: 健康检查端点
网关 SHALL 暴露 `/health` GET 端点，返回服务健康状态。

#### Scenario: 健康检查
- **WHEN** 请求 GET `/health`
- **THEN** 返回 200 和 JSON `{"status": "ok"}`
