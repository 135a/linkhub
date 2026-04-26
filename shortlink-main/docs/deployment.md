# 部署与回滚指南

## 环境变量

复制 `.env.example` 为 `.env`，按环境修改。

关键项：

- `DOMAIN=shortlink.nym.asia`
- `VITE_API_BASE_URL=/api/short-link/v1`
- `SPRING_PROFILES_ACTIVE=dev`

## 本地部署

1. `mvn -DskipTests clean package`
2. `docker compose up -d --build`
3. 执行 `scripts/smoke-compose-health.ps1` 校验单体栈服务状态与健康检查
4. 执行 `scripts/smoke-nginx.ps1` 做反向代理基础冒烟

## 预发布部署

1. 使用与生产一致的镜像标签
2. 在预发布域名完成全链路验证
3. 核查关键指标（4xx/5xx、响应延迟、短链创建成功率）
4. 将演练结果记录在 `docs/preprod-drill.md`

## 生产部署

1. 更新镜像标签并拉起新版本
2. 验证 `shortlink.nym.asia` 首页、登录、短链关键接口
3. 观察 30 分钟核心指标后再确认切流完成

## 回滚策略

1. 回滚到上一个稳定镜像标签
2. 回滚 `project` 与前端对应配置到上一版本
3. 复测首页访问与 `/api` 关键路径
