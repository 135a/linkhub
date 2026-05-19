## ADDED Requirements

### Requirement: 短链接创建 API 测试
系统 SHALL 验证 `POST /api/short-link/v1/create` 的请求响应。

#### Scenario: 正常创建
- **WHEN** 发送有效的创建请求 JSON
- **THEN** HTTP 状态码 200
- **AND** 响应体 success=true，data 包含 fullShortUrl

#### Scenario: 参数校验失败
- **WHEN** 缺少必填字段 originUrl
- **THEN** HTTP 状态码 400
- **AND** 响应体包含参数校验错误信息

#### Scenario: 未登录调用创建
- **WHEN** 请求不携带 token cookie/header
- **THEN** HTTP 状态码 401 或返回未登录错误码

### Requirement: 短链接跳转 API 测试
系统 SHALL 验证 `GET /{shortUri}` 重定向行为。

#### Scenario: 有效短链接跳转
- **WHEN** 访问存在的短链接 URI
- **THEN** HTTP 状态码 302
- **AND** Location header 指向原始 URL

#### Scenario: 不存在的短链接
- **WHEN** 访问不存在的短链接 URI
- **THEN** 302 重定向到 `/page/notfound`

### Requirement: 短链接分页查询 API 测试
系统 SHALL 验证 `GET /api/short-link/v1/page` 的分页查询。

#### Scenario: 正常分页
- **WHEN** 传入 gid、current、size 参数
- **THEN** 返回 200，data.records 为短链接列表
- **AND** data.total 为总条数

#### Scenario: gid 为空
- **WHEN** 不传 gid 参数
- **THEN** 返回 200（业务层面的错误码），消息提示 gid 不能为空

### Requirement: 短链接更新 API 测试
系统 SHALL 验证 `POST /api/short-link/v1/update` 的请求响应。

#### Scenario: 正常更新
- **WHEN** 发送有效更新请求
- **THEN** 返回 200，success=true

#### Scenario: 更新不存在的记录
- **WHEN** 更新一个不存在的 fullShortUrl
- **THEN** 返回错误，提示记录不存在

### Requirement: 回收站 API 测试
系统 SHALL 验证回收站相关接口。

#### Scenario: 移入回收站
- **WHEN** 发送删除请求到回收站接口
- **THEN** 返回 200 success=true

#### Scenario: 从回收站恢复
- **WHEN** 发送恢复请求
- **THEN** 返回 200 success=true

### Requirement: 分组管理 API 测试
系统 SHALL 验证分组管理相关接口。

#### Scenario: 查询用户分组列表
- **WHEN** 已登录用户查询分组
- **THEN** 返回 200，data 为分组列表

### Requirement: 用户注册登录 API 测试
系统 SHALL 验证用户认证相关接口。

#### Scenario: 用户注册
- **WHEN** 发送有效的用户名、密码、手机号
- **THEN** 返回 200，success=true

#### Scenario: 用户名已存在
- **WHEN** 注册已存在的用户名
- **THEN** 返回错误，提示用户名已存在

#### Scenario: 用户登录
- **WHEN** 发送正确的用户名和密码
- **THEN** 返回 200，设置 token cookie
- **AND** data 包含脱敏后的用户信息

#### Scenario: 登录失败
- **WHEN** 发送错误的密码
- **THEN** 返回 401 或错误码

### Requirement: 统计查询 API 测试
系统 SHALL 验证统计数据查询接口。

#### Scenario: 查询单条短链接统计
- **WHEN** 传入 fullShortUrl 和 gid
- **THEN** 返回 200，data 包含 PV/UV/UIP 和浏览器/操作系统分布
