## 背景

当前系统在创建短链接时，硬编码使用了 `createShortLinkDefaultDomain`（来自配置文件的默认域名），导致无法支持请求中指定的自定义域名。

## 目标 / 非目标

**目标:**
- 允许通过 API 请求指定短链接的域名。
- 确保系统在处理自定义域名时，布隆过滤器、数据库和缓存的一致性。

**非目标:**
- 校验自定义域名是否已在系统注册或备案（本阶段仅支持逻辑切换）。

## 设计决策

1.  **域名解析逻辑**:
    在 `createShortLink` 和 `createShortLinkByLock` 方法开始时，确定最终使用的域名：
    ```java
    String actualDomain = StrUtil.isNotBlank(requestParam.getDomain()) 
        ? requestParam.getDomain() 
        : createShortLinkDefaultDomain;
    ```

2.  **重构 Suffix 生成逻辑**:
    `generateSuffix` 和 `generateSuffixByLock` 方法需要感知当前使用的域名，以便在布隆过滤器校验时使用正确的 `fullShortUrl`。
    建议将解析后的 `actualDomain` 作为参数传递给这些方法，或者直接在方法内部重新解析。为了保持一致性，建议在 Service 方法中解析一次并传递。

3.  **模型填充**:
    使用 `actualDomain` 填充 `ShortLinkDO` 的 `domain` 字段和构造 `fullShortUrl`。

4.  **配置更新**:
    修改以下配置文件中的 `short-link.domain.default` 配置，使其引用环境变量 `DOMAIN`：
    - `project.yaml` (根目录)
    - `project_app.yaml` (根目录)
    - `main/src/main/resources/application.yaml` (核心资源目录)

    同时在 `.env` 中添加 `VITE_DOMAIN` 供前端使用。

5.  **前端 Store 修改**:
    修改 `console-vue/src/store/index.js`，使用 `import.meta.env.VITE_DOMAIN || 'link.example.com'` 作为默认值。

## 风险 / 权衡

- **布隆过滤器碰撞**: 如果多个域名使用同一个布隆过滤器，且后缀生成逻辑依赖于 `domain + "/" + suffix`，则需要确保布隆过滤器的 key 包含域名信息。当前实现中已经包含了域名，所以只需确保使用的是 `actualDomain`。
- **环境一致性**: 确保生产环境部署时，`.env` 文件或容器环境变量中已正确配置 `DOMAIN`，否则将回退到 `127.0.0.1:8001`。
