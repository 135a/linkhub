## Context

当前 Go 网关的路由配置使用本地 YAML 文件硬编码后端服务地址：
```yaml
routes:
  - path_prefix: "/api/v1/shortlink"
    target: "http://project:8001"
```

当后端服务（project、admin）扩缩容、迁移或重启时，IP 可能变化，需要手动更新网关配置。

项目中已有 Nacos 作为 Java 服务的服务注册与配置中心，Java 服务已注册到 Nacos。

## Goals / Non-Goals

**Goals:**
- Go 网关从 Nacos 动态获取后端服务地址
- Nacos 不可用时自动降级到本地配置
- 支持定时刷新服务列表
- 最小改动，不影响现有功能

**Non-Goals:**
- 不实现网关自身的注册（网关是入口，不需要被服务发现）
- 不实现复杂的负载均衡算法（简单轮询即可）
- 不实现配置监听（后续可升级）

## Decisions

### 1. 使用 Nacos Go SDK v2
**Why:** Nacos 官方维护的 Go SDK，文档完善，API 简单。

### 2. 降级策略设计
```
优先: Nacos 获取服务地址
  ↓ (如果失败)
降级: 使用 config.yaml 中的默认地址
  ↓ (记录日志)
继续: 正常转发请求
```

**Why:** 保证 Nacos 故障时网关仍能正常工作。

### 3. 缓存 + 定时刷新
- 启动时从 Nacos 获取服务列表，缓存到内存
- 每 30 秒刷新一次
- 请求时使用缓存的地址，不实时查询

**Why:** 避免每次请求都查询 Nacos，降低延迟和 Nacos 负载。

### 4. 配置结构设计
```yaml
nacos:
  enabled: true
  host: "nacos"
  port: 8848
  refresh_interval: 30  # 秒

routes:
  - path_prefix: "/api/v1/shortlink"
    service_name: "shortlink-project"  # Nacos 服务名
    default_target: "http://project:8001"  # 降级地址
```

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| Nacos 启动慢，网关启动时服务未注册 | 使用降级地址，等待下一次刷新 |
| 服务列表变化时有短暂不一致 | 30 秒刷新间隔可接受，不影响核心功能 |
| Nacos SDK 增加依赖体积 | 约 10MB，可接受 |
| 4GB 服务器内存压力 | Nacos 已在使用，无额外服务 |
