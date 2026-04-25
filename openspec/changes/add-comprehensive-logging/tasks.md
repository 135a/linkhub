## 1. Java 服务日志增强 (short-link-java)

- [x] 1.1 在 `shortlink-trace-starter` 模块中引入 `spring-boot-starter-aop` 依赖。
- [x] 1.2 创建 `@NoLog` 自定义注解，用于排除特定方法的日志记录。
- [x] 1.3 实现 `LogAspect` 切面类，拦截 Controller 和 Service 层方法。
- [x] 1.4 在 `LogAspect` 中实现 method 入参、出参及执行耗时的 `DEBUG` 级别记录。
- [x] 1.5 实现基于字段名（如 `password`）的敏感数据脱敏逻辑。
- [x] 1.6 在 `shortlink-trace-starter` 中配置全局异常处理器或在切面中统一记录 `ERROR` 日志。
- [x] 1.7 更新各模块（admin, project）的 `application.yml`，配置日志级别和 AOP 启用状态。

## 2. Go 网关日志增强 (short-link-gateway-go)

- [x] 2.1 在 `go.mod` 中引入 `github.com/natefinch/lumberjack` 以支持日志滚动（若未引入）。
- [x] 2.2 在 `internal/log` 包中重构日志客户端，集成 `uber-go/zap` 结构化日志库。
- [x] 2.3 在 `internal/middleware/traceid.go` 中重构 `AccessLogger` 中间件，使用 `zap` 记录结构化访问日志。
- [x] 2.4 实现 Go 端的敏感字段脱敏中间件或在 `zap` 编码器中处理。
- [x] 2.5 更新 `config.yaml`，增加日志级别（level）、路径（path）和滚动配置。
- [x] 2.6 在网关的关键业务逻辑（如路由匹配、限流拦截）中添加 `DEBUG` 级日志。

## 3. 测试与验证

- [x] 3.1 验证开发环境下（dev profile）是否输出了详尽的 `DEBUG` 日志。
- [x] 3.2 验证生产环境下是否仅输出 `INFO` 及以上级别的日志。
- [x] 3.3 验证敏感字段（如登录接口的密码）在日志中是否已正确脱敏。
- [x] 3.4 压力测试 AOP 开启后的系统响应延时，确保性能损耗在可接受范围内。
