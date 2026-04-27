## Context

在访问短链接分组列表接口（`GET /api/short-link/admin/v1/group`）时，后端抛出 `ShardingSphere` 分片路由异常：`Sharding value 'null' must implements Comparable`。
经过排查，抛出异常的原因是 `GroupServiceImpl.listGroup()` 调用了 `UserContext.getUsername()` 作为分片键，但返回了 `null`。

`UserContext` 内部通过 `UserTransmitFilter` 进行初始化。当前的 `UserTransmitFilter` 逻辑为：
```java
String userId = httpServletRequest.getHeader(UserConstant.USER_ID_KEY);
if (StringUtils.hasText(userId)) {
    // ... 解析并设置 UserContext
}
```
**关键修正**：
- 前端（Console Vue）在发送请求时，请求头中携带了 `Token` 和 `Username`。
- **Go Gateway 与本项目是独立项目，不会接入。**
- 因此，本项目作为单体架构，必须完全依靠自身的拦截器/过滤器（如 `UserTransmitFilter`）来解析请求头并建立用户上下文。目前由于 `UserTransmitFilter` 强依赖 `userId` 请求头而前端未传，导致上下文丢失。

## Goals / Non-Goals

**Goals:**
- 修复 `t_group` 查询时 `username` 分片键为 `null` 的问题。
- 确保 Java 后端能够独立、正确地从前端请求头中提取并建立用户上下文（不依赖外部 Gateway 注入 `userId`）。

**Non-Goals:**
- 不在此变更中引入复杂的分布式 Session 管理。
- 不修改现有的分片策略或表结构。

## Decisions

**决策 1：修改 `UserTransmitFilter` 的触发条件 (Chosen)**
- **逻辑调整**：将 `UserTransmitFilter` 解析用户信息的触发条件由“必须存在 `userId`”改为“优先检查 `username`”。
- **Rationale (原因)**：既然没有外部 Gateway 注入 `userId`，且前端当前是通过 `Username` 请求头传递身份信息的，后端应以此为依据初始化上下文。`username` 是本系统路由分片的核心标识。

**决策 2：同步删除废弃的 `UserTransmitInterceptor`**
- **逻辑调整**：删除 `core` 模块中残留且未生效的 `UserTransmitInterceptor`，统一使用 `UserTransmitFilter`。

## Risks / Trade-offs

- **[Risk] 部分下游逻辑可能依赖 `userId` 导致空指针**
  → Mitigation：在设置 `UserInfoDTO` 时允许 `userId` 为空（或保持为 `null`）。当前 `core` 模块的绝大部分核心业务（包含增删改查和短链接生成）主要依赖 `username` 作为业务隔离键和分片键。
- **[Risk] 安全性风险**
  → Mitigation：由于 Nginx 暂时直接透传前端请求，客户端伪造 `Username` 请求头可能存在越权风险。此风险将在后续完整启用 Go-Gateway 并在 Gateway 层强制拦截校验 Token 后得到彻底解决。本次变更为纯逻辑连通性修复。
