## 1. 后端修复：Redis Lua 流控脚本

- [x] 1.1 编辑 `shortlink-main/main/src/main/resources/lua/user_flow_risk_control.lua`，使用 `INCR` 和 `EXPIRE` 实现时间窗口内的计数统计逻辑。

## 2. 后端修复：用户身份提取回退机制

- [x] 2.1 修改 `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/biz/user/UserFlowRiskControlFilter.java`，在获取不到 `UserContext.getUsername()` 时，从 `HttpServletRequest` 的 `username` Header 提取，若依然无值再回退使用 `"other"`。

## 3. 前端修复：全局风控拦截提示

- [x] 3.1 修改 `console-vue/src/api/axios.js`，在响应拦截器处理业务响应结果时（如 `res.data.success === false` 且包含 message 等场景），引入并调用 `ElMessage.error()` 弹出对应的错误信息。
