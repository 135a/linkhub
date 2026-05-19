# 预发布端到端演练记录（单体项目）

## 演练信息

- 环境：preprod（与生产同镜像标签）
- 架构：单体仓库 + 前后端分离部署（`frontend` / `backend(project)` / `nginx`）
- 入口域名：`example.com`（预发布等价域名）
- 演练时间：2026-04-26

## 演练步骤

1. 执行 `docker compose up -d --build`
2. 执行 `scripts/smoke-compose-health.ps1`
3. 执行 `scripts/smoke-nginx.ps1`
4. 手工验证登录、短链创建、短链访问跳转

## 验证结果

- 容器状态：`frontend`、`project`、`rocketmq` 组件均为 running/healthy
- 反向代理验证：`/healthz`、首页、`/api` 转发均通过
- 关键链路：登录成功；创建短链成功；短链跳转返回目标页
- 异常场景：不可用短链返回 notfound 页面，状态可识别

## 风险与回滚结论

- 未发现阻断上线问题
- 若生产异常，按 `docs/deployment.md` 的镜像与单体配置回滚流程执行
