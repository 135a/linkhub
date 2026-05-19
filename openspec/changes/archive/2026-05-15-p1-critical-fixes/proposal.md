## Why

项目作为大厂实习面试作品，存在 10 项致命问题会在面试中被直接判定为不合格。本变更针对其中面试评分权重最高、修复收益最大的 5 个方向，覆盖安全、代码设计、可观测性三个核心维度。这些问题如果不修，面试官会在"安全意识""系统设计能力""生产环境经验"三个考察点直接给出负面评价。

## What Changes

### 安全增强
- 用户密码从明文存储改为 BCrypt 哈希存储与验证
- 数据库密码、AES 加密密钥、高德 API Key 从硬编码迁移到环境变量
- Controller 层添加 `@Valid` / `@Validated` 输入校验，覆盖所有用户输入的请求参数

### 代码架构优化
- `ShortLinkServiceImpl.restoreUrl()` 从 132 行 God Method 拆分为多级缓存策略模式，每个缓存层独立 handler
- 消除方法内 5 处 "缓存命中→记录统计→重定向" 的复制粘贴

### 可观测性
- 将 `PerformanceCounterServiceImpl` 中的 `AtomicLong` 计数器迁移到 Micrometer MeterRegistry
- 对标 `docs/observability.md` 文档，实现 `shortlink_redirect_total`、`shortlink_cache_hit_total` 等核心指标
- 添加 `logback-spring.xml`，提供结构化日志配置

## Capabilities

### New Capabilities

- `password-hashing`: 用户密码 BCrypt 哈希存储，登录时哈希比对，用户注册/修改密码时自动哈希
- `secrets-management`: 敏感配置项（数据库密码、AES 密钥、第三方 API Key）通过环境变量注入，不再硬编码在 yaml 文件中
- `input-validation`: Controller 层所有请求 DTO 通过 Bean Validation 校验，统一全局异常处理返回友好错误信息
- `cache-strategy-refactor`: restoreUrl 多级缓存链路从单一 God Method 重构为策略模式/责任链
- `prometheus-metrics`: Micrometer 指标替换 AtomicLong，/actuator/prometheus 输出可被 Prometheus 抓取的格式化指标

### Modified Capabilities

<!-- 无现有 spec 需要修改 -->

## Impact

### 代码层面
- `UserServiceImpl.java` — 注册/登录逻辑，明文比较改为 BCrypt
- `UserDO.java` — password 字段不变，但存储值变为哈希
- `shardingsphere-config-dev.yaml` / `shardingsphere-config-prod.yaml` — 硬编码值替换为 `${ENV_VAR}` 占位符
- `application.yaml` — 高德 API Key 替换为环境变量引用
- 所有 Controller 类 — 添加 `@Valid` 注解
- `ShortLinkServiceImpl.java` — restoreUrl 拆分，提取缓存层级策略
- `PerformanceCounterServiceImpl.java` / `PerformanceCounterService.java` — AtomicLong → MeterRegistry
- `CacheMonitoringServiceImpl.java` — 同上
- 新增 `main/src/main/resources/logback-spring.xml` — 结构化日志

### 不涉及
- 数据库 schema 变更（BCrypt 哈希长度兼容现有 varchar 字段，需要扩展长度到 60+）
- ShardingSphere 分片规则变更
- 前端变更
