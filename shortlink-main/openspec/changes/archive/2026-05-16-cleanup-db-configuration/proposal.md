## Why

`DataBaseConfiguration` 手动定义了 `primaryDataSource`、`sqlSessionFactory`、`mybatisPlusInterceptor` 三个 Bean，全部重复了 Spring Boot + MyBatis-Plus 的自动配置。更严重的是 `sqlSessionFactory` 中硬编码了 `StdOutImpl.class`，导致 `application.yaml` 中 `mybatis-plus.configuration.log-impl: NoLoggingImpl`（生产性能优化配置）被静默覆盖，从不生效。

## What Changes

- 删除 `DataBaseConfiguration` 中手动创建的 `primaryDataSource` Bean（Spring Boot `DataSourceAutoConfiguration` 自动从 `spring.datasource.*` 创建 ShardingSphere DataSource）
- 删除 `DataBaseConfiguration` 中手动创建的 `sqlSessionFactory` Bean（MyBatis-Plus `MybatisPlusAutoConfiguration` 自动配置，正确读取 YAML 中的 `log-impl`）
- 删除 `DataBaseConfiguration` 中手动创建的 `mybatisPlusInterceptor` Bean（MyBatis-Plus 自动配置分页插件）
- 如果类变空则删除整个 `DataBaseConfiguration.java`
- YAML 中的 `mybatis-plus.configuration.log-impl: NoLoggingImpl` 开始生效，高并发压测时不再有 stdout SQL 日志瓶颈

## Capabilities

### New Capabilities

- `db-config-cleanup`: 移除冗余数据库配置，确保 YAML 中 MyBatis-Plus 配置正确生效

### Modified Capabilities

<!-- 无现有 spec 受影响 -->

## Impact

- 受影响文件：`main/src/main/java/com/nym/shortlink/core/config/DataBaseConfiguration.java`（可能删除）
- 依赖：`ClickHouseDataSourceConfig.java` 中引用了 `MybatisSqlSessionFactoryBean`（来自 mybatis-plus 包，不受影响）
- 行为变更：SQL 日志输出从 stdout（始终开启）变为不输出（YAML 配置的 NoLoggingImpl 生效）
- 测试：生产配置无变化，现有测试应全部通过
