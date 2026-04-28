## Why

修复系统中存在的三个关键问题：
1. **短链接分组删除异常**：删除分组时出现“分组标识不能为空”的报错，且 UI 状态未能同步刷新。
2. **前端交互与闭环**: 修复创建短链接、新增分组后弹窗不消失的问题。确保无论接口成功还是失败，弹窗都能正确关闭且页面状态得到刷新。
3. **布隆过滤器稳定性**: 解决 Redisson 布隆过滤器在配置冲突时抛出 `Bloom filter config has been changed` 异常的问题。

## 3. 技术方案

### 3.1 后端逻辑
- **分组删除**: 强化 `gid` 校验。
- **字符编码**: 统一全链路 UTF-8 配置。
- **布隆过滤器**: 优化 `RBloomFilterConfiguration`，在初始化时不仅检查是否存在，还需验证现有配置（Size/HashIterations）是否与代码预期一致。若不一致则自动重建。

### 3.2 前端交互
- **弹窗控制**: 在 `MySpaceIndex.vue`、`CreateLink.vue` 等组件中使用 `try...finally` 结构。确保在异步请求结束后，无论结果如何都执行弹窗关闭逻辑。
- **状态刷新**: 在弹窗关闭后统一触发列表刷新逻辑。

## What Changes

- **后端修改**：
  - 检查并修复删除分组接口（Delete Group）的校验逻辑，确保 `gid` 正确传递。
  - 检查数据库连接、Spring Boot 配置以及 API 响应的字符编码设置，确保全程使用 UTF-8。
- **前端修改**：
  - 优化 `console-vue` 中删除分组后的状态更新逻辑，确保即时移除已删除的分组。
  - 修复创建短链接后的回调函数，确保成功后立即关闭对话框并触发列表刷新。

## Capabilities

### Modified Capabilities
- `shortlink-management`: 修复短链接管理流程中的 UI 反馈和后端校验逻辑，支持正确的分组操作和字符显示。

## Impact

- **后端**: `com.nym.shortlink.core.service.impl.GroupServiceImpl` (分组删除逻辑), `application.yaml` (编码配置)
- **前端**: `console-vue` 中的分组列表组件和短链接创建对话框组件。
