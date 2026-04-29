## 为什么 (Why)

系统在执行 API（如登录接口）并初始化 ClickHouse HTTP 连接时，抛出了 `java.lang.NoClassDefFoundError: org/apache/hc/client5/http/config/ConnectionConfig` 异常。原因是 `clickhouse-jdbc` 0.6.3 版本与 Spring Boot 依赖管理的 Apache HttpClient 5 (`httpclient5`) 版本不兼容，导致缺少 `ConnectionConfig` 类。这使得依赖 ClickHouse 的核心功能（如数据写入、统计等）无法正常运行。

## 变更内容 (What Changes)

- 在 `shortlink-main/main/pom.xml` 中显式指定一个与 `clickhouse-jdbc` 0.6.3 兼容的 `org.apache.httpcomponents.client5:httpclient5` 版本（通常是 5.2.x 或更高版本），以提供所需的 `ConnectionConfig` 类。
- 验证应用启动情况，确保 ClickHouse HTTP 连接成功建立，且接口调用不再抛出该异常。

## 能力 (Capabilities)

### 新增能力 (New Capabilities)
- `clickhouse-httpclient`: 解决 HTTP 客户端依赖冲突并建立稳定的 ClickHouse 连接。

### 修改的能力 (Modified Capabilities)


## 影响范围 (Impact)

- `shortlink-main/main/pom.xml`：关于 `httpclient5` 的依赖版本配置。
- 所有通过 HTTP 协议连接 ClickHouse 的数据库操作将恢复正常。
