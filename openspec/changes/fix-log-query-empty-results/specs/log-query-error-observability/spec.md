## ADDED Requirements

### Requirement: ClickHouse Native TCP 端口配置

系统 SHALL 默认使用端口 `9000`（ClickHouse Native TCP 协议）连接 ClickHouse，与 `clickhouse-go/v2` 驱动的 `clickhouse.Open()` 方法匹配。端口值 MUST 可通过 `CLICKHOUSE_PORT` 环境变量覆盖。

#### Scenario: 默认端口连接 ClickHouse

- **WHEN** 未设置 `CLICKHOUSE_PORT` 环境变量
- **THEN** 系统使用端口 `9000` 连接 ClickHouse

#### Scenario: 自定义端口连接 ClickHouse

- **WHEN** 设置 `CLICKHOUSE_PORT=19000`
- **THEN** 系统使用端口 `19000` 连接 ClickHouse

### Requirement: 启动时 ClickHouse 连接验证

系统 SHALL 在启动阶段完成 ClickHouse Store 初始化后，立即执行 Ping 操作验证连接有效性。若 Ping 失败，系统 MUST 终止启动并输出包含错误详情的致命日志。

#### Scenario: ClickHouse 可达时正常启动

- **WHEN** 服务启动且 ClickHouse 在配置的地址和端口上可达
- **THEN** Ping 成功，服务继续正常启动

#### Scenario: ClickHouse 不可达时终止启动

- **WHEN** 服务启动且 ClickHouse 在配置的地址和端口上不可达
- **THEN** 服务输出致命日志（包含连接错误详情）并终止进程

### Requirement: 查询失败返回错误

当 ClickHouse 查询执行失败时（count 查询或数据查询），`Query()` 方法 MUST 返回 `error` 给调用方，而非静默返回空结果。调用方（handler 层）SHALL 返回 HTTP 500 状态码。

#### Scenario: count 查询失败

- **WHEN** 执行 `SELECT count() FROM logs` 查询失败（如连接断开、表不存在）
- **THEN** `Query()` 返回 error，handler 返回 HTTP 500 和 `{"error": "query failed"}`

#### Scenario: 数据查询失败

- **WHEN** count 查询成功但后续的数据查询 `SELECT ... FROM logs` 失败
- **THEN** `Query()` 返回 error，handler 返回 HTTP 500 和 `{"error": "query failed"}`

#### Scenario: 查询成功但无数据

- **WHEN** 查询成功执行且 ClickHouse 中确实没有匹配的日志数据
- **THEN** `Query()` 正常返回 `{total: 0, logs: [], page: 1}`

### Requirement: 查询失败记录错误日志

当 ClickHouse 查询失败时，系统 MUST 使用标准日志库记录错误信息，包含失败的 SQL 类型和错误详情。

#### Scenario: count 查询失败时记录日志

- **WHEN** count 查询执行失败
- **THEN** 系统记录包含 "count query failed" 和错误详情的日志

#### Scenario: 数据查询失败时记录日志

- **WHEN** 数据查询执行失败
- **THEN** 系统记录包含 "data query failed" 和错误详情的日志

### Requirement: 批量写入失败记录日志

当 `flush()` 方法中的 `PrepareBatch` 或 `Send` 操作失败时，系统 MUST 记录包含错误详情的警告日志。

#### Scenario: PrepareBatch 失败

- **WHEN** `PrepareBatch` 调用失败
- **THEN** 系统记录包含 "prepare batch failed" 和错误详情的日志

#### Scenario: Send 失败

- **WHEN** 批量数据 `Send` 调用失败
- **THEN** 系统记录包含 "batch send failed" 和错误详情的日志

### Requirement: Docker Compose 端口配置一致性

所有 Docker Compose 配置文件中的 `CLICKHOUSE_PORT` 环境变量 MUST 设置为 `9000`，与 Native TCP 协议匹配。

#### Scenario: docker-compose.yml 端口配置

- **WHEN** 使用 `docker-compose.yml` 部署 log-collector 服务
- **THEN** `CLICKHOUSE_PORT` 环境变量值为 `9000`
