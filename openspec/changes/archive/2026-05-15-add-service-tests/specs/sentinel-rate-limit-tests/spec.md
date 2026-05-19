## ADDED Requirements

### Requirement: @RateLimit 注解快速失败模式
系统 SHALL 验证限流触发后快速返回错误而非阻塞等待。

#### Scenario: QPS 超阈值触发快速失败
- **WHEN** 配置 limitType=FAST_FAIL，QPS=1，并在极短时间内发起 3 次请求
- **THEN** 第 1 次请求正常返回
- **AND** 第 2、3 次请求返回限流错误（非 500）
- **AND** 错误消息为约定的限流提示

#### Scenario: 未超阈值时正常放行
- **WHEN** 请求频率低于 QPS 阈值
- **THEN** 所有请求均正常通过

### Requirement: @RateLimit 注解漏桶排队模式
系统 SHALL 验证 `CONTROL_BEHAVIOR_RATE_LIMITER` 模式下请求排队而非直接丢弃。

#### Scenario: 漏桶模式超出阈值请求排队
- **WHEN** 配置 limitType=RATE_LIMITER，QPS=1
- **THEN** 超出阈值的请求进入排队等待
- **AND** 所有请求最终均被处理（不丢失请求）

### Requirement: @RateLimit 注解预热模式
系统 SHALL 验证 `CONTROL_BEHAVIOR_WARM_UP` 模式下的冷启动行为。

#### Scenario: 预热期低阈值保护
- **WHEN** 配置 limitType=WARM_UP，warmUpPeriodSec=5
- **THEN** 系统在预热期内以较低阈值放行
- **AND** 预热完成后恢复到配置 QPS

### Requirement: 不同接口差异化限流
系统 SHALL 验证不同接口的 QPS 阈值独立生效。

#### Scenario: create 接口 QPS=200，update 接口 QPS=5
- **WHEN** 以 10 QPS 同时调用 create 和 update
- **THEN** create 正常通过
- **AND** update 触发限流
