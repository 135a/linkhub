## Context

当前项目通过 Docker Compose 部署，各服务直接暴露宿主机端口供访问：
- Gateway（短链接 API）：`localhost:8000`
- 日志前端：`localhost:3001`
- 日志 API：`localhost:8081`

云服务器域名为 `linkhub.nym.asia`，需要新增基于域名的路由：
- `linkhub.nym.asia` → 短链接项目（gateway + 前端）
- `log.nym.asia` → 日志系统

现有架构中，`log-frontend` 已有自己的 nginx 容器处理静态资源和 API 代理，短链接前端（`console-vue`）在 `docker-compose.yml` 中被注释掉。

## Goals / Non-Goals

**Goals:**
- 通过单一 Nginx 入口实现基于域名的虚拟主机路由
- 线上通过 `linkhub.nym.asia` 和 `log.nym.asia` 访问对应服务
- 本地开发保持 `localhost:端口` 直连方式不受影响
- 为后续 HTTPS/SSL 配置预留扩展点

**Non-Goals:**
- 不在此次变更中配置 SSL/HTTPS 证书（后续用 certbot 或 acme.sh）
- 不修改各服务的内部端口或网络架构
- 不做负载均衡或多实例支持
- 不修改 DNS 配置（需用户自行在 DNS 提供商配置 A 记录）

## Decisions

### 1. 新增独立 Nginx 反向代理容器，而非复用 log-frontend 的 nginx

**决定**：在 Docker Compose 中新增一个独立的 `nginx-proxy` 服务，监听宿主机 80 端口。

**替代方案**：复用 `log-frontend` 的 nginx 容器，增加虚拟主机配置。

**理由**：`log-frontend` 的 nginx 是专为日志前端设计的（处理 Vue SPA + API 代理），职责单一。新增独立代理容器实现关注点分离：入口路由与应用服务互不影响，便于独立升级和维护。

### 2. 基于 `server_name` 的虚拟主机路由

**决定**：使用 Nginx 的 `server_name` 指令，为每个域名配置独立的 `server` 块。

**替代方案**：使用基于路径的路由（如 `/log/*` → 日志系统）。

**理由**：域名路由更清晰，避免路径冲突，且符合用户的明确需求（`log.nym.asia` vs `linkhub.nym.asia`）。

### 3. Nginx 配置文件使用宿主机挂载

**决定**：将 nginx 配置文件放在项目的 `nginx/` 目录中，通过 Docker volume 挂载到容器内。

**替代方案**：将配置打包到自定义 Docker 镜像中。

**理由**：挂载方式修改配置后只需 `docker-compose restart nginx-proxy`，无需重新构建镜像，开发和运维效率更高。

### 4. `linkhub.nym.asia` 的路由策略

**决定**：
- `/api/*` 请求代理到 `gateway:8080`（短链接 API）
- `/` 其他请求代理到 `gateway:8080`（由 gateway 处理前端静态资源或返回 API 响应）

**理由**：当前短链接前端在 compose 中被注释掉，gateway 是唯一的短链接入口点。后续如果启用前端容器，只需修改 nginx 配置即可。

### 5. `log.nym.asia` 的路由策略

**决定**：所有请求直接代理到 `log-frontend:80`，由其内部 nginx 处理静态资源分发和 `/api` 代理。

**替代方案**：在外层 nginx 中分别配置静态资源和 API 代理规则。

**理由**：`log-frontend` 内部的 nginx 已经完整处理了 Vue SPA 路由和 API 代理，外层 nginx 只需做域名路由，避免重复配置。

### 6. 默认 server 块处理未知域名

**决定**：配置一个 `default_server` 块，对未匹配的域名返回 444（直接关闭连接）。

**理由**：安全最佳实践，防止通过 IP 直接访问或未配置域名的请求到达后端服务。

## Risks / Trade-offs

- **[端口 80 冲突]** → 如果宿主机已有其他服务占用 80 端口，需手动调整。可通过环境变量或 `.env` 文件自定义映射端口。
- **[无 HTTPS]** → 首次部署仅 HTTP，中间人攻击风险。缓解：后续迭代中通过 certbot 自动获取 Let's Encrypt 证书。本次预留 443 端口和 SSL 配置目录的挂载点。
- **[DNS 传播延迟]** → 新域名配置后可能需要 24-48 小时全球生效。可通过本地 hosts 文件提前验证。
- **[log-frontend 双层 nginx]** → `log.nym.asia` 经过外层 proxy 再到内层 nginx，多一跳。性能影响可忽略（内网通信），换来的是架构清晰和配置解耦。

## Migration Plan

1. 在项目根目录创建 `nginx/` 配置目录
2. 编写 nginx 虚拟主机配置文件
3. 更新 `docker-compose.yml` 和 `docker-compose.prod.yml` 添加 `nginx-proxy` 服务
4. 更新 gateway CORS 配置添加域名
5. 部署：`docker-compose up -d nginx-proxy`
6. 配置 DNS A 记录：`linkhub.nym.asia` 和 `log.nym.asia` → 服务器 IP
7. 验证域名访问正常
8. 回滚：`docker-compose stop nginx-proxy`，恢复直接端口访问
