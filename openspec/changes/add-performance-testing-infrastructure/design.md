## Context

本项目为面试展示工程，技术栈为 Spring Boot 3.0.7 + Vue3 + MySQL（ShardingSphere 分库分表） + Redis + RocketMQ + Sentinel。

**当前可观测性状态：**
- 已存在 `CacheMonitoringServiceImpl`，通过 Redis 计数器记录每日缓存命中/未命中次数，并提供 `getTodayHitRate()` 方法。
- 已有 `spring-boot-starter-actuator` 和 `micrometer-registry-prometheus` 依赖，但 `application.yaml` 中**未配置任何 `management.endpoints` 暴露规则**，实际上 `/actuator/metrics` 等端点不可访问。
- Sentinel 限流规则：`create_short-link` 资源 QPS 限制为 1（`SentinelRuleConfig`），但无限流触发次数统计。
- 布隆过滤器：`shortUriCreateCachePenetrationBloomFilter` 已在重定向流程中使用，但未对拦截事件计数。
- 目前**无任何 JMeter 压测文件**，无面试可用的性能测试文档。

**面试痛点：** 当面试官问"你的系统缓存命中率是多少？" / "QPS 大概多少？" / "限流是否生效？"时，没有可演示的实时数据和压测基准。

## Goals / Non-Goals

**Goals:**
- 开放 Actuator `/actuator/metrics` 和 `/actuator/prometheus` 端点（需配置 `application.yaml`）
- 新增 `/api/short-link/v1/metrics/summary` 聚合接口，一次返回所有面试关键指标：缓存命中率、布隆过滤器拦截统计、Sentinel 限流触发次数、当日 PV 总量
- 在布隆过滤器未命中路径新增 `AtomicLong` 计数器，暴露"布隆过滤器拦截总次数"
- 在 `CustomBlockHandler`（Sentinel 降级处理器）中增加限流触发计数器，暴露限流次数
- 提供两套 JMeter 测试计划（`.jmx` 文件）：
  1. **创建 QPS 压测计划**：验证限流（QPS=1）、测量创建吞吐量
  2. **重定向压测计划**：验证缓存命中率（预热后应 >95%）、测量跳转响应时间
- 提供面试测试手册（`test/jmeter/README.md`），包含预期指标数值、操作步骤、结果解读

**Non-Goals:**
- 不引入 Grafana/Prometheus 完整监控栈（复杂度超出单机展示需要）
- 不修改 ShardingSphere 或 RocketMQ 配置
- 不对限流 QPS 阈值进行永久调整（面试时可临时演示）
- 不做数据库慢查询分析（不在面试展示核心范围）

## Decisions

### 决策 1：使用进程内 AtomicLong 而非 Redis 存储布隆过滤器/限流统计

**选择**：新增 `PerformanceCounterService`，内部用 `AtomicLong` 存储布隆过滤器拦截次数和 Sentinel 限流触发次数。

**原因**：
- 布隆过滤器拦截是纯进程内操作，无需跨实例共享（单机演示场景）
- 避免在高并发重定向路径引入额外 Redis RTT
- 与现有 `CacheMonitoringServiceImpl` 的 Redis 计数器风格互补：缓存命中率用 Redis（支持多日趋势），布隆/限流计数用本地 AtomicLong（实时性更高）

**替代方案**：用 Micrometer Counter（`MeterRegistry`）——优点是可直接通过 `/actuator/metrics` 查询，缺点是 JMeter 测试时观测路径更复杂。最终选择自建聚合接口，降低面试演示难度。

### 决策 2：新增聚合 Metrics 接口而非直接用 `/actuator/metrics`

**选择**：新增 `GET /api/short-link/v1/metrics/summary` 接口，一次性返回所有关键指标的 JSON。

**原因**：
- `/actuator/metrics` 每次只查询单一指标，面试时需要多次调用才能展示全貌
- 聚合接口可以自定义字段名，使输出更直观（如 `cacheHitRate: 97.3%`）
- 接口路径在系统现有 Controller 体系中（`/api/short-link/v1/...`），符合已有路由规范

**接口响应结构：**
```json
{
  "cacheHitRate": "97.3%",
  "cacheHitCount": 14782,
  "cacheMissCount": 402,
  "bloomFilterInterceptCount": 156,
  "sentinelBlockCount": 48,
  "todayRedirectTotal": 15184
}
```

### 决策 3：JMeter 测试计划分为两个独立 .jmx 文件

**选择**：`create-qps-test.jmx`（短链创建 + 限流验证）和 `redirect-cache-test.jmx`（重定向缓存命中率验证）分开存放于 `test/jmeter/` 目录。

**原因**：
- 两个场景测试目标不同：创建测限流（低 QPS）、重定向测缓存（高 QPS + 预热）
- 分开后可以独立运行，避免互相干扰（创建操作会污染缓存预热状态）
- JMeter 文件结构按面试回答粒度设计：每个文件对应一个面试问题场景

### 决策 4：Actuator 端点开放策略

在 `application.yaml` 新增：
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  endpoint:
    health:
      show-details: always
```
仅开放必要端点，不开放 `shutdown`、`env`（包含敏感配置）等高风险端点。

## Risks / Trade-offs

- **[风险] 进程内 AtomicLong 重启后归零** → 面试前记得先跑一轮压测热身，并在演示前说明"这是本次运行期间的统计"
- **[风险] 布隆过滤器计数在重定向降级路径中可能误计** → 在 `restoreUrl` 中 `!contains` 分支的"查库后未找到"出口计数，而非查库前，确保只统计真正被拦截的无效请求
- **[权衡] 聚合接口无鉴权** → 该接口仅用于面试演示，不在 JWT 过滤器白名单之外，需确认接口路径已加入白名单或临时放开
- **[权衡] JMeter 文件硬编码 localhost:8001** → 使用 JMeter 用户自定义变量（User Defined Variables）统一管理 `host` 和 `port`，方便修改

## Migration Plan

1. 修改 `application.yaml`：新增 `management.endpoints` 配置（1 行改动，无 DB migration）
2. 新增 `PerformanceCounterService` 接口 + `PerformanceCounterServiceImpl`（进程内计数）
3. 修改 `ShortLinkServiceImpl.restoreUrl()`：在布隆过滤器未命中 → 查库未命中分支调用 `performanceCounterService.incrementBloomFilterIntercept()`
4. 修改 `CustomBlockHandler`：在限流处理方法中调用 `performanceCounterService.incrementSentinelBlock()`
5. 新增 `PerformanceMetricsController`：提供 `/api/short-link/v1/metrics/summary` 接口
6. 新增 `test/jmeter/create-qps-test.jmx` 和 `test/jmeter/redirect-cache-test.jmx`
7. 新增 `test/jmeter/README.md`：面试测试文档

**回滚策略**：所有新增为纯 additive 改动，删除新增类即可回滚；`application.yaml` 的 actuator 配置可直接注释。

## Open Questions

- 限流 QPS 阈值当前为 1（`SentinelRuleConfig`），面试压测时是否需要临时调高到 10 或 100 以便演示"正常创建"再"触发限流"的对比？建议在 tasks 中作为可配置项处理。
- `/api/short-link/v1/metrics/summary` 接口是否需要加入 JWT 白名单？需要在 tasks 中明确过滤器配置。
