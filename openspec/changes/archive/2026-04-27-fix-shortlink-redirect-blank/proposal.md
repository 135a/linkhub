## Why

用户生成的短链接在浏览器访问时出现一片空白。根据调研和用户反馈，核心原因如下：
1. **Nginx 路由限制**：现有的 `nginx-domain-routing` 规范仅将 `/api` 前缀的请求转发至后端。由于短链接通常形如 `domain/short-code`（不含 `/api`），这些请求被 Nginx 误认为是前端路由，导致后端根本接收不到跳转请求。
2. **空白错误页**：当后端接收到请求但未命中短链接时（例如域名不匹配或已过期），会重定向到 `/page/notfound`。目前 `notfound.html` 为空文件，导致最终显示一片空白。
3. **域名与完整链接不一致（新 Bug）**：系统在显示短链接时使用 `domain + shortUri`，而在点击跳转时使用 `fullShortUrl`。如果这两个字段在数据库中不一致（例如在更新分组时使用了默认域名覆盖了原域名），会导致用户看到的链接和点击的链接不一致。
4. **域名匹配过于严格**：后端跳转逻辑使用 `serverName + serverPort` 进行精确匹配，在多域名或复杂网络环境下容易失效。

## What Changes

- **优化 Nginx 路由**：更新 Nginx 规范，支持将非 API 且非静态资源的根路径请求转发至后端处理跳转逻辑。
- **完善 404 页面**：为 `notfound.html` 添加友好的用户提示和引导，消除空白页面。
- **修复数据一致性问题**：确保 `domain` 字段与 `fullShortUrl` 中的域名部分始终保持一致，特别是在创建和更新逻辑中。
- **增强后端跳转兼容性**：改进 `ShortLinkServiceImpl.restoreUrl` 中的域名拼接和匹配逻辑，确保不同访问方式下的稳定性。
- **集成入口补全**：确保后端有明确的 Controller 映射处理跳转请求。

## Capabilities

### Modified Capabilities
- `nginx-domain-routing`: 扩展路由规则，支持短链接路径转发。
- `shortlink-management`: 修复跳转逻辑中的域名匹配问题，解决字段不一致 Bug，并完善未命中时的错误展示。

## Impact

- **Nginx 配置**: 修改反向代理规则。
- `ShortLinkServiceImpl.java`: 优化 `restoreUrl` 构建逻辑，修复 `updateShortLink` 和 `createShortLink` 中的域名同步问题。
- `notfound.html`: 增加页面内容和基础样式。
- `ShortLinkController.java`: 添加短链接跳转的入口映射。
