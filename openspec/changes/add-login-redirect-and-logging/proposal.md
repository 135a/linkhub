## Why

1. 目前我们在开发与调试过程中，很难量化各个业务逻辑的执行时间。通过在所有的 Controller 接口中自动打印请求的相关时间戳（开始、结束或耗时），能极大地提升我们对系统性能的感知，帮助我们优化和计算 Service 方法的执行耗时。
2. 前端系统中存在一个明显的交互缺陷：用户在成功登录后，前端正确收到了包含 token 的业务成功响应（`success: true`，`code: "0"`），但前端代码并未触发页面跳转，导致用户停留在登录页，中断了使用体验。

## What Changes

- **后端 Controller 耗时日志**：新增全局的面向切面（AOP）拦截或 Web 过滤器。当每个请求到达 Controller 接口时记录开始时间，在方法执行结束返回时，利用 `log.info` 打印该次请求消耗的时间戳，避免对每个 Controller 进行冗余的人工修改。
- **前端登录成功重定向**：修复前端应用中的登录逻辑（通常在如 `login.vue` 或其相关组件中）。在接口返回成功并把 Token 保存后，触发页面路由跳转逻辑，引导用户进入系统主页。

## Capabilities

### New Capabilities
- `frontend-login-redirect`: 前端登录行为规范，涵盖验证成功后的页面重定向交互体验。

### Modified Capabilities
- `api-request-logging`: 扩展现有的 API 日志规范，要求额外记录并输出接口的时间戳和总体耗时。

## Impact

- **前端系统**：`console-vue` 中处理登录动作的相关 Vue 页面及路由。
- **后端系统**：`core` 模块中用于记录 Controller 日志的 AOP 切面或 Web Interceptor，属于非侵入性增强。
