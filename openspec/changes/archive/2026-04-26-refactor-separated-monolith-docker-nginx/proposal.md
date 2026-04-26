## Why

当前项目的前后端边界、部署方式与品牌表达不够聚焦于“个人实习项目”的工程目标，导致可维护性、可测试性与可观测性不足。现在重构为前后端分离的单体架构并统一容器化部署，可以显著提升系统稳健性、交付一致性与量化评估能力。

## What Changes

- 将现有项目重构为前后端分离的单体：后端提供稳定 API，前端独立构建与发布，仓库仍保持单体协作流程。
- 引入 Docker / Docker Compose 本地与生产一致化运行方案，规范服务编排、环境变量与健康检查。
- 新增 Nginx 反向代理与静态资源托管配置，将域名 `shortlink.nym.asia` 映射到前端访问路径，并转发 API 请求。
- 清理页面与文案中的营销/人设信息（如“拿个Offer”“马丁”等），统一为中性、项目导向表达。
- 建立可测试与可度量的工程基线：覆盖关键路径测试、可观测日志与核心业务指标采集。

## Capabilities

### New Capabilities
- `frontend-backend-separation`: 定义前后端分离单体的目录边界、通信契约与构建发布约束。
- `dockerized-deployment`: 提供基于 Docker 的多服务编排、环境隔离与一致化运行能力。
- `nginx-domain-routing`: 支持 `shortlink.nym.asia` 到前端路由映射，并安全转发后端 API。
- `project-neutral-branding`: 清理营销化内容并统一为面向求职作品集的中性表达规范。
- `testability-observability-baseline`: 建立测试分层、关键健康探针与基础指标采集能力。

### Modified Capabilities
- 无（当前仓库中未发现可复用的既有 capability 规格，后续以新增 specs 为主）。

## Impact

- 代码结构：影响前端工程目录、后端服务入口、共享配置与构建脚本。
- 运行与部署：新增 Dockerfile、Compose 编排、Nginx 配置与环境变量管理策略。
- 域名与网络：涉及 `shortlink.nym.asia` 的路由、TLS（如适用）与反向代理路径规则。
- 内容与产品表达：影响前端文案、README/文档中的项目定位与对外描述。
- 质量保障：新增/调整测试脚本、CI 检查项与运行指标输出。
