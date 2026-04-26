## 原因 (Why)

系统目前将 `admin`（用户和分组管理）与 `project`（短链接核心处理）拆分为不同的模块，它们之间通过 Feign 进行网络通信。将 `admin` 模块合并到 `core`（`main`）模块中，可以极大地简化整体系统架构，消除远程网络调用的开销，降低部署的复杂性，并集中处理诸如全局异常处理和各项配置等通用逻辑。这更适合当前系统的规模。

## 变更内容 (What Changes)

- **BREAKING**: 完全移除独立的 `admin` 模块。
- 将所有来自 `admin` 模块的控制器(Controller)、服务(Service)、数据访问层(DAO)和数据传输对象(DTO)（例如：User, Group, RecycleBin相关逻辑）迁移至 `main` 模块的 `core` 包下（`com.nym.shortlink.core`）。
- 将原本的远程 Feign 调用（例如 `ShortLinkActualRemoteService`）替换为本地 Spring Bean 服务的直接注入与调用。
- 合并重复的基础设施类，例如 `GlobalExceptionHandler`、`MyMetaObjectHandler`、`RBloomFilterConfiguration` 以及其他通用工具类。
- 在同一个 Spring 应用上下文中，统一数据库、Redis 以及 ShardingSphere 的相关配置。
- 更新部署脚本和 Docker Compose 配置文件，以适配单体后端服务的部署模式。

## 能力 (Capabilities)

### 新增能力 (New Capabilities)

- `module-consolidation`: 架构重构能力，将整个短链接系统整合为一个统一的单体服务运行，使用本地服务调用替代远程 Feign 通信，并统一全局配置。

### 修改的能力 (Modified Capabilities)

*(无)*

## 影响范围 (Impact)

- **代码**: `admin` 目录将被完全删除。`main` 模块将吸收其所有业务逻辑。Feign 客户端接口将被删除并替换为本地服务逻辑。
- **API**: 现有的 API 接口端点在功能上保持完全一致，但它们将统一由单个应用端口提供服务，而不再分散在 admin 和 project 的不同端口中。
- **依赖**: 如果 Spring Cloud OpenFeign 及其相关的路由依赖不再被其他服务使用，可以将其移除以减轻依赖负担。
- **系统**: 部署架构从微服务模式转变为单体模式，因此需要同步更新 `docker-compose.yml`、部署脚本以及 Nginx 的路由配置。
