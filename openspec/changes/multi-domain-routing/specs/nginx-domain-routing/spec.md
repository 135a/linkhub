## ADDED Requirements

### Requirement: linkhub.nym.asia 域名路由

Nginx 反向代理 SHALL 将 `linkhub.nym.asia` 域名的请求路由到短链接项目的 gateway 服务。所有请求 MUST 代理到 `gateway:8080`。

#### Scenario: API 请求路由

- **WHEN** 客户端通过 `http://linkhub.nym.asia/api/v1/shortlink/...` 发起请求
- **THEN** 请求被代理到 `gateway:8080`，响应正常返回

#### Scenario: 根路径请求路由

- **WHEN** 客户端通过 `http://linkhub.nym.asia/` 发起请求
- **THEN** 请求被代理到 `gateway:8080`

### Requirement: log.nym.asia 域名路由

Nginx 反向代理 SHALL 将 `log.nym.asia` 域名的请求路由到日志系统前端服务。所有请求 MUST 代理到 `log-frontend:80`。

#### Scenario: 日志前端页面访问

- **WHEN** 客户端通过 `http://log.nym.asia/` 访问日志前端
- **THEN** 请求被代理到 `log-frontend:80`，返回日志查询页面

#### Scenario: 日志 API 请求路由

- **WHEN** 客户端通过 `http://log.nym.asia/api/v1/logs/query` 发起查询请求
- **THEN** 请求被代理到 `log-frontend:80`，由其内部 nginx 转发到 `log-collector:8081`

### Requirement: 未知域名拒绝访问

Nginx 反向代理 MUST 配置 `default_server` 块，对未匹配任何已配置域名的请求返回 444 状态码（直接关闭连接）。

#### Scenario: 通过 IP 直接访问

- **WHEN** 客户端通过服务器 IP 地址直接访问 80 端口
- **THEN** Nginx 返回 444，连接被关闭

#### Scenario: 未配置域名访问

- **WHEN** 客户端通过未配置的域名访问（如 `unknown.example.com`）
- **THEN** Nginx 返回 444，连接被关闭

### Requirement: 本地端口访问不受影响

新增 Nginx 反向代理 MUST NOT 影响现有的 localhost 端口直连方式。各服务的宿主机端口映射 MUST 保持不变。

#### Scenario: 本地访问 gateway

- **WHEN** 用户通过 `http://localhost:8000` 访问 gateway
- **THEN** 请求正常到达 gateway 服务，与新增 nginx-proxy 无关

#### Scenario: 本地访问日志前端

- **WHEN** 用户通过 `http://localhost:3001` 访问日志前端
- **THEN** 请求正常到达 log-frontend 服务，与新增 nginx-proxy 无关

### Requirement: CORS 域名支持

Gateway 的 CORS 配置 MUST 包含域名来源，允许来自 `http://linkhub.nym.asia` 和 `http://log.nym.asia` 的跨域请求。

#### Scenario: 域名来源跨域请求

- **WHEN** 浏览器从 `http://linkhub.nym.asia` 页面发起 API 请求
- **THEN** Gateway 的 CORS 响应头包含 `Access-Control-Allow-Origin: http://linkhub.nym.asia`

### Requirement: Nginx 代理请求头传递

Nginx 反向代理 SHALL 在转发请求时设置 `Host`、`X-Real-IP` 和 `X-Forwarded-For` 请求头，确保后端服务能获取客户端真实信息。

#### Scenario: 后端获取真实客户端 IP

- **WHEN** 客户端 IP 为 `203.0.113.50` 通过 `linkhub.nym.asia` 访问
- **THEN** Gateway 收到的请求中 `X-Real-IP` 为 `203.0.113.50`，`X-Forwarded-For` 包含 `203.0.113.50`

### Requirement: Docker Compose 服务定义

`nginx-proxy` 服务 MUST 在 `docker-compose.yml` 和 `docker-compose.prod.yml` 中定义，映射宿主机 80 端口到容器 80 端口，依赖 `gateway` 和 `log-frontend` 服务。

#### Scenario: 服务启动顺序

- **WHEN** 执行 `docker-compose up -d`
- **THEN** `nginx-proxy` 在 `gateway` 和 `log-frontend` 启动后启动

#### Scenario: 配置文件挂载

- **WHEN** `nginx-proxy` 容器启动
- **THEN** 项目根目录 `nginx/` 下的配置文件被挂载到容器的 `/etc/nginx/conf.d/` 目录
