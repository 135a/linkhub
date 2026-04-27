## 1. 统一 AOP 耗时统计与日志清理

- [x] 1.1 在后端工程中创建一个 AOP 切面类（如 `ApiExecutionTimeAspect`），配置对所有 `com.nym.shortlink.core.controller` 包下的接口方法的拦截，自动记录接口的执行起止时间，并统一输出带有接口信息与总耗时的 `log.info`。
- [x] 1.2 搜索并清理现有的所有 Controller 类代码（例如 `UserController` 等），将原先为了打印入参出参或调试而在业务层中手工散布的冗余 `log.info` 全部删除。

## 2. 前端登录交互修复

- [x] 2.1 审查并修改前端的登录逻辑文件（通常位于 `console-vue/src/views/login/index.vue`），在接收到 HTTP 返回的成功标志并完成 Token 的本地存储后，新增 `router.push('/')` （或其他有效的主页路由），以保证登录闭环，避免页面停滞。
