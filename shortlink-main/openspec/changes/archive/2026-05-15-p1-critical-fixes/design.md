## Context

项目当前状态：Spring Boot 3.0.7 + MyBatis Plus 3.5.3.1，无 Spring Security，密码明文存储，密钥硬编码，无输入校验，restoreUrl 为 132 行 God Method，可观测性指标使用 AtomicLong 未注册到 Micrometer。

目标：修复报告中标记为 P1 的 5 项致命问题，向面试官展示安全意识和系统设计能力。

## Goals / Non-Goals

**Goals:**
- 用户密码使用 BCrypt 哈希存储，登录时哈希比对
- 敏感配置项通过 `${ENV_VAR}` 引用环境变量，提供 `.env.example` 模板
- 所有 Controller 输入参数添加 Bean Validation，统一异常处理
- `restoreUrl` 拆分为 5 个独立缓存层 handler，消除复制粘贴
- Micrometer 注册核心业务指标，/actuator/prometheus 可抓取
- 添加 `logback-spring.xml` 提供控制台 + 文件双输出，滚动保留

**Non-Goals:**
- 不引入 Spring Security（过度引入会掩盖自己对安全的理解）
- 不修改 ShardingSphere 分片规则
- 不修改前端
- 不添加 OpenTelemetry 分布式追踪（P2 范围）
- 不重构 Consumer/Transaction 边界（P2 范围）

## Decisions

### D1: BCrypt 而非 Argon2

**选择**：BCrypt via `org.springframework.security:spring-security-crypto`

**理由**：Spring Boot 3 内置支持，`BCryptPasswordEncoder` 一行即可集成，无需额外依赖。Argon2 更安全但需要 `BouncyCastle` 额外依赖，且 JDK 17 没有原生支持。对于展示安全意识的面试场景，BCrypt 足够解释"为什么哈希""为什么加盐""work factor 的含义"。`spring-security-crypto` 只引入加密模块，不引入认证链，保持轻量。

**替代方案**：自写 SHA-256 + 随机盐 — 面试中容易被追问盐管理、彩虹表、迭代次数等细节，不如直接用 BCrypt 标准实现。

### D2: 环境变量命名规范

**选择**：使用 Spring Boot `${}` 占位符 + 默认 dev 值

格式：`${SHORTLINK_DB_PASSWORD:root}` — 必须大写，`SHORTLINK_` 前缀统一命名空间，冒号后为 dev 默认值。

改造配置项：
| 配置项 | 环境变量 | 原硬编码值 |
|--------|---------|-----------|
| 数据库密码 | `SHORTLINK_DB_PASSWORD` | `root` |
| AES 加密密钥 | `SHORTLINK_AES_SECRET_KEY` | `d6oadClrrb9A3GWo` |
| 高德 API Key | `SHORTLINK_AMAP_KEY` | `99b616b8433a465891a505ab161746af` |

提供 `main/.env.example` 作为模板文件。

### D3: restoreUrl 重构为模板方法

**选择**：在 `ShortLinkServiceImpl` 内部拆分为 5 个 private 方法 + 一个编排方法，不引入外部策略类。

```
restoreUrl(shortUri, request, response)
  │
  ├─ tryL1Cache(fullShortUrl)        → Caffeine 命中 → 重定向
  ├─ tryL2Cache(fullShortUrl)        → Redis 命中 → 回填 L1 → 重定向
  ├─ tryNullCache(fullShortUrl)      → 空值缓存命中 → 404
  ├─ tryBloomFilter(fullShortUrl)    → Bloom 未命中 → 查库
  └─ tryDatabaseWithLock(fullShortUrl) → 分布式锁 + 查库 → 重定向/404
```

每个方法返回 `Optional<String>`（原始 URL 或 empty），编排方法按顺序尝试。消除 5 处重复的 stats 记录代码，抽取 `recordStatsAndRedirect(originUrl)` 统一方法。

**替代方案**：责任链模式（每个 handler 一个类）更适合多个缓存层独立变化的场景。但 5 个层是固定的（L1→L2→null→Bloom→DB），使用责任链过度工程化。保持提取为 private 方法更符合"只做必要的抽象"。

### D4: Micrometer 指标注册

**选择**：`PerformanceCounterServiceImpl` 中 AtomicLong → `MeterRegistry.counter()` / `MeterRegistry.timer()`，Spring Boot Actuator 自动暴露。

核心指标：
```
shortlink_redirect_total{cache_level="L1|L2|L3|null_cache|not_found"}  // Counter
shortlink_cache_hit_total{cache_type="l1|l2"}                            // Counter
shortlink_cache_miss_total                                               // Counter
shortlink_redirect_duration_seconds                                      // Timer (Histogram)
shortlink_errors_total{error_type="...")                                  // Counter (可选)
```

`CacheMonitoringServiceImpl` 中的异步 INCR 改为同步 `meterRegistry.counter().increment()`，消除 MONITOR_EXECUTOR 的 DiscardPolicy 问题。

**替代方案**：自定义 Prometheus Collector — 增加复杂度，Micrometer 已足够。

### D5: 日志结构化

选择 `logback-spring.xml`，JSON 格式 + 控制台文本双输出，文件按天滚动保留 7 天。不需要引入 logstash-logback-encoder（太重），使用标准 `ch.qos.logback.classic.encoder.PatternLayoutEncoder` + `RollingFileAppender`。

## Risks / Trade-offs

- [R1] BCrypt 哈希需要扩展 `t_user.password` 列宽 → 当前 `varchar(256)` 足够（BCrypt 输出固定 60 字符），无需迁移
- [R2] 旧用户密码是明文 → 提供一次性迁移方案：启动时检测，若 BCrypt 验证失败则不兼容，需重新注册。无历史用户数据，风险为零
- [R3] `restoreUrl` 重构可能引入缓存行为的 regression → 现有 `ShortLinkServiceRestoreTest` 等 4 个测试类的 16 个测试作为回归保护
- [R4] Micrometer 指标注册从异步变同步增加微小延迟 → Counter 递增是 O(1) 内存操作，无 I/O，影响可忽略

## Open Questions

- 是否需要在设计文档中包含前端相关的安全改进（如登录页面输入校验）? → 暂不，P1 聚焦后端
