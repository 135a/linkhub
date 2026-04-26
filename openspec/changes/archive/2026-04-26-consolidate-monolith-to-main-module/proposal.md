## Why

当前仓库同时存在 `admin`、`project`、`gateway` 等多个模块，职责边界重叠且部署、调试和交付链路复杂，已经与“单体项目”目标不一致。需要尽快收敛为单模块架构，降低维护成本并提升团队对代码结构的理解一致性。

## What Changes

- 将 `admin`、`project`、`gateway` 的有效业务能力整合到统一的 `main` 模块中，形成单体运行形态。
- **BREAKING**：删除 `gateway` 模块及其独立启动/构建路径，相关网关职责迁移至 `main` 内部实现或入口层配置。
- **BREAKING**：删除与主链路无关、重复或历史遗留模块，调整 Maven 聚合与模块引用关系，仅保留 `main` 为核心业务模块。
- 统一应用配置、构建脚本、容器编排和文档入口，确保开发、测试、部署均以单模块为标准流程。
- 清理失效依赖与冗余目录，确保最终项目结构清晰、可读、可维护。

## Capabilities

### New Capabilities
- `single-main-module-architecture`: 定义系统仅保留 `main` 单模块的架构约束、目录规范与运行边界。
- `module-consolidation-migration`: 定义从多模块向 `main` 聚合迁移的行为要求（代码迁移、依赖收敛、配置对齐）。
- `irrelevant-module-pruning`: 定义无关模块识别与删除后的完整性要求（构建通过、启动可用、关键链路不丢失）。

### Modified Capabilities
- `dockerized-deployment`: 调整部署规格以匹配单模块产物与镜像/编排方式。
- `frontend-backend-separation`: 调整前后端集成方式，移除对多后端模块拓扑的依赖，统一指向 `main`。
- `testability-observability-baseline`: 更新可测试性与可观测性基线，确保在单模块形态下维持原有监控与验证能力。

## Impact

- 受影响代码：`shortlink-main` 下 Maven 聚合配置、各模块源码与配置文件、构建脚本、容器与部署配置。
- 受影响系统：本地开发环境、CI 构建流程、容器化部署路径、联调与回归测试流程。
- 受影响接口：模块间内部调用会被重组为单模块内调用，外部 API 语义应保持兼容（如有变更需在后续 specs 明确）。
- 风险与依赖：需要分阶段迁移与回归验证，重点关注启动流程、路由入口、认证链路和短链接核心读写路径。
