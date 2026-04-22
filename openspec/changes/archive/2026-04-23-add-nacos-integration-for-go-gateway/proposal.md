# Add Nacos Integration for Go Gateway

## Why

当前 Go 网关的路由配置是硬编码在 YAML 文件中的，无法动态感知后端服务的变化。当 Java 服务扩缩容、迁移或重启时，需要手动更新网关配置并重启。

通过集成 Nacos 作为服务发现和配置中心，网关可以：
- 自动发现后端服务的最新地址
- 支持服务动态扩缩容
- 降低配置维护成本

## What Changes

1. Go 网关启动时连接 Nacos，获取后端服务实例列表
2. 优先使用 Nacos 返回的服务地址进行路由转发
3. 如果 Nacos 不可用或服务未注册，降级使用本地 YAML 配置的默认地址
4. 支持定时刷新服务列表，保持最新状态

## Capabilities

### 1. Nacos 服务发现
- 网关启动时连接 Nacos
- 根据服务名称查询实例列表
- 支持负载均衡选择实例
- 定时刷新服务列表

### 2. 降级策略
- Nacos 不可用时使用本地配置
- 服务未注册时使用默认地址
- 记录降级日志方便排查

### 3. 配置管理
- Nacos 连接参数配置
- 服务映射关系配置
- 刷新间隔配置

## Impact

### Affected Code
- `short-link-gateway-go/internal/config/` - 新增 Nacos 配置
- `short-link-gateway-go/internal/nacos/` - **新建** Nacos 客户端
- `short-link-gateway-go/internal/proxy/router.go` - 修改路由逻辑
- `short-link-gateway-go/config.yaml` - 新增 Nacos 配置段
- `short-link-gateway-go/go.mod` - 引入 Nacos SDK

### Affected Services
- Go 网关 - 主要改动
- Nacos - 使用已有实例，无需额外配置
