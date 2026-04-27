## 1. 核心逻辑修改

- [x] 1.1 修改 `ShortLinkServiceImpl.java` 中的 `createShortLink` 方法，支持从请求参数获取域名。
- [x] 1.2 修改 `ShortLinkServiceImpl.java` 中的 `createShortLinkByLock` 方法，支持从请求参数获取域名。
- [x] 1.3 修改 `ShortLinkServiceImpl.java` 中的 `generateSuffix` 和 `generateSuffixByLock` 方法，使其接受域名参数。

- [x] 1.4 修改 `project.yaml`、`project_app.yaml` 以及 `main/src/main/resources/application.yaml`，将 `short-link.domain.default` 改为引用环境变量 `${DOMAIN:127.0.0.1:8001}`。
- [x] 1.5 在各级 `.env` 文件中添加 `VITE_DOMAIN=shortlink.nym.asia` (包括根目录和 console-vue 目录)。
- [x] 1.6 修改 `console-vue/src/store/index.js`，使用 `import.meta.env.VITE_DOMAIN`。

## 2. 验证与测试

- [ ] 2.1 验证在指定域名（如 `shortlink.nym.asia`）时，生成的 `fullShortUrl` 正确。
- [ ] 2.2 验证未指定域名时，系统正确读取 `.env` 中的 `DOMAIN` 环境变量作为默认域名。
- [ ] 2.3 验证在 `.env` 中未定义 `DOMAIN` 时，回退到 `127.0.0.1:8001`。
