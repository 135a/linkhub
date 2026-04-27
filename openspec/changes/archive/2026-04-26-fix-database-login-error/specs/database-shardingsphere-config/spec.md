# Capability: Database ShardingSphere Config

## Purpose
后端 ShardingSphere 数据源初始化与配置规范，确保应用具备正确且可用的数据库连接池及分片路由配置。

## ADDED Requirements

### Requirement: 提供完整的 ShardingSphere 路由和数据源配置
系统 MUST 包含有效且与环境（dev/prod 等）相匹配的 ShardingSphere 配置文件（如 `shardingsphere-config-dev.yaml`），该文件内部须包含被正确声明的数据源和相关的分表分库（或读写分离）规则，以避免因为根配置解析为空而导致的空指针及框架层执行瘫痪。

#### Scenario: Mybatis 及连接池初始化与执行
- **WHEN** 系统启动或用户发起数据库相关操作（如登录），触发 ShardingSphere DataSource 获取及路由时
- **THEN** 系统 MUST 能够从预置的文件中成功解析出数据源集合（不会出现 `rootConfig is null` 错误），以保证 Mybatis 能够正常获取连接并执行查询。
