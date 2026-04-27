## 1. 基础配置与 Actuator 暴露

- [x] 1.1 在 `application.yaml` 中配置 Actuator 端点暴露规则，包含 `health,metrics,prometheus,info`
- [x] 1.2 将 `/actuator/**` 和 `/api/short-link/v1/metrics/summary` 加入 JWT 过滤器的白名单（如有必要）

## 2. 性能计数服务实现

- [x] 2.1 创建 `PerformanceCounterService` 接口及其实现类 `PerformanceCounterServiceImpl`
- [x] 2.2 在 `PerformanceCounterServiceImpl` 中实现基于 `AtomicLong` 的布隆过滤器拦截计数和 Sentinel 限流触发计数
- [x] 2.3 修改 `ShortLinkServiceImpl.restoreUrl`，在布隆过滤器拦截逻辑中调用计数器增加方法
- [x] 2.4 修改 `CustomBlockHandler`，在限流触发逻辑中调用计数器增加方法

## 3. 指标聚合接口开发

- [x] 3.1 在 `ShortLinkController` 或新创建的 `PerformanceMetricsController` 中新增 `GET /api/short-link/v1/metrics/summary` 接口
- [x] 3.2 接口逻辑：聚合 `CacheMonitoringService` 的缓存数据和 `PerformanceCounterService` 的实时计数数据
- [x] 3.3 验证接口输出格式符合 `design.md` 中的 JSON 结构定义

## 4. JMeter 压测计划编写

- [x] 4.1 在 `test/jmeter/` 目录下创建 `create-qps-test.jmx`
- [x] 4.2 在 `create-qps-test.jmx` 中配置两组测试：一组调用普通创建接口，一组调用分布式锁创建接口
- [x] 4.3 在 `test/jmeter/` 目录下创建 `redirect-cache-test.jmx`，包含高并发重定向和无效 URI 拦截验证
- [x] 4.4 确保所有 `.jmx` 文件使用变量（UDV）管理 host、port 等参数

## 5. 面试测试文档编写

- [x] 5.1 创建 `test/jmeter/README.md` 面试测试手册
- [x] 5.2 编写分布式锁 vs 非锁创建性能对比实验步骤及预期 QPS 数值说明
- [x] 5.3 编写缓存命中率演示步骤及预热方法
- [x] 5.4 编写布隆过滤器拦截演示步骤
