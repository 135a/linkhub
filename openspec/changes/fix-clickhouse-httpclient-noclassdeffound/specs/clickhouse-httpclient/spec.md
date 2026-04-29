## ADDED Requirements

### Requirement: 提供兼容的 ClickHouse HTTP 客户端依赖
系统必须提供包含 `ConnectionConfig` 类的兼容版本的 `org.apache.httpcomponents.client5:httpclient5` 依赖（如 5.3.x 版本），以保障 `clickhouse-jdbc` 正常建立 HTTP 连接。

#### Scenario: 应用程序初始化 ClickHouse 数据库连接并处理请求
- **WHEN** 应用程序启动，且用户触发任意涉及 ClickHouse 的写入或查询操作（如：登录、访问统计）
- **THEN** 底层 `ClickHouseHttpClient` 应成功初始化连接，不会抛出 `NoClassDefFoundError: org.apache.hc.client5.http.config.ConnectionConfig` 异常，且整个请求链路能够成功处理并响应。
