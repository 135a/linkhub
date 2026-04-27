# Capability: User Flow Risk Control

## Purpose
用户请求流量限制与风控管理机制。规定用户维度的流量访问限制、限流计数的判定机制，以及前后端针对流控超限（限流拦截）情况的交互响应规范。

## ADDED Requirements

### Requirement: 准确获取用户标识
系统 MUST 在风控拦截和日志记录时准确获取触发操作的用户名。当通过 `UserContext` 获取失败时，系统 MUST 回退尝试从 HTTP Request Header 的 `username` 属性中提取该用户名，若仍未提取到则默认使用 "other"。

#### Scenario: 业务请求缺少完整身份但携带用户名 Header
- **WHEN** 由于缺少 `userId` 导致系统未构造完整的 `UserContext`（上下文中的 username 为 null），但请求头中附带了 `username` 时
- **THEN** 流量风控过滤器 MUST 提取请求头中的 `username` 并以此作为该次请求的限流维度与日志标记。

### Requirement: 固定的时间窗口流量计数
系统 MUST 使用完整的 Redis Lua 脚本准确统计用户在配置的时间窗口内（Time Window）发起的请求次数，以避免脚本缺失或错误导致计数值始终为 null 而产生的误拦截。

#### Scenario: 窗口期内用户的首次请求
- **WHEN** 用户在未被计数的周期内发送第一次请求触发计数值变更为 1 时
- **THEN** 系统 MUST 对该限流 Key 执行过期时间（EXPIRE）设置，过期时长等于系统配置的时间窗口。

#### Scenario: 窗口期内的后续访问
- **WHEN** 用户在尚未过期的时间窗口内发起第 N（N>1）次请求时
- **THEN** 系统 MUST 仅自增计数值，并正确返回当前计数值用于限流阈值比对。

### Requirement: 前端全局风控错误反馈
前端应用 MUST 在网络请求响应拦截器中，对所有业务处理失败（例如 `success` 为 `false`）的情况进行拦截，并通过 UI 组件展示全局错误消息。

#### Scenario: 触发后端流量控制限制
- **WHEN** 前端应用接收到后端返回的错误负载，其中 `success` 为 `false` 且 `code` 对应流量限制错误（如 `A000300`）时
- **THEN** 前端 axios 响应拦截器 MUST 提取负载中的 `message` 字段，并在界面上触发明显的错误提示弹窗（如 "当前访问网站人数过多，请稍后再试"）。
