# ShortLink Monolith

这是一个用于实习面试展示的短链接个人项目，采用单仓库协作，后端为独立 Spring Boot 单体。

## 项目结构

- `console-vue/`: 前端控制台（Vue 3 + Vite）
- `project/`: 后端单体服务（唯一后端模块）
- `deploy/nginx/`: 生产网关与域名映射配置
- `docs/`: 架构、部署、观测与迁移文档

## 快速启动

1. 构建后端 jar：
   - `mvn -DskipTests clean package`
2. 构建并启动容器：
   - `docker compose up -d --build`
3. 访问：
   - 前端：`http://localhost`（生产域名为 `https://shortlink.nym.asia`）
   - API：`http://localhost/api/...`（经 Nginx 转发至 `project`）

## 质量与治理

- 关键词治理脚本：`scripts/check-content.ps1`
- 反向代理冒烟脚本：`scripts/smoke-nginx.ps1`
- CI 会拆分前端与后端构建/测试，并执行内容检查

## 文档入口

- 架构与迁移：`docs/architecture.md`
- 部署与回滚：`docs/deployment.md`
- 可观测性基线：`docs/observability.md`
- 迁移验收清单：`docs/monolith-cutover-checklist.md`
