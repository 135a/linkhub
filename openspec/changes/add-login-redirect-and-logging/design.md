## Context

当前项目中为了分析接口执行耗时，开发者曾在各个 Controller 层中散落了许多 `log.info` 的调用语句。这不仅使业务代码变得冗余，而且维护成本高。另外，前端应用在用户成功登录、后台正确返回 Token 之后，缺失了路由重定向的逻辑控制，导致用户视觉上完全感知不到系统状态的变化，一直停留在登录页面。

## Goals / Non-Goals

**Goals:**
- **全局切面耗时统计**：通过 Spring AOP（面向切面编程）的 `@Around` 通知，拦截所有 Controller 中的接口执行，并自动计算和打印该次请求消耗的总时间。
- **冗余日志清理**：将 Controller 文件中现有的、用于打印出入参及耗时相关的手工 `log.info` 语句进行批量删除，使业务层代码更干净纯粹。
- **前端成功跳转**：补全并修正登录页面（如 `login.vue`）中的逻辑，在其收到成功登录的 Promise Resolve 之后，调用 `router.push` 将页面导航至系统主页（如 `/` 或 `/home`）。

**Non-Goals:**
- 不涉及引入大型的外部监控系统和链路追踪工具（如 SkyWalking 或 Prometheus），仅在当前控制台与日志文件中输出。

## Decisions

1. **AOP 集中式日志管理**:
   - **Decision**: 在工程内创建一个如 `ApiExecutionTimeAspect` 的 AOP 类，将切入点（Pointcut）定位于拦截所有 `com.nym.shortlink.core.controller` 包下的方法。利用 `ProceedingJoinPoint.proceed()` 的前后时间戳计算得到耗时并使用统一格式 `log.info` 打印。
   - **Rationale**: AOP 提供了对横切关注点的完美支持，能做到零代码侵入。这能直接使得我们可以安全地大面积清理掉之前手工写入的所有 Controller 日志。

2. **前端重定向控制**:
   - **Decision**: 检查和修改 `console-vue/src/views/login/index.vue` 或处理登录相关逻辑的 Vue 组件，补充 Vue Router 的 `.push()` 跳转操作。
   - **Rationale**: 登录成功后进行页面跳转是最基础和标准的用户体验规范。

## Risks / Trade-offs

- **[Risk]** 在大批量删除原有 Controller 手写日志时，有可能会误删一些含有特殊上下文信息的调试记录。
  **[Mitigation]** 严格采用正则或语义过滤，仅清理针对“接口入口/出口”记录的通用日志；同时，如果必要，可以在 AOP 切面中增加对目标方法名、执行状态的完整输出记录。
