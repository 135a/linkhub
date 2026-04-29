## 1. 依赖配置 (Dependency Configuration)

- [ ] 1.1 在 `shortlink-main/main/pom.xml` 中，为 `org.apache.httpcomponents.client5:httpclient5` 显式指定版本（如 `5.3.1`），覆盖 Spring Boot 的默认版本管理。
- [ ] 1.2 重新加载 Maven 依赖，确保本地环境正确解析出对应版本的包，并验证 `ConnectionConfig` 类存在。

## 2. 验证与测试 (Verification and Testing)

- [ ] 2.1 启动服务，执行 `/api/short-link/admin/v1/user/login` 或其他涉及 ClickHouse 记录的方法。
- [ ] 2.2 验证应用日志中不再抛出 `java.lang.NoClassDefFoundError: org/apache/hc/client5/http/config/ConnectionConfig`。
- [ ] 2.3 验证 ClickHouse 数据库中正确写入或查询到了对应的数据，保证 HTTP 连接可用。
