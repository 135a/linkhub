## 1. 基础设施准备

- [x] 1.1 在 go.mod 中引入 Nacos Go SDK v2 依赖
- [x] 1.2 更新 config.yaml，新增 Nacos 配置段和服务映射
- [x] 1.3 更新 Config 结构体，支持 Nacos 配置加载

## 2. Nacos 客户端实现

- [x] 2.1 创建 `internal/nacos/client.go`，实现 Nacos 客户端初始化
- [x] 2.2 实现 `GetServiceInstances(serviceName)` 方法，查询服务实例
- [x] 2.3 实现服务实例缓存，支持定时刷新（goroutine + ticker）
- [x] 2.4 实现健康检查过滤，只返回健康实例
- [x] 2.5 实现简单轮询负载均衡

## 3. 路由逻辑改造

- [x] 3.1 修改 `proxy/router.go`，优先从 Nacos 获取服务地址
- [x] 3.2 实现降级逻辑：Nacos 失败时使用 default_target
- [x] 3.3 添加降级日志（WARNING 级别）
- [x] 3.4 更新 main.go，初始化 Nacos 客户端并启动刷新

## 4. 测试与验证

- [x] 4.1 编写 Nacos 客户端单元测试（mock Nacos 响应）
- [x] 4.2 编写降级逻辑单元测试（模拟 Nacos 不可用）
- [x] 4.3 本地验证：启动 Nacos + Java 服务 + Go 网关，验证服务发现
- [x] 4.4 本地验证：停止 Nacos，验证降级到本地配置