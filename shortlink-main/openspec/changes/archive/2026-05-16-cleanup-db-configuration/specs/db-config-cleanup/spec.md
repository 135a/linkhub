## ADDED Requirements

### Requirement: YAML MyBatis-Plus 配置生效
系统 SHALL 使用 `application.yaml` 中 `mybatis-plus.configuration.log-impl` 配置作为 MyBatis SQL 日志实现，该配置 SHALL 覆盖框架默认值。

#### Scenario: NoLoggingImpl 生效
- **WHEN** `application.yaml` 中配置 `mybatis-plus.configuration.log-impl: NoLoggingImpl`
- **THEN** SQL 语句 SHALL NOT 输出到 stdout

#### Scenario: StdOutImpl 切换
- **WHEN** `application.yaml` 中配置 `mybatis-plus.configuration.log-impl: StdOutImpl`
- **THEN** SQL 语句 SHALL 输出到 stdout

### Requirement: 无冗余 DataBaseConfiguration
系统 SHALL NOT 存在手动定义的 Bean 重复 Spring Boot / MyBatis-Plus 自动配置的功能。DataSource、SqlSessionFactory、分页插件 SHALL 由框架自动配置创建。

#### Scenario: DataSource 自动创建
- **WHEN** 应用启动且 `spring.datasource.url` 指向 ShardingSphere 配置
- **THEN** 系统 SHALL 自动创建 ShardingSphere DataSource Bean

#### Scenario: SqlSessionFactory 自动创建
- **WHEN** 应用启动且 classpath 下存在 MyBatis-Plus
- **THEN** 系统 SHALL 自动创建 SqlSessionFactory，读取 YAML 中的 `mybatis-plus` 配置

#### Scenario: 分页插件自动创建
- **WHEN** 应用启动且 classpath 下存在 MyBatis-Plus
- **THEN** 系统 SHALL 自动配置分页拦截器，支持 MySQL 分页查询
