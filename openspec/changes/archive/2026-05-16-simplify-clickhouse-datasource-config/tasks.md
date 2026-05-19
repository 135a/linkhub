## 1. YML 配置

- [x] 1.1 在 `application.yaml` 的 `clickhouse.datasource` 下新增 pool 参数（原硬编码的 6 个 HikariCP 参数改为直接绑定方式），并增加 `driver-class-name`

## 2. Java Config 简化

- [x] 2.1 重写 `ClickHouseDataSourceConfig.java`：删除 3 个 `@Value` 字段，用 `@ConfigurationProperties("clickhouse.datasource")` + `DataSourceBuilder` 创建 DataSource Bean，保留 `@MapperScan` 和 `clickHouseSqlSessionFactory` Bean

## 3. 验证

- [x] 3.1 执行 `mvn compile -pl main` 确认编译通过
- [x] 3.2 执行 `mvn test -pl main` 确认全部 102 个测试通过
