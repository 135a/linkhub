## ADDED Requirements

### Requirement: 系统必须提供用于面试演示的 JMeter 压测计划文件
系统 MUST 在 `test/jmeter/` 目录下提供以下文件：`create-qps-test.jmx`（短链接创建 QPS 压测与限流验证）、`redirect-cache-test.jmx`（重定向缓存命中率验证）。两个文件 MUST 均为可直接用 JMeter 5.x 打开并执行的标准 `.jmx` 格式，且均使用用户自定义变量（User Defined Variables）管理 `host`、`port`、`gid` 等可变参数。

#### Scenario: 创建 QPS 压测计划可正常加载并运行
- **WHEN** 测试人员用 JMeter 打开 `create-qps-test.jmx` 并点击运行
- **THEN** JMeter SHALL 能向目标服务发起短链接创建请求，并在结果中区分成功响应（HTTP 200）和被限流响应（HTTP 429 或自定义错误码）

#### Scenario: 重定向缓存命中率压测计划可正常加载并运行
- **WHEN** 测试人员用 JMeter 打开 `redirect-cache-test.jmx` 并点击运行
- **THEN** JMeter SHALL 能向目标服务发起重定向请求，并在聚合报告中展示响应时间（P50/P95/P99）和吞吐量（QPS）

#### Scenario: 压测计划支持参数化配置
- **WHEN** 测试人员修改 JMeter 文件顶部的 User Defined Variables 中的 `host` 和 `port`
- **THEN** 所有线程组中的 HTTP 请求 MUST 自动使用新的 host 和 port，无需逐一修改

### Requirement: 系统必须提供面试测试文档
系统 MUST 在 `test/jmeter/README.md` 中提供完整的面试测试操作手册，内容 MUST 包含：JMeter 安装要求、测试前置条件（服务启动、预热步骤）、各测试场景的操作步骤、预期指标数值区间（如缓存命中率预期 >90%，重定向 P99 < 50ms）、JMeter 结果解读说明、如何实时查看指标摘要接口。

#### Scenario: 面试测试文档涵盖所有核心指标场景
- **WHEN** 面试官提问关于缓存命中率、QPS、限流、布隆过滤器任一指标
- **THEN** 测试文档 MUST 包含对应的测试操作步骤和预期数值区间，使演示者能在 2 分钟内完成该项指标的现场演示

#### Scenario: 测试文档包含预热步骤说明
- **WHEN** 面试演示开始前
- **THEN** 文档 MUST 说明如何通过运行重定向压测进行缓存预热，以及预热完成的判断标准（如缓存命中率稳定在 90% 以上）

### Requirement: 压测计划必须覆盖布隆过滤器拦截验证场景
`redirect-cache-test.jmx` MUST 包含一个专用线程组，专门向服务发送大量不存在的 shortUri 请求，用于触发布隆过滤器拦截，并通过 `/api/short-link/v1/metrics/summary` 接口验证 `bloomFilterInterceptCount` 计数上升。

#### Scenario: 无效 shortUri 批量请求触发布隆过滤器拦截统计
- **WHEN** JMeter 向服务发送 100 个不存在的 shortUri 请求（如随机生成的 6 位字符串）
- **THEN** `/api/short-link/v1/metrics/summary` 中的 `bloomFilterInterceptCount` MUST 增加，增量 SHALL 接近请求数量（允许 ±5% 误差，因部分请求可能命中数据库）

#### Scenario: 有效短链接请求与无效请求在压测报告中可区分
- **WHEN** 压测完成后查看 JMeter 聚合报告
- **THEN** 有效重定向请求组（HTTP 302）和无效请求组（重定向至 /page/notfound）SHALL 在不同线程组中分别统计，响应码可被区分
