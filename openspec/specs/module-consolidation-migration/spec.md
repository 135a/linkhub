# module-consolidation-migration

## Purpose
定义从多后端模块向单模块 `main` 聚合迁移的过程约束与验收标准。

## Requirements

### Requirement: 既有业务能力必须迁移并收敛到 main 模块
系统 MUST 将 `admin` 与 `project` 中仍需保留的业务能力迁移到 `main` 模块，并保持迁移后外部 API 语义与核心业务行为可验证。

#### Scenario: 迁移后核心功能可用
- **WHEN** 完成代码迁移并启动 `main` 模块
- **THEN** 短链接创建、查询、跳转等核心能力 SHALL 按既定行为正常工作

#### Scenario: 重复实现被收敛
- **WHEN** 迁移完成后进行代码审查或静态检查
- **THEN** 系统 MUST 不再存在跨模块重复的同类业务实现

### Requirement: 迁移过程必须分阶段并具备回滚点
系统 MUST 按“结构收敛、代码迁移、依赖清理、回归验证”阶段推进，并在每个阶段形成可回滚的稳定状态。

#### Scenario: 阶段验收通过后再进入下一阶段
- **WHEN** 当前阶段尚未通过编译与关键路径验证
- **THEN** 流程 MUST 阻止进入下一迁移阶段

#### Scenario: 阶段失败可回退
- **WHEN** 某阶段出现启动失败或关键回归失败
- **THEN** 团队 SHALL 回退至上一阶段稳定版本并重新执行迁移
