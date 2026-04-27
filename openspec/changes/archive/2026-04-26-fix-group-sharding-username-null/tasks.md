## 1. 拦截器逻辑优化

- [x] 1.1 修改 `UserTransmitFilter.java` 中的解析逻辑，优先检查 `username` 请求头是否存在
- [x] 1.2 在 `UserTransmitFilter` 中，只要 `username` 存在，即初始化 `UserContext` 内部的用户信息

## 2. 代码清理

- [x] 2.1 删除 `com.nym.shortlink.core.common.biz.user.UserTransmitInterceptor.java`（该拦截器功能已被 Filter 替代且未注册）

## 3. 功能验证

- [x] 3.1 启动项目并调用 `GET /api/short-link/admin/v1/group` 接口
- [x] 3.2 确认日志中不再出现 `Sharding value 'null' must implements Comparable` 异常
- [x] 3.3 验证前端分组列表页面是否能正常展示数据
