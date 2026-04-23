## Context

日志系统 (`short-link-log-go`) 使用 `clickhouse-go/v2` (v2.17.0) 通过 `clickhouse.Open()` 连接 ClickHouse，该方法使用 **Native TCP 协议**。然而，配置文件和 Docker Compose 均将端口设为 `8123`，这是 ClickHouse 的 **HTTP 接口端口**，而 Native TCP 协议需要端口 `9000`。

当前 `store/clickhouse.go` 中的 `Query()` 方法在 count 查询失败时（L126），直接返回空结果 `{total:0, logs:[], page:1}` 而没有记录任何日志。`flush()` 方法在 `PrepareBatch` 或 `Send` 失败时也是静默返回。这导致端口配置错误时，系统表面上"正常运行"但实际上：
- 所有日志写入被静默丢弃
- 所有查询返回空结果
- 无任何告警或错误日志

## Goals / Non-Goals

**Goals:**
- 修复 ClickHouse 端口配置，使日志写入和查询恢复正常
- 增强关键路径的错误可观测性，防止类似问题被掩盖
- 服务启动时尽早检测并暴露数据存储连接问题

**Non-Goals:**
- 不切换 ClickHouse 驱动协议（不从 Native TCP 改为 HTTP）
- 不重构日志写入的异步批处理架构
- 不修改前端逻辑或 API 接口定义
- 不实现日志写入失败的重试机制（保持当前 fire-and-forget 设计）

## Decisions

### 1. 修复端口而非切换协议

**决定**：将端口配置从 `8123` 修正为 `9000`，保持使用 Native TCP 协议。

**替代方案**：切换为 HTTP 协议（使用 `clickhouse.Open()` 的 HTTP 模式或 `database/sql` 接口），端口保持 `8123`。

**理由**：Native TCP 协议性能更优（二进制协议，支持压缩），且代码已基于 Native 接口编写（`PrepareBatch`, `Append`, `Send`），改端口的变更范围远小于切换协议。

### 2. Query 失败时返回错误而非空结果

**决定**：`Query()` 中 count 查询失败时，记录错误日志并返回 `error` 给调用方，而非静默返回空结果。

**替代方案**：保持返回空结果但添加日志。

**理由**：返回空结果会让前端认为"没有数据"而非"查询出错"，用户无法区分。让 handler 层返回 500 错误码更合理，前端已有对应的 `查询失败` 错误提示处理。同时添加 `log.Printf` 记录错误详情方便排查。

### 3. flush 失败时记录日志而非静默丢弃

**决定**：`flush()` 中 `PrepareBatch` 和 `Send` 失败时，使用 `log.Printf` 记录错误。

**替代方案**：引入错误 channel 或 metric 计数器。

**理由**：日志收集服务本身是 fire-and-forget 设计（buffer 满时也是直接丢弃），不适合引入复杂的错误处理链路。简单日志记录足以发现问题，后续有需要再引入 metrics。

### 4. 启动时 Ping 验证 ClickHouse 连接

**决定**：在 `main.go` 中 `NewClickHouseStore` 成功后，调用 `chStore.Ping()` 验证连接有效性。失败时 `log.Fatalf` 终止启动。

**替代方案**：延迟到第一次写入/查询时再验证。

**理由**：快速失败（fail-fast）能在部署阶段立即暴露配置问题，避免服务"假活"。`HealthHandler` 已有 `Ping()` 逻辑但仅在请求 `/health` 时触发，启动时主动验证更可靠。

## Risks / Trade-offs

- **[端口硬编码风险]** → 通过环境变量覆盖机制缓解。`config.go` 中的 `9000` 只是默认值，Docker Compose 和生产环境可通过 `CLICKHOUSE_PORT` 环境变量自定义。
- **[Ping 阻塞启动]** → ClickHouse 容器可能比 log-collector 启动慢，Ping 失败会导致 crash loop。缓解方式：Docker Compose 已有 `depends_on` 依赖关系，加上 `restart: unless-stopped` 策略，服务会自动重试直到 ClickHouse 就绪。
- **[Query 返回 error 的兼容性]** → Handler 层已正确处理 error 情况（返回 500），前端也有 `查询失败` 的 catch 处理，无兼容性问题。
- **[flush 日志量]** → 如果 ClickHouse 长时间不可用，flush 每 2 秒触发一次日志。在日志收集服务中这是可接受的行为，因为本身就意味着数据丢失中。
