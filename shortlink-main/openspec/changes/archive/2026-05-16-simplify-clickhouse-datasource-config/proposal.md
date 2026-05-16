## Why

ClickHouse 数据源配置在 `ClickHouseDataSourceConfig.java` 中用 `@Value` 逐个注入属性再手动构建 `HikariConfig`，代码冗长（~45行 Java）、连接池参数硬编码。而 `application.yaml` 中已经定义了 `clickhouse.datasource.*` 配置项，完全可以直接用 Spring Boot 的 `@ConfigurationProperties` 绑定，消除手写代码。

## What Changes

- `ClickHouseDataSourceConfig.java`：用 `@ConfigurationProperties("clickhouse.datasource")` + `DataSourceBuilder` 替代 `@Value` 注入和手动 `HikariConfig` 构建
- `application.yaml`：在 `clickhouse.datasource` 下增加 `hikari.*` 子配置，将原硬编码的 pool 参数迁移到 yml 管理

## Capabilities

### New Capabilities

- `clickhouse-config-simplify`: 将 ClickHouse 数据源从 Java 手动构建改为 Spring Boot ConfigurationProperties 自动绑定

### Modified Capabilities

- `db-config-cleanup`: ClickHouse 连接的连接池参数（max-pool-size、min-idle、connection-timeout 等）从 Java 硬编码迁移到 yml，符合该 spec "避免手动定义与框架重复的 Bean" 的精神（虽然 ClickHouse 作为第二数据源必须手动注册 Bean，但 DataSource 的组装应交由框架绑定）

## Impact

- `main/src/main/java/com/nym/shortlink/core/config/ClickHouseDataSourceConfig.java` — 代码量缩减约 70%
- `main/src/main/resources/application.yaml` — 新增 `clickhouse.datasource.hikari.*` 配置项
