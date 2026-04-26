# dockerized-deployment

## Purpose
约束容器化部署的最小编排能力与健康可探测性，确保环境一致性与发布可验证性。

## Requirements

### Requirement: 系统必须支持基于 Docker Compose 的一键编排运行
系统 MUST 提供可执行的 Docker Compose 编排文件，至少包含 `backend`（即独立 Spring Boot 单体应用）与其必要配套服务，并定义依赖关系、网络与必要环境变量；编排中 MUST NOT 要求 `gateway` 服务。

#### Scenario: 本地一键启动成功
- **WHEN** 维护者执行 `docker compose up -d`
- **THEN** 三个核心服务 SHALL 在同一编排网络中启动并可互相通信

#### Scenario: 缺失关键环境变量时显式失败
- **WHEN** 启动时缺失运行所需关键环境变量
- **THEN** 编排 MUST 显式报错并阻止不完整配置的服务进入可用状态

### Requirement: 服务健康状态必须可探测
系统 MUST 为单体后端服务提供健康检查，以支持启动顺序控制、故障发现与自动化验证；健康探测链路 MUST NOT 依赖网关探活或服务注册中心。

#### Scenario: 后端健康检查失败
- **WHEN** 后端健康检查端点连续失败达到阈值
- **THEN** 编排与监控 SHALL 将后端标记为不健康并触发告警或重试策略

#### Scenario: 发布前执行容器健康冒烟检查
- **WHEN** 新版本镜像完成构建
- **THEN** 流程 MUST 执行容器级健康冒烟检查并仅在通过后允许继续部署
