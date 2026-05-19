## Context

当前 `ClickHouseDataSourceConfig.java` 通过 `@Value` 逐个注入 3 个 yml 属性（url/username/password），再手动 `new HikariConfig()` + 7 个 `setXxx()` 构建连接池。`application.yaml` 中已有 `clickhouse.datasource` 配置前缀。

ClickHouse 是第二数据源，必须手动注册 Bean（Spring Boot 只自动配置 `spring.datasource.*` 主数据源），但 DataSource 的组装无需手写——Spring Boot 提供了 `@ConfigurationProperties` + `DataSourceBuilder` 自动绑定。

## Goals / Non-Goals

**Goals:**
- 用 `@ConfigurationProperties("clickhouse.datasource")` 替代 `@Value` 注入，让 DataSource 由框架绑定
- 将硬编码的 HikariCP 连接池参数（maximum-pool-size 等）迁移到 yml 管理
- 保留 `@MapperScan` 和 `clickHouseSqlSessionFactory` Bean（第二数据源必须手动指定）

**Non-Goals:**
- 不改动 SQL SessionFactory、Mapper XML 位置
- 不改变 ClickHouse 连接行为或性能特征
- 不涉及 ShardingSphere/MySQL 主数据源

## Decisions

### 1. 使用 `@ConfigurationProperties` + `DataSourceBuilder` 替代手动 HikariConfig

**方案**：
```java
@Bean(name = "clickHouseDataSource")
@ConfigurationProperties("clickhouse.datasource")
public DataSource clickHouseDataSource() {
    return DataSourceBuilder.create().build();
}
```

**选择理由**：
- `DataSourceBuilder` 根据 `spring.datasource.type` 或 classpath 自动选择连接池（默认 HikariCP）
- 如果 yml 不配 `type`，Spring Boot 2.x/3.x 默认就是 HikariCP
- 3 个 `@Value` 字段 → 消除为 0
- pool 参数不再硬编码，dev/prod 可差异化

**备选方案**（不采用）：
- 保留 `@Value` 但将 pool 参数也作为 `@Value` 读取 → 换汤不换药，还是手写字段
- 删除整个 Java Config 类 → 不可行，第二数据源必须手动注册

### 2. 必须显式指定 driverClassName

**理由**：`DataSourceBuilder` 通过 `DatabaseDriver.fromJdbcUrl()` 匹配驱动，但 ClickHouse JDBC 驱动可能不在 Spring Boot 内置的驱动映射表中，需显式设置 `.driverClassName("com.clickhouse.jdbc.ClickHouseDriver")`，防止自动匹配失败回退到错误的驱动。

### 3. pool 参数使用 HikariCP 标准属性名

**YAML 中配置**：
```yaml
clickhouse:
  datasource:
    url: jdbc:clickhouse://...
    username: ...
    password: ...
    hikari:
      maximum-pool-size: 100
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: ClickHouseHikariPool
```

Spring Boot 的 `@ConfigurationProperties` 会自动将 `clickhouse.datasource.hikari.*` 绑定到 `HikariDataSource` 的属性。

## Risks / Trade-offs

- **[低风险] ClickHouse 驱动自动检测失败** → 已通过显式 `.driverClassName()` 缓解
- **[低风险] `@ConfigurationProperties` 绑定行为变化** → yml 中的 `hikari.*` 子属性需与 `HikariDataSource` 属性名精确匹配（Spring Boot 已内置支持）
