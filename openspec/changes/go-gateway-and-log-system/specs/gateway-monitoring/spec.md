## ADDED Requirements

### Requirement: 网关性能指标 API
网关 SHALL 暴露 `/api/v1/metrics` 端点，返回网关自身的性能指标。

#### Scenario: 返回 QPS
- **WHEN** 请求 GET `/api/v1/metrics`
- **THEN** 返回 JSON 包含当前 QPS、P50/P90/P99 延迟、错误率、活跃连接数

#### Scenario: 实时统计窗口
- **WHEN** 查询性能指标
- **THEN** 指标基于最近 1 分钟的滑动窗口计算

### Requirement: 请求统计
网关 SHALL 内部维护请求计数器、延迟直方图和错误计数器。

#### Scenario: 请求计数递增
- **WHEN** 每个请求处理完成
- **THEN** 对应路由的请求计数器 +1

#### Scenario: 延迟记录
- **WHEN** 每个请求处理完成
- **THEN** 记录该请求的处理延迟到直方图

#### Scenario: 错误计数
- **WHEN** 请求返回 5xx 状态码
- **THEN** 错误计数器 +1

### Requirement: 限流状态可观测
网关 SHALL 在性能指标中暴露限流状态（当前令牌桶余量、限流拒绝次数）。

#### Scenario: 限流状态
- **WHEN** 请求 GET `/api/v1/metrics`
- **THEN** 返回各路由的令牌桶剩余量和被拒绝的请求数

### Requirement: 指标 API 安全
网关性能指标端点 SHALL 仅接受来自内部网络的请求。

#### Scenario: 外部请求拒绝
- **WHEN** 从非回环地址访问 `/api/v1/metrics`
- **THEN** 返回 403 Forbidden
