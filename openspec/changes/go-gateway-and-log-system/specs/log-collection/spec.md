## ADDED Requirements

### Requirement: 日志接收 API
日志收集系统 SHALL 提供 HTTP POST 端点 `/api/v1/logs/ingest`，接收来自网关和下游服务的结构化 JSON 日志。

#### Scenario: 单条日志入库
- **WHEN** POST 请求体包含单条 JSON 日志（含 level、timestamp、trace_id、service、message 字段）
- **THEN** 系统返回 202 Accepted

#### Scenario: 批量日志入库
- **WHEN** POST 请求体包含 JSON 数组形式的批量日志（最多 1000 条/批次）
- **THEN** 系统返回 202 Accepted 并全部入库

#### Scenario: 无效日志格式拒绝
- **WHEN** 请求体缺少必填字段（level 或 timestamp 或 message）
- **THEN** 返回 400 状态码和错误描述

### Requirement: 日志查询 API
日志收集系统 SHALL 提供 GET `/api/v1/logs/query` 端点，支持按时间范围、服务名、日志级别、关键词、TraceID 过滤查询。

#### Scenario: 按时间范围查询
- **WHEN** 请求包含 start_time 和 end_time 参数
- **THEN** 返回该时间范围内的日志，按时间倒序排列

#### Scenario: 按 TraceID 查询
- **WHEN** 请求包含 trace_id 参数
- **THEN** 返回该 TraceID 关联的所有日志记录

#### Scenario: 组合条件查询
- **WHEN** 请求同时包含 service、level、keyword 参数
- **THEN** 返回满足所有条件的日志

#### Scenario: 分页查询
- **WHEN** 请求包含 page 和 page_size 参数
- **THEN** 返回对应页的日志数据，最多 100 条/页

### Requirement: ClickHouse 存储
日志数据 SHALL 写入 ClickHouse 表，按天分区，保留策略可配置（默认 30 天）。

#### Scenario: 日志写入
- **WHEN** 新日志数据到达
- **THEN** 日志被写入 ClickHouse 的 `logs` 表，按当天日期分区

#### Scenario: 过期数据清理
- **WHEN** 日志数据超过配置的保留天数
- **THEN** 系统自动删除过期分区

### Requirement: 日志格式校验
日志收集系统 SHALL 对入库的日志执行格式校验，确保符合统一的 JSON Schema。

#### Scenario: 标准格式通过
- **WHEN** 日志包含 level (INFO/WARN/ERROR/DEBUG)、timestamp (ISO 8601)、message (string)
- **THEN** 日志通过校验并入库

#### Scenario: 非法级别拒绝
- **WHEN** 日志 level 字段不是 INFO/WARN/ERROR/DEBUG 之一
- **THEN** 返回 400 拒绝入库

### Requirement: 日志收集服务健康检查
日志收集系统 SHALL 暴露 `/health` 端点，返回自身状态和 ClickHouse 连接状态。

#### Scenario: 全部健康
- **WHEN** GET `/health` 且 ClickHouse 连接正常
- **THEN** 返回 200 `{"status": "ok", "clickhouse": "connected"}`

#### Scenario: ClickHouse 不可用
- **WHEN** GET `/health` 且 ClickHouse 连接失败
- **THEN** 返回 503 `{"status": "degraded", "clickhouse": "disconnected"}`
