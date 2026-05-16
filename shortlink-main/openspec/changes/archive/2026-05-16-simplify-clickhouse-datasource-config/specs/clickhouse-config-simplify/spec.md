## ADDED Requirements

### Requirement: ClickHouse 数据源通过 ConfigurationProperties 绑定
ClickHouse 数据源 Bean SHALL 使用 `@ConfigurationProperties("clickhouse.datasource")` + `DataSourceBuilder` 自动绑定 yml 配置，SHALL NOT 使用 `@Value` 手动注入各属性再拼装。

#### Scenario: 数据源从 yml 自动创建
- **WHEN** `application.yaml` 中配置了 `clickhouse.datasource.jdbc-url`, `clickhouse.datasource.username`, `clickhouse.datasource.password`
- **THEN** 系统 SHALL 自动创建名为 `clickHouseDataSource` 的 DataSource Bean，其 JDBC URL、用户名、密码与 yml 一致

#### Scenario: 缺少密码时正常运行
- **WHEN** `application.yaml` 中 `clickhouse.datasource.password` 为空或未配置（ClickHouse 本地开发无需密码）
- **THEN** 系统 SHALL 正常创建 DataSource，连接 ClickHouse 无需密码认证

### Requirement: ClickHouse 连接池参数由 yml 管理
ClickHouse HikariCP 连接池参数（maximum-pool-size、minimum-idle、connection-timeout、idle-timeout、max-lifetime、pool-name）SHALL 在 `application.yaml` 的 `clickhouse.datasource.*` 下以扁平结构定义，SHALL NOT 在 Java 代码中硬编码。

#### Scenario: 连接池参数从 yml 读取
- **WHEN** `application.yaml` 中配置了 `clickhouse.datasource.maximum-pool-size: 100`
- **THEN** ClickHouse 数据源连接池最大连接数 SHALL 为 100

#### Scenario: 未配置连接池参数时使用 HikariCP 默认值
- **WHEN** `application.yaml` 中未配置 ClickHouse 数据源的任何 HikariCP 连接池参数
- **THEN** ClickHouse 数据源 SHALL 使用 HikariCP 内建默认值（maximum-pool-size=10 等）
