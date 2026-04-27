## MODIFIED Requirements

### Requirement: 系统必须输出可量化运行指标与结构化日志
系统 MUST 产出结构化日志与基础指标，包括短链创建成功率、跳转成功率、4xx/5xx 比例与响应延迟分位数，以支持质量趋势分析；指标与日志采集 MUST 可在独立单体部署下直接启用，不依赖注册中心或远程调用链路。此外，系统 MUST 额外追踪并暴露以下细粒度指标：Redis 缓存命中率（按日统计，支持当日及近 7 日趋势）、布隆过滤器拦截次数（自进程启动起的累计计数）、Sentinel 限流触发次数（自进程启动起的累计计数），上述三项指标 MUST 可通过聚合接口 `/api/short-link/v1/metrics/summary` 一次性获取。

#### Scenario: 指标可被定期采集
- **WHEN** 服务正常运行
- **THEN** 指标端点或日志汇聚 SHALL 提供可机器读取的稳定数据格式用于采集

#### Scenario: 异常请求可追溯
- **WHEN** 发生 5xx 错误或异常跳转
- **THEN** 日志 MUST 包含请求标识、错误类型与关键上下文以支持定位根因

#### Scenario: 缓存命中率指标可实时查询
- **WHEN** 运维人员或面试演示者调用 `/api/short-link/v1/metrics/summary`
- **THEN** 系统 MUST 返回当日缓存命中次数、未命中次数及命中率百分比，且数据 SHALL 反映最近一次重定向请求后的最新状态

#### Scenario: 布隆过滤器拦截统计可实时查询
- **WHEN** 运维人员或面试演示者调用 `/api/short-link/v1/metrics/summary`
- **THEN** 系统 MUST 返回自进程启动以来布隆过滤器拦截的无效请求总次数

#### Scenario: 限流触发次数可实时查询
- **WHEN** 运维人员或面试演示者调用 `/api/short-link/v1/metrics/summary`
- **THEN** 系统 MUST 返回自进程启动以来 Sentinel 限流拦截的请求总次数
