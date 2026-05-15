## ADDED Requirements

### Requirement: Micrometer 替换 AtomicLong 计数器

系统 MUST 将 `PerformanceCounterServiceImpl` 中的 `AtomicLong` 计数器替换为 Micrometer `Counter` 和 `Timer`，注册到全局 `MeterRegistry`。

#### Scenario: 重定向计数作为 Micrometer Counter

- **WHEN** 一次短链接重定向完成
- **THEN** `shortlink_redirect_total` Counter（tag: `cache_level=L1|L2|L3|null_cache|not_found`）自增 1

#### Scenario: 缓存命中计数作为 Micrometer Counter

- **WHEN** 缓存 L1 或 L2 命中
- **THEN** `shortlink_cache_hit_total` Counter（tag: `cache_type=l1|l2`）自增 1

#### Scenario: 缓存未命中计数

- **WHEN** L1 和 L2 均未命中（需要查询 L3 或返回 404）
- **THEN** `shortlink_cache_miss_total` Counter 自增 1

#### Scenario: 重定向耗时记录

- **WHEN** 一次 `restoreUrl` 处理完成
- **THEN** `shortlink_redirect_duration_seconds` Timer 记录本次处理耗时（秒）

### Requirement: Prometheus endpoint 可抓取指标

系统 MUST 确保 `/actuator/prometheus` endpoint 输出上述 Micrometer 指标，格式符合 Prometheus text exposition format。

#### Scenario: Prometheus 抓取

- **WHEN** Prometheus 或类似系统 GET `/actuator/prometheus`
- **THEN** 返回包含 `shortlink_redirect_total{cache_level="L1",} 123.0` 等格式化指标的文本

### Requirement: 缓存监控异步写入改为同步

系统 SHALL 将 `CacheMonitoringServiceImpl` 中的异步 INCR（通过 MONITOR_EXECUTOR）改为同步调用 `meterRegistry.counter().increment()`，消除 DiscardPolicy 丢失监控数据的问题。

#### Scenario: 缓存事件实时记录

- **WHEN** `recordHitAsync` 或 `recordMissAsync` 被调用
- **THEN** 对应的 Micrometer Counter 在当前线程同步递增
- **AND** 不再通过 `MONITOR_EXECUTOR` 线程池提交

### Requirement: 结构化日志配置

系统 MUST 提供 `logback-spring.xml` 配置文件，支持控制台文本输出和文件 JSON 输出。

#### Scenario: 日志输出到控制台和文件

- **WHEN** 应用启动
- **THEN** 日志同时输出到控制台（文本格式）和 `logs/shortlink.log`（按天滚动，保留 7 天）
- **AND** 默认日志级别为 INFO，`com.nym.shortlink` 包为 DEBUG

#### Scenario: 日志包含 TraceId（当注入时）

- **WHEN** 请求带有 `X-Trace-Id` header
- **THEN** 日志输出中包含 `%mdc{traceId}` 占位符的值（若有）
