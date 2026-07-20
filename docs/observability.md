# 可观测性与测试基线

## 结构化日志字段

后端日志至少包含：

- `traceId`: 请求追踪标识
- `path`: 请求路径
- `method`: HTTP 方法
- `status`: 响应状态码
- `durationMs`: 请求耗时
- `errorCode`: 业务错误码（如有）

## 核心指标

- 短链创建成功率
- 短链跳转成功率
- 4xx / 5xx 占比
- 接口响应延迟（P50 / P95 / P99）

当前在 `project` 服务通过 Micrometer 暴露以下基础指标（Prometheus）：

- `shortlink_create_requests_total` / `shortlink_create_success_total`（创建成功率）
- `shortlink_redirect_requests_total` / `shortlink_redirect_success_total`（跳转成功率）
- `shortlink_http_status_4xx` / `shortlink_http_status_5xx`（4xx/5xx 比例）
- `shortlink_create_latency` / `shortlink_redirect_latency`（包含 P50/P95/P99）

采集端点：`/actuator/prometheus`

## 测试分层

- 后端：关键控制器/服务单元与集成测试
- 前端：核心页面渲染、登录流程、错误提示
- 反向代理：Nginx 反代与深链回退冒烟

## 面试展示建议

- 保留最近一次 CI 报告截图
- 输出关键指标快照（近 24 小时）
- 准备 1 页架构与请求链路说明图
