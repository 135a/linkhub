## 1. Nginx 路由配置

- [x] 1.1 在 Nginx 配置文件中增加对根路径短链接的识别逻辑（匹配 6 位 Base62 字符）。
- [x] 1.2 将匹配到的请求转发至后端服务（不带 `/api` 前缀）。

## 2. 后端入口补全

- [x] 2.1 修改 `ShortLinkController.java`，添加处理短链接跳转的 `@GetMapping("/{shortUri}")` 接口。
- [x] 2.2 在该接口中调用 `shortLinkService.restoreUrl` 方法。

## 3. 跳转逻辑与数据一致性优化

- [x] 3.1 修改 `ShortLinkServiceImpl.java` 中的 `restoreUrl` 方法。
- [x] 3.2 优化 `fullShortUrl` 的构建和匹配逻辑，使其支持跨域兼容。
- [x] 3.3 修复 `updateShortLink` 方法，确保更新分组时 `domain` 字段不会被错误的默认值覆盖。
- [x] 3.4 检查 `createShortLink` 方法，确保新生成的记录中 `domain` 与 `fullShortUrl` 严格同步。

## 4. 404 页面完善

- [x] 4.1 编辑 `src/main/resources/templates/notfound.html` 文件。
- [x] 4.2 添加基本的 HTML 结构、错误提示信息（“链接已失效或不存在”）及返回首页的按钮。

## 5. 功能验证

- [x] 5.1 验证正常短链接的跳转是否成功。
- [x] 5.2 验证不同域名访问下的跳转稳定性。
- [x] 5.3 验证无效短链接是否能正确显示 404 提示页。
