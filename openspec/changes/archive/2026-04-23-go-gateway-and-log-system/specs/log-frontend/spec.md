## ADDED Requirements

### Requirement: 用户注册
日志前端 SHALL 提供用户注册功能，用户通过用户名和密码注册，密码以 bcrypt 加密存储至 MySQL。

#### Scenario: 成功注册
- **WHEN** 用户提供不重复的用户名和符合复杂度要求（至少 6 位）的密码
- **THEN** 系统创建用户账户并返回注册成功

#### Scenario: 重复用户名拒绝
- **WHEN** 用户尝试使用已存在的用户名注册
- **THEN** 系统返回 409 冲突错误

#### Scenario: 弱密码拒绝
- **WHEN** 用户注册密码少于 6 位
- **THEN** 系统返回 400 并提示密码强度不足

### Requirement: 用户登录
日志前端 SHALL 提供用户登录功能，登录成功后返回 JWT Token 用于后续请求鉴权。

#### Scenario: 成功登录
- **WHEN** 用户提供正确的用户名和密码
- **THEN** 系统返回 JWT Token（有效期 24 小时）

#### Scenario: 密码错误拒绝
- **WHEN** 用户提供错误的密码
- **THEN** 系统返回 401 未授权

#### Scenario: 暴力登录防护
- **WHEN** 同一 IP 在 5 分钟内连续失败登录超过 5 次
- **THEN** 系统临时锁定该 IP 的登录能力 10 分钟

### Requirement: 日志查询界面
日志前端 SHALL 提供 Web 界面，支持日志检索、按条件过滤和时间范围选择。

#### Scenario: 全量日志列表
- **WHEN** 用户登录后访问日志页面且未设置任何过滤条件
- **THEN** 展示最近 1 小时的日志列表，按时间倒序

#### Scenario: 关键词搜索
- **WHEN** 用户在搜索框输入关键词并执行搜索
- **THEN** 界面展示包含该关键词的日志记录

#### Scenario: 多条件过滤
- **WHEN** 用户同时设置了时间范围、服务名、日志级别的过滤条件
- **THEN** 界面展示满足所有条件的日志

#### Scenario: TraceID 链路追踪视图
- **WHEN** 用户输入 TraceID 并点击追踪
- **THEN** 界面展示该 TraceID 关联的所有日志，按时间线排列

### Requirement: 认证保护
日志前端 SHALL 对所有 API 请求附加 JWT Token，未认证请求重定向到登录页。

#### Scenario: 未认证访问拦截
- **WHEN** 未登录用户访问日志查询页面
- **THEN** 重定向到登录页面

#### Scenario: Token 过期处理
- **WHEN** 用户的 JWT Token 过期后发起 API 请求
- **THEN** 系统返回 401，前端自动跳转登录页

### Requirement: 前端技术栈
日志前端 SHALL 使用 Vue 3 + Vite + Element Plus 构建，独立部署。

#### Scenario: 生产构建
- **WHEN** 执行 `npm run build`
- **THEN** 生成可部署的静态文件到 dist/ 目录
