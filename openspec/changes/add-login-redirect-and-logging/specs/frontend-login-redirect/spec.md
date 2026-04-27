# Capability: Frontend Login Redirect

## Purpose
前端登录行为规范，涵盖验证成功后的页面重定向交互体验，保证用户登录闭环。

## ADDED Requirements

### Requirement: 登录成功后重定向至主页
前端应用在收到后端的有效登录响应并正确保存鉴权令牌（Token）后，MUST 立即自动执行路由跳转，将用户引导至系统内部首页。

#### Scenario: 处理成功的登录响应
- **WHEN** 客户端发出登录请求，且后端返回明确的成功标志（如 `success: true`）并提供了 Token 数据时
- **THEN** 前端界面 MUST 在完成凭证存储后，自动将页面路由导航至系统主页面（如 `/` 或 `/home`）。
