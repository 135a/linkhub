## Context

当前短链接系统存在跳转失败的问题。主要原因在于 Nginx 仅转发 `/api` 路径，导致根路径下的短链接请求无法到达后端。此外，后端在未命中短链接时重定向到空的 `notfound.html`，且其内部域名匹配逻辑过于严格。

同时，新发现了一个 Bug：前端显示的链接文本与实际跳转的 `href` 不一致。这是因为数据库中的 `domain` 字段与 `fullShortUrl` 中的域名部分发生了脱节。

## Goals / Non-Goals

**Goals:**
- 实现 Nginx 对根路径短链接请求的正确转发。
- 在后端提供处理短链接跳转的 API 入口。
- 修复 `domain` 与 `fullShortUrl` 的一致性问题。
- 优化后端域名匹配逻辑，提高在不同环境下的兼容性。
- 完善 404 错误页面的展示。

**Non-Goals:**
- 不涉及短链接生成算法的修改。

## Decisions

### 1. Nginx 路由优化
在 Nginx 配置中增加一个针对短链接特征（Base62 格式，通常为 6 位字符）的 location 块。
- **方案**：使用正则匹配 `~^/([a-zA-Z0-9]{6})$`，将其转发至后端服务。

### 2. 后端跳转入口补全
在后端新增一个跳转控制器映射。
- **映射**：`@GetMapping("/{shortUri}")`。
- **逻辑**：调用 `shortLinkService.restoreUrl(shortUri, request, response)`。

### 3. 数据一致性修复
在 `ShortLinkServiceImpl` 的创建和更新逻辑中，确保 `domain` 和 `fullShortUrl` 始终同步。
- **方案**：在 `updateShortLink` 的跨分组逻辑中，不再使用默认域名覆盖 `domain` 字段，而是保持原域名或根据 `fullShortUrl` 动态解析。

### 4. 域名匹配逻辑增强
修改 `ShortLinkServiceImpl.restoreUrl` 中的 `fullShortUrl` 构建逻辑。
- **方案**：不再单纯依赖 `request.getServerName()`，改为从配置或数据库记录中匹配。

### 5. 404 页面填充
为 `notfound.html` 编写基础 HTML 代码。

## Risks / Trade-offs

- **正则冲突风险**：如果前端存在同样长度为 6 位字符的路由，可能会被 Nginx 错误转发至后端。
- **Trade-off**：优先保证短链接可用性，建议前端路由使用更具辨识度的路径（如 `/dashboard/*`）。
