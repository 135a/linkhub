## Why

在访问短链接分组列表接口（`GET /api/short-link/admin/v1/group`）时，由于 MyBatis-Plus 查询 `t_group` 表时分片键 `username` 参数为空（`null`），导致 ShardingSphere 路由分片失败，抛出 `java.sql.SQLException: Sharding value 'null' must implements Comparable` 异常。我们需要修复此问题以保证用户能够正常获取其创建的分组列表。

## What Changes

- 修改分组查询逻辑，在执行 `t_group` 表的查询操作时，确保正确获取并传入当前登录用户的 `username` 作为分片键。
- 检查并验证 `UserContext` 或其他用于传递用户信息的上下文中是否正确初始化并包含了 `username` 属性。
- 确保相关接口（如分组列表查询）调用时，上下文中的用户信息不丢失。

## Capabilities

### New Capabilities

- 无。本次为纯技术性 Bug 修复，不涉及新增产品能力规格。

### Modified Capabilities

- 无。本次为纯技术性 Bug 修复，不改变原有的产品规格和业务逻辑。

## Impact

- **受影响的代码**：`GroupServiceImpl`（特别是 `listGroup` 方法及其底层调用的 Mapper 查询）、可能的 `UserTransmitFilter` 或 `UserContext` 相关上下文管理代码。
- **受影响的系统功能**：管理后台短链接分组列表查看功能。
- **不兼容的变更 (Breaking Changes)**：无。
