## ADDED Requirements

### Requirement: 网关监控大盘页面
Console Vue 前端 SHALL 在短链接管理后台中新增网关监控大盘页面，展示网关实时性能指标。

#### Scenario: 大盘数据展示
- **WHEN** 用户访问网关监控页面
- **THEN** 页面展示 QPS、P50/P90/P99 延迟、错误率、活跃连接数的实时数值

#### Scenario: 自动刷新
- **WHEN** 用户停留在监控页面
- **THEN** 页面每 5 秒自动刷新网关性能指标数据

### Requirement: 日志系统入口
Console 前端 SHALL 提供跳转链接至独立的日志系统前端。

#### Scenario: 跳转日志系统
- **WHEN** 用户在 Console 导航栏或监控页面点击「日志中心」入口
- **THEN** 跳转至日志系统前端地址

### Requirement: 监控图表
Console 监控大盘 SHALL 以图表形式展示最近 1 小时的 QPS 趋势和延迟分布。

#### Scenario: QPS 趋势图
- **WHEN** 用户查看 QPS 趋势图
- **THEN** 展示最近 1 小时的 QPS 折线图

#### Scenario: 延迟分布图
- **WHEN** 用户查看延迟分布图
- **THEN** 展示最近 1 小时的 P50、P90、P99 延迟变化趋势
