## 1. 添加依赖

- [x] 1.1 添加 `spring-security-crypto` 依赖到 `main/pom.xml`（仅 crypto 模块，不引入 spring-security 全家桶）

## 2. 密码安全

- [x] 2.1 创建 `PasswordEncoderConfig`，注册 `BCryptPasswordEncoder` Bean（work factor=10）
- [x] 2.2 修改 `UserServiceImpl.register()` — 存储前对密码做 `passwordEncoder.encode()`
- [x] 2.3 修改 `UserServiceImpl.login()` — 使用 `passwordEncoder.matches()` 比对，删除 Redis session 缓存绕过逻辑
- [x] 2.4 检查 `t_user` 表 password 列宽（确保≥60），若不足生成 ALTER TABLE DDL

## 3. 密钥管理

- [x] 3.1 修改 `shardingsphere-config-dev.yaml` — 数据库密码和 AES 密钥改为 `${SHORTLINK_DB_PASSWORD:root}` / `${SHORTLINK_AES_SECRET_KEY:d6oadClrrb9A3GWo}`
- [x] 3.2 修改 `shardingsphere-config-prod.yaml` — 同上（无默认值：`${SHORTLINK_DB_PASSWORD}` / `${SHORTLINK_AES_SECRET_KEY}`）
- [x] 3.3 修改 `application.yaml` — 高德 API Key 改为 `${SHORTLINK_AMAP_KEY:99b616b8433a465891a505ab161746af}`
- [x] 3.4 创建 `main/.env.example` 模板文件

## 4. 输入校验 + 全局异常处理

- [x] 4.1 为所有 Req DTO 添加 Bean Validation 注解（`@NotBlank`, `@Size`, `@Email` 等）
- [x] 4.2 所有 Controller 方法的 `@RequestBody` 参数添加 `@Valid` 注解
- [x] 4.3 创建全局异常处理器 `GlobalExceptionHandler`，处理 `MethodArgumentNotValidException` 和 `ConstraintViolationException`
- [x] 4.4 添加 `RequestBody` 校验失败的友好错误响应格式（字段+错误信息列表）

## 5. restoreUrl 多级缓存拆分

- [x] 5.1 提取 Caffeine 缓存查询逻辑到 `restoreUrl` 编排方法中
- [x] 5.2 提取 Redis L2 缓存查询逻辑
- [x] 5.3 提取空值缓存检查
- [x] 5.4 提取 `resolveByBloomFilter()` — Bloom filter + 降级查库
- [x] 5.5 提取 `queryDatabaseUnderLock()` — 分布式锁 + 双重检查 + 查库
- [x] 5.6 重写 `restoreUrl()` 为编排方法 + `redirectWithStats()` / `redirectToNotFound()` 统一方法

## 6. Prometheus 指标 + 结构化日志

- [x] 6.1 重构 `PerformanceCounterServiceImpl` — `AtomicLong` 替换为 `MeterRegistry.counter()`
- [x] 6.2 实现 `shortlink_redirect_total`（tag: cache_level）、`shortlink_cache_hit_total`（tag: cache_type）、`shortlink_cache_miss_total`、`shortlink_redirect_duration_seconds`
- [x] 6.3 重构 `CacheMonitoringServiceImpl` — 异步 INCR 改为同步 `meterRegistry.counter().increment()`，移除 `MONITOR_EXECUTOR`
- [x] 6.4 创建 `main/src/main/resources/logback-spring.xml` — 控制台文本 + 文件按天滚动 7 天保留
- [x] 6.5 添加 `%mdc{traceId}` 占位符支持 TraceId（logback-spring.xml 已包含）

## 7. 测试验证

- [x] 7.1 更新 `UserServiceTest` — 验证 BCrypt 哈希存储和验证逻辑
- [x] 7.2 更新 Controller 测试 — 验证 `@Valid` 校验 + 全局异常处理返回 400
- [x] 7.3 确认现有 `ShortLinkServiceRestoreTest` 等 16 个缓存测试全部通过（回归保护）
- [x] 7.4 运行 `mvn test` 确认 102 个测试 0 失败
