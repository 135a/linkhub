## ADDED Requirements

### Requirement: 域名路由必须将 shortlink.nym.asia 指向前端入口
Nginx MUST 对 `shortlink.nym.asia` 提供站点配置，并将非 API 请求路由到前端静态资源入口，支持单页应用刷新与深链访问。

#### Scenario: 访问前端路由成功回退
- **WHEN** 用户直接访问前端深层路由（如 `/dashboard/links`）
- **THEN** Nginx SHALL 回退到前端入口文件并返回可渲染页面

#### Scenario: 域名绑定生效
- **WHEN** 请求主机头为 `shortlink.nym.asia`
- **THEN** Nginx MUST 命中该站点配置并返回对应前端资源而非默认站点

### Requirement: API 请求必须安全反向代理到后端
Nginx MUST 将 `/api` 前缀请求反向代理至后端服务，并正确传递必要请求头与响应状态码，不篡改业务语义。

#### Scenario: API 请求正常转发
- **WHEN** 客户端请求 `/api/links`
- **THEN** Nginx SHALL 将请求转发到后端 API 服务并返回后端原始业务响应

#### Scenario: 后端异常时前端可感知错误
- **WHEN** 后端服务不可用或返回 5xx
- **THEN** 代理层 MUST 返回可识别的错误状态并记录错误日志用于排查
