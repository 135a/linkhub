# 架构说明（独立后端单体）

## 目标

- 保持单仓库研发协作效率
- 明确前后端边界，前端只通过 API 通信
- 用 Docker Compose 统一开发/测试/部署运行方式
- 后端仅保留一个 Spring Boot 单体模块（`project`）

## 目录映射

- `frontend`: 对应 `console-vue/`
- `backend`: 对应 `project/`
- `deploy/nginx`: 统一入口与域名映射

## 请求链路

1. 浏览器访问 `shortlink.nym.asia`
2. Nginx 返回前端静态资源
3. 前端调用 `/api/...`
4. Nginx 直接转发到 `project:8001`

## API 契约

- 前端 API 基础路径通过 `VITE_API_BASE_URL` 注入
- 默认值：`/api/short-link/v1`
- 由 Nginx 直接反向代理至后端单体

## CI 质量门禁

- 前端：`npm run build` + `npm run lint`
- 后端：`mvn test` + `mvn package`
- 内容治理：禁用词扫描必须通过
