# 面试展示结果包（单体项目）

## 1) 测试报告

### 后端关键路径测试

- 模块：`project`
- 命令：`mvn -pl project -Dtest=ShortLinkControllerTest test`
- 覆盖点：
  - 短链创建成功返回
  - 短链跳转请求链路（控制层委托）
  - 错误处理返回统一失败码

### 反向代理与编排冒烟

- 容器健康检查：`scripts/smoke-compose-health.ps1`
- Nginx/API 冒烟：`scripts/smoke-nginx.ps1`

## 2) 关键指标快照（模板）

> 采样窗口：最近 24 小时（预发布或生产）

- 短链创建成功率：`<填写数值>%`
- 短链跳转成功率：`<填写数值>%`
- 4xx 占比：`<填写数值>%`
- 5xx 占比：`<填写数值>%`
- 延迟分位数：
  - P50：`<填写数值> ms`
  - P95：`<填写数值> ms`
  - P99：`<填写数值> ms`

## 3) 架构说明入口

- 架构与链路：`docs/architecture.md`
- 部署与回滚：`docs/deployment.md`
- 预发布演练记录：`docs/preprod-drill.md`
- 可观测基线：`docs/observability.md`

## 4) 演示建议顺序（5 分钟）

1. 先讲单体架构边界（前端、反向代理、后端）
2. 演示 `docker compose up -d` 与健康检查脚本
3. 演示短链创建与访问跳转
4. 展示测试与指标快照，说明质量门禁
