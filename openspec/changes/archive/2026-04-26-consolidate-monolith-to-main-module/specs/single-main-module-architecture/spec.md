## ADDED Requirements

### Requirement: 系统必须以单一 Spring Boot 模块作为唯一运行单元
系统 MUST 仅保留一个可独立运行的 Spring Boot `main` 模块，作为唯一启动入口与业务承载单元；运行时不得依赖其他后端业务模块。

#### Scenario: 单模块启动成功
- **WHEN** 维护者执行单体应用启动命令
- **THEN** 系统 SHALL 仅启动 `main` 模块并提供完整后端能力

#### Scenario: 多模块入口被禁止
- **WHEN** 构建或启动流程检测到额外后端模块入口
- **THEN** 流程 MUST 明确失败并提示仅允许 `main` 作为启动单元

### Requirement: 系统必须移除分布式治理依赖
系统 MUST 不包含网关层、Nacos 注册/配置中心以及远程服务调用机制；所有业务调用 SHALL 在单进程内通过本地代码完成。

#### Scenario: 构建时无分布式依赖
- **WHEN** 执行依赖解析与打包
- **THEN** 构建结果 MUST 不包含网关、Nacos、RPC/Feign 等分布式治理依赖

#### Scenario: 业务调用仅限本地
- **WHEN** 核心业务服务执行调用链
- **THEN** 调用路径 SHALL 不经过远程服务发现、远程代理或网络跳转
