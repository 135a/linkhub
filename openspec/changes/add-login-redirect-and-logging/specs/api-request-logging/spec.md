# Capability: API Request Logging

## Purpose
提供所有对外暴露 Controller 接口的统一出入日志及耗时记录能力，以便日常调试与生产环境排查请求连通性及性能瓶颈。

## MODIFIED Requirements

### Requirement: Controller 接口出入及耗时记录
系统 MUST 在所有对外暴露的 Controller 接口中，自动地在请求进入时打印起始日志，在请求处理完毕时计算总体耗时，并打印包含请求耗时信息的退出日志。系统中不应包含由手工添加在 Controller 业务代码内的冗余进入/退出日志语句。

#### Scenario: 请求进入 Controller 接口
- **WHEN** 客户端请求刚打入 Controller 的接口方法，尚未执行底层 Service 逻辑时
- **THEN** 系统 MUST 自动输出一条表示请求进入的日志信息，内容包含接口或方法名称。

#### Scenario: 成功执行 Controller 接口并返回响应
- **WHEN** 任意一个 Controller 接口方法处理完毕，即将返回响应结果时
- **THEN** 系统 MUST 自动计算该次请求执行的消耗时间（如毫秒数），并输出一条包含该耗时的表示请求处理完毕的日志信息。
