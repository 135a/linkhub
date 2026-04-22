## ADDED Requirements

### Requirement: TraceID 注入
短链接服务 SHALL 在响应头中返回 `X-Trace-ID`，使客户端可追踪请求链路。

#### Scenario: 响应包含 TraceID
- **WHEN** 网关处理完请求并向下游转发
- **THEN** 下游服务的响应头包含该请求的 `X-Trace-ID`

### Requirement: Java 端 TraceID 透传
短链接 Java 服务 SHALL 通过 Spring MVC Interceptor 从请求头读取 `X-Trace-ID` 并注入到 SLF4J MDC 中。

#### Scenario: MDC 注入
- **WHEN** 请求到达短链接服务且携带 `X-Trace-ID` 头
- **THEN** 该请求产生的所有日志中 `trace_id` 字段与该 TraceID 一致

#### Scenario: 无 TraceID 时生成
- **WHEN** 请求到达短链接服务但未携带 `X-Trace-ID` 头（直连调用场景）
- **THEN** 服务自行生成 TraceID 并注入 MDC

### Requirement: 日志格式统一
短链接 Java 服务 SHALL 输出 JSON 格式的结构化日志，包含 level、timestamp、trace_id、service、message、thread 字段。

#### Scenario: JSON 日志输出
- **WHEN** 短链接服务产生日志
- **THEN** 日志输出为单行 JSON，字段符合统一 Schema

### Requirement: Spring Boot Starter
短链接服务 SHALL 通过引入 Spring Boot Starter 依赖自动完成 TraceID 拦截器和日志格式化的配置。

#### Scenario: Starter 自动装配
- **WHEN** 项目在 pom.xml 中添加 Starter 依赖
- **THEN** 自动注册 TraceID 拦截器和 JSON 日志编码器，无需额外配置
