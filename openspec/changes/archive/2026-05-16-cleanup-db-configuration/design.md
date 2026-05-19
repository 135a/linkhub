## Context

当前 `DataBaseConfiguration` 类手动定义了三个 Bean，均与 Spring Boot + MyBatis-Plus 自动配置功能完全重复：

- `primaryDataSource` → Spring Boot `DataSourceAutoConfiguration` 自动创建
- `sqlSessionFactory` → MyBatis-Plus `MybatisPlusAutoConfiguration` 自动创建
- `mybatisPlusInterceptor` → MyBatis-Plus 自动配置分页插件

其中 `sqlSessionFactory` 硬编码了 `StdOutImpl.class`，导致 `application.yaml` 中的 `mybatis-plus.configuration.log-impl: NoLoggingImpl` 永不生效。

项目中另一个数据源配置 `ClickHouseDataSourceConfig` 使用独立 `@Qualifier`，不受影响。

## Goals / Non-Goals

**Goals:**
- 删除 `DataBaseConfiguration` 类，让框架自动配置接管
- 确保 YAML 中 `mybatis-plus.configuration.log-impl` 配置实际生效
- 保持所有现有功能不受影响（分页、ShardingSphere 数据源、Mapper 扫描）

**Non-Goals:**
- 不修改 ShardingSphere 配置（`shardingsphere-config-{dev|prod}.yaml`）
- 不修改 ClickHouse 数据源配置
- 不修改 `application.yaml` 中的 mybatis-plus 配置

## Decisions

### 决策 1：完全删除 DataBaseConfiguration

**选择：** 删除整个 `DataBaseConfiguration.java` 文件。

**理由：**
- `primaryDataSource`：Spring Boot 自动从 `spring.datasource.driver-class-name`（ShardingSphereDriver）和 `spring.datasource.url` 创建 DataSource，无需手动干预
- `mybatisPlusInterceptor`：MyBatis-Plus 的 `MybatisPlusAutoConfiguration` 自动配置分页拦截器，只需在 YAML 中保留配置
- `sqlSessionFactory`：MyBatis-Plus 自动创建 SqlSessionFactory，读取 YAML 中的 `mapper-locations` 和 `log-impl` 配置

**备选方案：** 只修改 `sqlSessionFactory` 方法，去掉硬编码的 `StdOutImpl`，改为读取 YAML 配置。但这个方案复杂度更高且保留了冗余代码，不推荐。

### 决策 2：YAML 配置完整性验证

验证 `application.yaml` 中已有的 mybatis-plus 配置是否满足自动配置需求：

```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
  mapper-locations: classpath:mapper/*.xml
```

`mapper-locations` 由 `MybatisPlusProperties` 自动读取并设置，`log-impl` 自动应用到 MyBatis Configuration。无需额外配置。

## Risks / Trade-offs

- **[低风险] SQL 日志行为变更**：从 stdout 始终输出变为不输出（NoLoggingImpl）。这是预期的正确行为，YAML 注释明确写明是"性能优化"。如果开发时需要查看 SQL，可以在 YAML 中临时改为 `StdOutImpl`。
  → 缓解：YAML 中已有注释说明，开发环境可自行切换

- **[低风险] 测试兼容性**：测试使用 H2 内存数据库 + `application-test.yaml`（禁用了 ShardingSphere 等自动配置），删除 `DataBaseConfiguration` 不影响测试环境的模拟 Bean。
  → 缓解：运行全部测试验证
