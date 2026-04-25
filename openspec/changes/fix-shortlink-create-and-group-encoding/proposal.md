## 背景

两个关键运行时 Bug 阻断了开发环境的核心功能：
1. 创建短链接始终返回 `B000001 系统执行出错`，原因是 `ShortLinkCreateInitTxProducer` 和 `FaviconUpdateProducer` 的 Java 源文件丢失，导致 `@Transactional` 方法内抛出异常并回滚数据库写入。
2. 分组名称包含中文时，前端显示乱码，原因是 Go 网关转发请求头时未进行 URL 编码，而 `UserTransmitFilter` 无条件对请求头做 URL 解码，非 ASCII 字符可能被错误处理。

## 变更内容

- **恢复缺失的 MQ 生产者源文件**：重新创建 `ShortLinkCreateInitTxProducer.java` 和 `FaviconUpdateProducer.java`，使 Spring 容器能正确注入，`createShortLink()` 事务方法得以完整执行。
- **修复中文乱码根因**：先查询数据库确认数据是否已乱码，再决定是修复 MySQL 字符集配置还是修复请求头编码逻辑。
- **验证 `UserTransmitFilter` 解码逻辑**：确保过滤器仅对实际经过 URL 编码的请求头执行解码（即含 `%` 时才解码），避免未来重复解码的回归问题。
- **重新构建并重新部署 `short-link-project` 和 `short-link-gateway-go` 镜像**。

## 能力范围

### 新增能力
*（无——此次均为 Bug 修复，恢复既有功能）*

### 修改能力
- `short-link-create`：恢复缺失的 MQ 生产者源文件，使创建短链接的完整流程端到端可用。
- `gateway-auth`：修复网关在转发用户身份请求头时的编码问题，解决中文分组名乱码。

## 影响范围

- **`short-link-java/project/src/…/mq/producer/`**：新增两个源文件。
- **`short-link-gateway-go/internal/middleware/auth.go`**：按需对 `username`、`user-name`、`real-name` 等请求头进行 URL 编码后再转发。
- **`short-link-java/admin/src/…/common/biz/user/UserTransmitFilter.java`**：在解码前添加 `contains("%")` 判断保护。
- Docker 镜像：需重新构建 `short-link-project` 和 `short-link-gateway-go`。
- 无数据库 Schema 变更。
- 无外部 API 契约变更。
