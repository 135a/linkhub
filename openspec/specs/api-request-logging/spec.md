# Capability: API Request Logging

## Purpose
提供所有对外暴露 Controller 接口的统一出入日志记录能力，以便日常调试与生产环境排查请求的连通性和执行轨迹。

## Requirements

### Requirement: Controller 接口出入日志记录
系统 MUST 在所有对外暴露的 Controller 接口方法的入口（请求刚进入接口，开始执行业务逻辑或 Service 方法前）以及出口（执行完毕并准备返回结果前），显式地打印信息级别的日志。该日志应能清晰地表明请求的进入和处理完成。

#### Scenario: 请求进入 Controller 接口
- **WHEN** 客户端请求刚打入 Controller 的接口方法，尚未执行底层 Service 逻辑时
- **THEN** 系统会通过 `log.info` 级别输出一条表示请求进入的日志信息，内容包含接口名称或路径。

#### Scenario: 成功执行 Controller 接口并返回响应
- **WHEN** 任意一个 Controller 接口方法处理完毕，即将返回响应结果时
- **THEN** 系统会通过 `log.info` 级别输出一条表示请求处理完毕的日志信息。
