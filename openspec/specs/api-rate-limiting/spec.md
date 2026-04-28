# Capability: API Rate Limiting

## Purpose

基于 Alibaba Sentinel 对系统所有 Controller 接口实施 QPS 限流保护，通过自定义注解 `@RateLimit` 声明式配置资源名、QPS 阈值、限流算法和提示信息，由 AOP 切面动态注册规则并拦截超限请求，返回统一 HTTP 429 响应。

## Requirements

### Requirement: 自定义限流注解
系统 SHALL 提供 `@RateLimit` 注解，可标注在 Controller 方法上，声明 Sentinel 资源名（`resource`）、QPS 阈值（`qps`，默认 10）、限流算法（`controlBehavior`，默认快速失败）、漏桶等待时长（`maxQueueingTimeMs`，默认 500ms）及限流提示信息（`message`，默认"当前网站访问人数过多,请耐心等待"）。

#### Scenario: 注解声明资源名和 QPS
- **WHEN** 开发者在 Controller 方法上添加 `@RateLimit(resource = "create_short-link", qps = 1)`
- **THEN** 系统 SHALL 将该方法识别为受限流保护的资源，资源名为 `create_short-link`，QPS 上限为 1

#### Scenario: 注解使用默认 QPS
- **WHEN** 开发者仅指定 `resource` 而未指定 `qps`
- **THEN** 系统 SHALL 使用默认 QPS 值 10 作为该资源的流量阈值

#### Scenario: 注解指定漏桶算法
- **WHEN** 开发者设置 `controlBehavior = RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER`
- **THEN** 系统 SHALL 对超限请求进行匀速排队处理，等待时间不超过 `maxQueueingTimeMs`

#### Scenario: 注解指定自定义提示信息
- **WHEN** 开发者设置 `message = "创建请求过于频繁，请稍后再试"`
- **THEN** 限流触发时响应体中的 `message` 字段 SHALL 使用该自定义文本

---

### Requirement: 限流规则动态注册
系统 SHALL 通过 AOP 切面在带有 `@RateLimit` 注解的方法首次被调用时，自动向 Sentinel `FlowRuleManager` 追加注册对应的 QPS 限流规则，无需在启动配置类中手动声明。

#### Scenario: 首次请求触发规则注册
- **WHEN** 某个带 `@RateLimit` 注解的接口收到第一次请求
- **THEN** 系统 SHALL 向 Sentinel 注册该资源的 FlowRule（`FLOW_GRADE_QPS`，count = 注解 qps 值），并允许本次请求正常通过

#### Scenario: 规则不重复注册
- **WHEN** 同一资源的接口被多次调用
- **THEN** 系统 SHALL 只注册一次规则，后续调用跳过注册步骤，直接进入限流判断

---

### Requirement: 所有接口限流覆盖
系统 SHALL 为以下所有 Controller 方法添加 `@RateLimit` 注解，各接口资源名和 QPS 阈值如下表：

| 接口描述 | HTTP 路由 | 资源名 | QPS | 算法 |
|---|---|---|---|---|
| 创建短链接 | `POST /api/short-link/admin/v1/create` | `create_short-link` | 1 | 漏桶 |
| 批量创建短链接 | `POST /api/short-link/admin/v1/create/batch` | `batch-create_short-link` | 1 | 漏桶 |
| 修改短链接 | `POST /api/short-link/admin/v1/update` | `update_short-link` | 5 | 快速失败 |
| 分页查询短链接 | `GET /api/short-link/admin/v1/page` | `page_short-link` | 20 | 快速失败 |
| 短链接跳转 | `GET /{shortUri}` | `redirect_short-link` | 100 | 快速失败 |
| 新增分组 | `POST /api/short-link/admin/v1/group` | `save_group` | 5 | 快速失败 |
| 查询分组列表 | `GET /api/short-link/admin/v1/group` | `list_group` | 20 | 快速失败 |
| 修改分组名称 | `PUT /api/short-link/admin/v1/group` | `update_group` | 5 | 快速失败 |
| 删除分组 | `DELETE /api/short-link/admin/v1/group` | `delete_group` | 5 | 快速失败 |
| 排序分组 | `POST /api/short-link/admin/v1/group/sort` | `sort_group` | 10 | 快速失败 |
| 用户注册 | `POST /api/short-link/admin/v1/user` | `user_register` | 1 | 快速失败 |
| 用户登录 | `POST /api/short-link/admin/v1/user/login` | `user_login` | 5 | 快速失败 |
| 查询用户信息 | `GET /api/short-link/admin/v1/user/{username}` | `get_user` | 20 | 快速失败 |
| 查询无脱敏用户 | `GET /api/short-link/admin/v1/actual/user/{username}` | `get_actual_user` | 20 | 快速失败 |
| 查询用户名是否存在 | `GET /api/short-link/admin/v1/user/has-username` | `check_username` | 20 | 快速失败 |
| 修改用户信息 | `PUT /api/short-link/admin/v1/user` | `update_user` | 5 | 快速失败 |
| 退出登录 | `DELETE /api/short-link/admin/v1/user/logout` | `user_logout` | 10 | 快速失败 |
| 移入回收站 | `POST /api/short-link/admin/v1/recycle-bin/save` | `recycle_save` | 5 | 快速失败 |
| 回收站分页查询 | `GET /api/short-link/admin/v1/recycle-bin/page` | `recycle_page` | 20 | 快速失败 |
| 恢复短链接 | `POST /api/short-link/admin/v1/recycle-bin/recover` | `recycle_recover` | 5 | 快速失败 |
| 删除短链接 | `POST /api/short-link/admin/v1/recycle-bin/remove` | `recycle_remove` | 5 | 快速失败 |
| 单链接统计 | `GET /api/short-link/admin/v1/stats` | `stats_single` | 10 | 快速失败 |
| 分组统计 | `GET /api/short-link/admin/v1/stats/group` | `stats_group` | 10 | 快速失败 |
| 单链接访问记录 | `GET /api/short-link/admin/v1/stats/access-record` | `stats_access_record` | 10 | 快速失败 |
| 分组访问记录 | `GET /api/short-link/admin/v1/stats/access-record/group` | `stats_group_access_record` | 10 | 快速失败 |

#### Scenario: 写接口（创建/修改/删除）低频保护
- **WHEN** 某写操作接口在 1 秒内收到超过其 QPS 上限的请求
- **THEN** 系统 SHALL 拦截超出部分的请求，返回 HTTP 429，不执行业务逻辑

#### Scenario: 读接口（查询/统计）高频允许
- **WHEN** 查询类接口在 1 秒内收到不超过其 QPS 上限的请求
- **THEN** 系统 SHALL 正常处理所有请求，不触发限流

---

### Requirement: 限流触发统一响应
当接口触发限流时，系统 SHALL 返回 HTTP 状态码 429，响应体包含错误码 `RATE_LIMIT_429` 和接口自定义或默认提示信息。

#### Scenario: 普通接口（有返回值）触发限流
- **WHEN** 带返回值的 Controller 方法触发 Sentinel BlockException
- **THEN** 系统 SHALL 返回 HTTP 429，Body 为 `{"code": "RATE_LIMIT_429", "message": "<接口自定义或默认提示>", "data": null}`

#### Scenario: void 接口（批量创建）触发限流
- **WHEN** 返回类型为 `void` 的接口（如批量创建短链接）触发 BlockException
- **THEN** 系统 SHALL 通过 `HttpServletResponse` 设置 status=429 并写入相同格式的 JSON Body

---

### Requirement: 废弃原始 SentinelRuleConfig
原 `SentinelRuleConfig` 中的硬编码规则 SHALL 被废弃，限流规则统一由 `RateLimitAspect` 切面通过 `@RateLimit` 注解动态加载，不再在启动时集中注册。

#### Scenario: 启动时不预加载规则
- **WHEN** 应用启动
- **THEN** 系统 SHALL 不再通过 `FlowRuleManager.loadRules` 批量加载规则，`SentinelRuleConfig` 被删除或标注为废弃
