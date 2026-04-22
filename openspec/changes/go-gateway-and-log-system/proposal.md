## Why

当前项目使用 Spring Cloud Gateway 作为网关，在短链接高并发场景下面临 JVM 内存开销大、启动慢、连接池瓶颈等问题。同时日志体系分散在各个服务中，缺乏统一的采集、聚合和查询能力。本变更用 Go 语言实现高性能网关替代 Spring Cloud Gateway，并构建统一日志收集系统，全面接入短链接项目，目标是降低延迟、提升吞吐量、统一可观测性。

## What Changes

- 用 Go 语言重写网关层，替代 Spring Cloud Gateway，保留现有路由规则和过滤逻辑
- 构建基于 Go 的日志收集系统，统一采集网关和后端服务的结构化日志
- 网关接入日志收集系统，实现请求链路追踪（traceID 贯穿网关 → 后端服务）
- 日志系统独立前端，包含简单登录注册功能，提供日志查询、检索、过滤界面
- 前端 Console Vue 页面新增网关性能监控大盘，可跳转至日志系统
- 更新部署架构（Docker Compose），以 Go 网关容器替换原 Spring Cloud Gateway 容器，新增日志系统服务

## Capabilities

### New Capabilities

- `go-gateway`: Go 语言实现的网关，包含路由转发、限流、鉴权、跨域等核心能力
- `log-collection`: 日志收集系统，包含日志采集、聚合、存储、查询接口
- `log-frontend`: 日志系统独立前端，含简单登录注册和日志查询界面
- `trace-correlation`: 全链路追踪，traceID 从网关透传到后端短链接服务
- `gateway-monitoring`: 网关性能监控（QPS、延迟、错误率、连接数）
- `console-gateway-monitor`: Console 前端网关监控大盘

### Modified Capabilities

- `shortlink-resolve`: 短链接服务需增加 traceID 透传适配和日志格式统一（接入新的日志收集系统）

## Impact

- **新增 Go 项目**: `short-link-gateway-go/` 目录，包含 Go 网关代码
- **新增 Go 项目**: `short-link-log-go/` 目录，包含日志收集 + ClickHouse 写入 + 日志前端
- **目录结构调整**: 项目顶层采用清晰分层架构：
  - `docker-compose.yml` — 一键启动所有服务
  - `short-link-java/` — Java 短链接核心微服务
  - `short-link-gateway-go/` — Go 网关
  - `short-link-log-go/` — Go 日志收集系统
- **依赖变更**: 新增 Go 生态依赖（gin/gorm/clickhouse-go 等）
- **部署变更**: 统一 `docker-compose.yml` 编排所有服务，替换原 Spring Cloud Gateway 容器
- **后端适配**: Java 短链接服务需添加 traceID 拦截器和统一日志格式（非破坏性，向后兼容）
- **前端变更**: `console-vue` 新增网关性能监控大盘和日志系统跳转入口
- **API 变更**: 网关对外 API 保持与 Spring Cloud Gateway 一致，对下游服务透明
