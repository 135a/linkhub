## Why

项目目前仅通过 `localhost:端口` 方式访问各服务，缺乏域名路由能力。需要支持两种访问模式：本地开发时通过 `localhost:端口` 直接访问，线上部署时通过域名访问（`linkhub.nym.asia` 访问短链接项目，`log.nym.asia` 访问日志系统）。这是项目上线到云服务器的基础需求。

## What Changes

- **新增 Nginx 反向代理服务**：在 Docker Compose 中新增一个 Nginx 容器，监听 80 端口，基于域名（`server_name`）将请求路由到不同的后端服务
- **配置 `linkhub.nym.asia` 域名路由**：将域名请求代理到 gateway 服务（`gateway:8080`），同时代理前端静态资源
- **配置 `log.nym.asia` 域名路由**：将域名请求代理到日志前端（`log-frontend:80`），API 请求代理到 log-collector（`log-collector:8081`）
- **保持本地端口直连**：不修改现有的端口映射，`localhost:8000`（gateway）、`localhost:3001`（日志前端）等保持可用
- **更新 CORS 配置**：在 gateway 的 CORS 允许来源中添加域名

## Capabilities

### New Capabilities

- `nginx-domain-routing`: 覆盖 Nginx 反向代理的域名路由配置——基于 `server_name` 的虚拟主机分发、SSL 预留、各服务的代理规则

### Modified Capabilities

_(无现有 spec 需要修改)_

## Impact

- **新增文件**：
  - Nginx 反向代理配置文件（`nginx/` 目录）
  - Docker Compose 中新增 nginx 服务
- **修改文件**：
  - `docker-compose.yml` — 添加 nginx 反向代理服务
  - `docker-compose.prod.yml` — 添加 nginx 反向代理服务（含资源限制）
  - `short-link-gateway-go/config.yaml` — CORS 允许来源添加域名
- **外部依赖**：需要在 DNS 提供商配置 `linkhub.nym.asia` 和 `log.nym.asia` 的 A 记录指向云服务器 IP
- **无 Breaking Change**：新增反向代理层，不影响现有的 localhost 端口访问方式
