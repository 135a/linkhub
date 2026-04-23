## 1. 配置更新 (Configuration Updates)

- [x] 1.1 修改 `short-link-log-go/internal/config/config.go`，将 `CLICKHOUSE_PORT` 默认值从 `8123` 改为 `9000`
- [x] 1.2 修改 `docker-compose.yml`，将 `log-collector` 服务的 `CLICKHOUSE_PORT` 环境变量改为 `9000`
- [x] 1.3 检查并更新 `docker-compose.dev.yml` 和 `docker-compose.prod.yml` 中的 `CLICKHOUSE_PORT` 配置（如果存在）

## 2. 存储层错误处理增强 (Store Error Handling)

- [x] 2.1 修改 `short-link-log-go/internal/store/clickhouse.go` 中的 `Query` 方法，在 count 查询失败时记录日志并返回 error
- [x] 2.2 修改 `short-link-log-go/internal/store/clickhouse.go` 中的 `Query` 方法，在数据查询失败时记录日志并返回 error
- [x] 2.3 修改 `short-link-log-go/internal/store/clickhouse.go` 中的 `flush` 方法，在 `PrepareBatch` 失败时使用 `log.Printf` 记录错误
- [x] 2.4 修改 `short-link-log-go/internal/store/clickhouse.go` 中的 `flush` 方法，在 `Send` 失败时使用 `log.Printf` 记录错误

## 3. 启动校验 (Startup Validation)

- [x] 3.1 修改 `short-link-log-go/main.go`，在初始化 `chStore` 后调用 `Ping()` 方法
- [x] 3.2 若 `Ping()` 失败，使用 `log.Fatalf` 打印错误详情并终止服务启动

## 4. 验证与测试 (Verification)

- [ ] 4.1 重新构建并启动服务，验证 `log-collector` 能够成功连接 ClickHouse
- [ ] 4.2 验证日志查询页面在无筛选条件下能正确返回数据
- [ ] 4.3 模拟 ClickHouse 断开连接，验证服务启动失败并输出致命日志
- [ ] 4.4 模拟查询失败，验证 API 返回 HTTP 500 错误码且控制台有相关错误日志

