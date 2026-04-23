## Why

日志查询页面在没有设置任何筛选条件时返回 `{"total":0,"logs":[],"page":1}`，但所有服务都在正常运行并产生日志。根本原因是 **ClickHouse 连接端口与驱动协议不匹配**：`clickhouse-go/v2` 使用 Native TCP 协议（需要端口 9000），但配置中使用的是 HTTP 接口端口（8123），导致查询失败后被静默吞掉，返回空结果。

## What Changes

- **修复 ClickHouse 端口配置**：将默认端口从 `8123`（HTTP 协议）改为 `9000`（Native TCP 协议），与 `clickhouse-go/v2` 驱动的 `clickhouse.Open()` 匹配
- **修复 Docker Compose 环境变量**：将 `CLICKHOUSE_PORT=8123` 改为 `CLICKHOUSE_PORT=9000`
- **增强错误处理可观测性**：在 ClickHouse 查询失败时记录错误日志而非静默返回空结果，防止类似问题被掩盖
- **增加启动时连接验证**：在服务启动时 Ping ClickHouse 验证连接有效性，快速暴露配置问题

## Capabilities

### New Capabilities

- `log-query-error-observability`: 覆盖日志查询链路的错误处理与可观测性——query 失败时记录错误日志、flush 失败时记录告警、启动时验证 ClickHouse 连接有效性

### Modified Capabilities

_(无现有 spec 需要修改)_

## Impact

- **代码影响**：
  - `short-link-log-go/internal/config/config.go` — 默认端口值修改
  - `short-link-log-go/internal/store/clickhouse.go` — 错误处理增强（Query、flush 方法）
  - `short-link-log-go/main.go` — 增加启动时连接验证
- **部署配置影响**：
  - `docker-compose.yml` — `CLICKHOUSE_PORT` 环境变量修改
  - `docker-compose.dev.yml` / `docker-compose.prod.yml` — 若存在覆盖配置需同步修改
- **无 Breaking Change**：修改仅涉及内部端口配置和错误日志，不影响外部 API 行为
