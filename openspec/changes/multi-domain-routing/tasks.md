## 1. Nginx 配置文件创建

- [x] 1.1 在项目根目录创建 `nginx/` 目录
- [x] 1.2 创建 `nginx/default.conf`，配置 default_server 块（返回 444 拒绝未知域名）
- [x] 1.3 创建 `nginx/linkhub.conf`，配置 `linkhub.nym.asia` 虚拟主机，将请求代理到 `gateway:8080`，设置 `Host`、`X-Real-IP`、`X-Forwarded-For` 请求头
- [x] 1.4 创建 `nginx/log.conf`，配置 `log.nym.asia` 虚拟主机，将请求代理到 `log-frontend:80`，设置代理请求头

## 2. Docker Compose 配置

- [x] 2.1 在 `docker-compose.yml` 中新增 `nginx-proxy` 服务，使用 `nginx:latest` 镜像，映射 `80:80`，挂载 `./nginx/` 到 `/etc/nginx/conf.d/`，依赖 `gateway` 和 `log-frontend`
- [x] 2.2 在 `docker-compose.prod.yml` 中新增 `nginx-proxy` 服务，添加资源限制（memory limit 128m）

## 3. CORS 配置更新

- [x] 3.1 在 `short-link-gateway-go/config.yaml` 的 `cors.allowed_origins` 中添加 `http://linkhub.nym.asia` 和 `http://log.nym.asia`

## 4. 验证

- [ ] 4.1 启动 nginx-proxy 服务，验证容器正常运行
- [ ] 4.2 通过修改本地 hosts 文件，验证 `linkhub.nym.asia` 路由到 gateway
- [ ] 4.3 通过修改本地 hosts 文件，验证 `log.nym.asia` 路由到日志前端
- [ ] 4.4 验证 `localhost:8000` 和 `localhost:3001` 仍然正常访问
- [ ] 4.5 验证通过 IP 直接访问 80 端口被拒绝（返回 444）
