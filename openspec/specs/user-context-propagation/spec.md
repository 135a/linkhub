# Spec: user-context-propagation

## Requirements

### Requirement: 请求上下文身份解析
系统 SHALL 能够从前端请求头中提取用户信息（如 `username`）并建立内部用户上下文（`UserContext`）。

#### Scenario: 成功解析用户名并建立上下文
- **WHEN** 前端请求携带 `username` 请求头
- **THEN** 系统后端应能解析该请求头并将其设置到 `UserContext` 中，供后续分片查询逻辑使用
