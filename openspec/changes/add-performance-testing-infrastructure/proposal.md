## Why

本项目作为面试展示工程，需要能够在面试官提问时，快速、可信地展示系统各项关键性能指标（缓存命中率、QPS、布隆过滤器拦截率、限流效果、接口响应时间等）。目前系统缺乏标准化的压测方案与可观测性指标端点，无法在面试场景中以数据为支撑进行技术深度阐述。

## What Changes

- 新增 `/api/short-link/v1/metrics/summary` 接口，聚合输出布隆过滤器命中率、Redis 缓存命中率、当日 QPS 峰值等核心指标
- 在 Spring Boot Actuator 基础上，扩展自定义 Micrometer 计数器/计时器：短链接重定向计数、缓存命中/未命中、布隆过滤器拦截、限流触发次数
- 新增布隆过滤器拦截统计日志（命中时额外打印计数）
- 提供完整的 JMeter 测试计划（`.jmx` 文件），覆盖以下场景：
  - 短链接创建 QPS 压测（含用户限流验证）
  - 短链接重定向 QPS 压测（缓存命中路径 vs 缓存穿透路径）
  - 布隆过滤器拦截率验证（大量不存在 shortUri 的请求）
- 新增详细面试测试文档，包含各指标预期数值区间、JMeter 参数配置说明、结果解读方法

## Capabilities

### New Capabilities

- `performance-metrics-api`: 暴露聚合运行时指标的 REST 接口，供面试时实时展示缓存命中率、QPS、布隆过滤器统计等数据
- `jmeter-test-plans`: 提供标准化 JMeter 压测文件，覆盖短链接创建、重定向、限流、缓存穿透等典型面试场景，并附带测试执行说明与结果解读文档

### Modified Capabilities

- `testability-observability-baseline`: 在现有指标端点基础上，新增布隆过滤器命中率、Redis 缓存命中率、Sentinel 限流触发次数等细粒度可观测性需求，以支撑面试场景的量化问答

## Impact

- **后端代码**：`ShortLinkServiceImpl`（布隆过滤器统计）、`ShortLinkController`（新增 metrics 接口）、`application.yaml`（Actuator 端点开放配置）、新增 `PerformanceMetricsService` 及 Micrometer 配置类
- **依赖**：无新增依赖，复用已有的 `spring-boot-starter-actuator` 和 `micrometer-registry-prometheus`
- **测试资源**：新增 `test/jmeter/` 目录，包含 `.jmx` 压测文件和 `README.md` 面试测试文档
- **配置**：开放 Actuator metrics/prometheus 端点，确保本地和 Docker 环境均可访问
