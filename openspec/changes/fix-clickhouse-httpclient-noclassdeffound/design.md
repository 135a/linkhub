## 架构背景 (Context)

项目使用 Spring Boot 3.0.7 构建，最近升级了缓存与 ClickHouse（`clickhouse-jdbc` 0.6.3），ClickHouse JDBC 驱动底层使用 Apache HttpClient 5 作为 HTTP 通信引擎。由于 Spring Boot 默认管理的 `httpclient5` 版本（如 5.2.x 早期版本或 5.1.x）缺少 `org.apache.hc.client5.http.config.ConnectionConfig` 类，导致运行时在初始化 ClickHouse 连接时抛出 `NoClassDefFoundError`。

## 目标与非目标 (Goals / Non-Goals)

**目标 (Goals):**
- 解决 `ConnectionConfig` 找不到的异常，恢复系统 ClickHouse 数据库的 HTTP 连接能力。
- 确保指定的 `httpclient5` 版本与系统内现有的其它依赖（如 Spring、Hutool）兼容。

**非目标 (Non-Goals):**
- 替换 ClickHouse 的底层连接协议（不改成 TCP/gRPC）。
- 升级整个 Spring Boot 框架版本。

## 设计决策 (Decisions)

1. **显式指定 `httpclient5` 版本号**
   - **决策**: 在 `shortlink-main/main/pom.xml` 中将 `<dependency><groupId>org.apache.httpcomponents.client5</groupId><artifactId>httpclient5</artifactId><version>5.3.1</version></dependency>` 的版本显式覆盖。
   - **理由**: `ConnectionConfig` 类在较新版本的 HttpClient 5（如 `5.2.2` 及以上版本，通常选择稳定的 `5.3.x` 系列如 `5.3.1`）中得到支持和结构调整。通过覆盖 Spring Boot 依赖管理的默认版本，可以解决 `clickhouse-jdbc` 0.6.3 的强制依赖。
   - **替代方案**: 降级 `clickhouse-jdbc` 版本。但这可能会引入其他已知的 bug 或放弃 HTTP 协议性能优化，因此选择升级并固定 HttpClient 5 的版本。

2. **仅在依赖 ClickHouse 的模块修改**
   - **决策**: 将特定版本的 `httpclient5` 定义在 `main/pom.xml` 中。
   - **理由**: 减小对项目全局其他可能未使用该 HttpClient 的模块产生未知的兼容性影响。

## 风险与权衡 (Risks / Trade-offs)

- **[风险] 导致其他依赖于 HttpClient 的第三方库冲突**: Spring Boot Web 环境和可能依赖该类库的其他组件可能受影响。
  - **缓解措施**: 在修改 POM 后进行本地全量编译并启动应用，重点测试高并发写入和外部 API 调用场景。

## 待确认问题 (Open Questions)

- ClickHouse 0.6.3 驱动是否还对其他 httpclient5 组件（如 `httpcore5`）有隐式的版本要求？需要一并验证。
