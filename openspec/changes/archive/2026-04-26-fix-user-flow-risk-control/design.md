## Context

系统在用户流量风控（`UserFlowRiskControlFilter`）方面存在缺陷，导致正常使用受阻：
1. `user_flow_risk_control.lua` 文件为空，导致 Redis 脚本执行结果返回 null，过滤器将其误判为超限。
2. 风控拦截时，获取用户名的逻辑仅依赖 `UserContext`。而在某些请求中（或未正确设置 UserId 时），`UserContext` 为空，导致日志输出为默认的 "other"，无法追溯真实用户。
3. 当前端收到风控拦截响应（`code: "A000300"`，HTTP 200）时，由于 `axios` 响应拦截器未对业务错误状态（`success: false` 或特定 `code`）进行全局提示处理，导致前端无任何弹窗响应，用户体验极差。

## Goals / Non-Goals

**Goals:**
- 提供完整的 Lua 脚本实现基本的时间窗口限流计数。
- 在 `UserFlowRiskControlFilter` 中增加从 HTTP Request Header (`username`) 获取用户标识的回退机制。
- 完善前端 `axios.js` 的响应拦截器，对包含 `success: false` 及特定错误码的响应进行统一的 `ElMessage` 弹窗提示。

**Non-Goals:**
- 不重构整体限流架构（如引入 Sentinel 或其他限流组件）。
- 不修改现有的 `UserTransmitFilter` 逻辑，仅在风控过滤器内部做兜底。

## Decisions

1. **Lua 脚本补全 (Redis 限流逻辑)**:
   - **Decision**: 使用 `INCR` 和 `EXPIRE` 命令实现固定窗口限流。
   - **Rationale**: 传递给脚本的参数为 `KEYS[1]` = username, `ARGV[1]` = timeWindow。使用 `INCR` 每次请求自增，当结果为 1 时（即第一次请求），设置过期时间为 `timeWindow`。返回当前的计数。
   - **Alternative**: 使用滑动窗口（ZSET），但固定窗口已经足够满足当前 `UserFlowRiskControlConfiguration` 的设计需求且性能更高。

2. **用户名获取兜底**:
   - **Decision**: 在 `UserContext.getUsername()` 返回 null 时，尝试通过 `((HttpServletRequest) request).getHeader("username")` 获取。若仍为空，再回退到 "other"。
   - **Rationale**: 前端请求默认携带了 `Username` Header，而 `UserTransmitFilter` 强依赖 `userId` 的存在。为避免风控日志记录丢失，增加直接读 Header 的能力最为稳妥。

3. **前端拦截器全局错误提示**:
   - **Decision**: 在 `console-vue/src/api/axios.js` 中拦截响应，当 `res.data.success === false` 时，使用 `ElMessage.error(res.data.message)` 进行提示，并返回 `Promise.reject(res.data)`。
   - **Rationale**: 统一处理所有业务层异常（包括 `A000300`）。避免在每个 API 调用处重复写错误处理逻辑。

## Risks / Trade-offs

- **[Risk]** 固定窗口限流可能存在临界点突发流量问题。
  **[Mitigation]** 作为后台管理/小型项目，目前的并发量较低，此问题可接受。未来若业务扩大，可再升级为令牌桶算法。
- **[Risk]** 前端拦截器抛出业务错误后，可能会阻断原本的 `.then()` 逻辑。
  **[Mitigation]** 确保在 `.catch()` 中进行处理，或业务代码不需要过度关心网络层之外的逻辑。统一拦截已经是最优实践。
