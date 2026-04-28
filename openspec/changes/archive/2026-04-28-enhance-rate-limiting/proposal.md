## Why

`SentinelRuleConfig` 已在启动时向 Sentinel 注册了 QPS 限流规则，但 Controller 层缺少资源埋点（`@SentinelResource` 注解及对应的 AOP 切面），导致规则悬空、限流实际不生效。随着短链接服务上线，创建接口面临恶意批量请求风险，跳转接口承载大量并发，必须尽快补全限流防护。

## What Changes

- 在 Spring 配置类中注册 `SentinelResourceAspect` Bean，使 `@SentinelResource` 注解能被 AOP 代理拦截
- 为**创建短链接**接口（`POST /api/short-link/admin/v1/create`）添加 `@SentinelResource` 埋点，资源名 `create_short-link`，QPS 上限 1
- 为**批量创建短链接**接口（`POST /api/short-link/admin/v1/create/batch`）添加 `@SentinelResource` 埋点，资源名 `batch-create_short-link`，QPS 上限 1
- 为**短链接跳转**接口（`GET /{shortUri}`）添加 `@SentinelResource` 埋点，资源名 `redirect_short-link`，QPS 上限较高（如 100），防止异常流量冲击
- 在 `SentinelRuleConfig` 中补充批量创建和跳转的 `FlowRule`，确保三个接口的规则完整加载
- 为每个接口提供 `blockHandler` 降级方法，限流触发时返回统一的 HTTP 429 响应和提示信息

## Capabilities

### New Capabilities

- `api-rate-limiting`：短链接核心接口（创建、批量创建、跳转）的 Sentinel QPS 限流能力，包含规则配置、AOP 埋点及降级处理

### Modified Capabilities

- `shortlink-management`：短链接管理接口的行为新增限流约束，超出 QPS 时返回 429 而非正常响应

## Impact

- **代码**：`SentinelRuleConfig.java`、`ShortLinkController.java`、Spring MVC 配置类（注册 `SentinelResourceAspect`）
- **API**：创建 / 批量创建 / 跳转三个接口在超限时响应从 200 变为 429
- **依赖**：已有 `com.alibaba.csp:sentinel-core`，需确认是否引入 `sentinel-annotation-aspectj` 依赖（`@SentinelResource` AOP 支持）
- **测试**：需补充限流场景的集成测试，验证超限请求确实被拦截
