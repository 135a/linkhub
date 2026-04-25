## 背景

`short-link-project` 服务和 Go API 网关 `short-link-gateway-go` 各存在一个运行时 Bug，阻断了两个核心流程：

**Bug 1：短链接创建失败**
`ShortLinkServiceImpl.createShortLink()` 被 `@Transactional` 注解修饰，其中注入了两个 Spring Bean（`ShortLinkCreateInitTxProducer` 和 `FaviconUpdateProducer`）。这两个类的 `.java` 源文件被意外删除，但编译后的 `.class` 文件仍存在于 Docker 镜像的 `app.jar` 中（已通过 `javap` 反编译确认），因此 Spring 容器启动正常。然而一旦在事务方法内部抛出任何异常，`@Transactional` 会回滚 `t_link` 和 `t_link_goto` 的数据库写入，导致前端收到 B000001 错误。

**Bug 2：中文分组名乱码**
`UserTransmitFilter.java` 无条件对 `username` 请求头执行 `URLDecoder.decode(userName, UTF_8)`。Go 网关目前直接转发原始（未编码）的请求头值。乱码的根因需要先通过查询数据库确认——如果是 MySQL 字符集配置问题（如连接字符集非 utf8mb4），则修复字符集；如果数据库存储正常，则是前端渲染或 HTTP 响应编码问题。

**通过 `javap` 反编译确认的关键信息：**
- `ShortLinkCreateInitTxProducer.sendCreateInitTransaction(String fullShortUrl, String gid)` → 返回 `SendResult`
- `FaviconUpdateProducer.send(String fullShortUrl, String originUrl)` → 返回 `void`
- 两者均依赖 `RocketMQTemplate`，Topic 通过 `@Value` 注入
- 从容器日志确认的 Topic 名称：
  - tx-init: `short-link_project-service_tx-init_topic`
  - favicon: `short-link_project-service_favicon-update_topic`

## 目标 / 非目标

**目标：**
- 在源码树中恢复 `ShortLinkCreateInitTxProducer.java` 和 `FaviconUpdateProducer.java`，实现与编译类完全一致的方法签名。
- 排查并修复中文分组名乱码问题（MySQL 字符集 或 请求头编码）。
- 重新构建 `short-link-project` 镜像，验证短链接创建端到端通过。

**非目标：**
- 重构 MQ 拓扑或切换为事务性发件箱模式。
- 修改 RocketMQ Topic 命名规范。
- 变更消费者逻辑（`ShortLinkCreateInitTxConsumer`、`FaviconUpdateConsumer`）。

## 技术决策

### 决策 1：从反编译签名还原生产者源文件

**方案：** 参照现有 `ShortLinkStatsSaveProducer.java` 的代码模板，手动编写两个生产者类，使方法签名与反编译结果一致。

**理由：** `javap` 输出已提供明确的类签名，无需猜测。两个生产者均使用 `RocketMQTemplate`，与所有现有生产者保持一致。

**需要在 Nacos 配置中新增的属性键（待验证实际 key 名称）：**
- 控制 tx-init topic 的属性 key（字段名 `txTopic`）
- 控制 favicon topic 的属性 key（字段名 `faviconTopic`）

### 决策 2：`ShortLinkCreateInitTxProducer` 使用事务消息发送

**方案：** 使用 `rocketMQTemplate.sendMessageInTransaction()`，而非普通的 `syncSend()`。

**理由：** JAR 中存在 `ShortLinkCreateInitTxListener` 类，这是 RocketMQ 事务消息的本地事务监听器回查机制的标准配套组件，说明该生产者必须使用事务消息发送模式。

### 决策 3：先查数据库，再决定乱码修复路径

**方案：** 执行 `SELECT HEX(name) FROM t_group_0` 判断数据是否已在 DB 层面乱码。

**两条修复路径：**
- **数据库乱码** → 修复 MySQL 连接字符集（确保 `CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`），重新初始化数据。
- **数据库正常** → 检查 HTTP 响应的 `Content-Type` 是否包含 `charset=UTF-8`，或检查前端解析逻辑。

**不优先修改请求头编码逻辑**：`UserTransmitFilter` 的 `URLDecoder.decode` 对于不含 `%` 的纯 UTF-8 字符串是安全的，当前不会导致乱码。

## 风险 / 权衡

- **[风险] Nacos 中属性 key 名称不匹配** → `@Value` 注入后字段为 null，调用 `syncSend(null, ...)` 会触发 NPE。缓解措施：重新部署后立即检查容器日志，确认生产者初始化无报错。

- **[风险] 事务监听器未正确注册** → RocketMQ Broker 会持续回查本地事务状态，导致消息堆积或超时回滚。缓解措施：检查 JAR 中 `ShortLinkCreateInitTxListener` 的注解，确保其与 `ShortLinkCreateInitTxProducer` 使用同一个 `RocketMQTemplate` 实例。

- **[风险] 乱码根因判断有误** → 如果乱码在 DB 层面不存在，则字符集修复无效，需要继续排查响应编码。缓解措施：严格按先查 DB 数据、再逐层排查的顺序执行。

## 部署步骤

1. 新增 `ShortLinkCreateInitTxProducer.java` 和 `FaviconUpdateProducer.java` 到 `short-link-java/project/src/…/mq/producer/`。
2. 确认 Nacos 配置中存在对应的 Topic 属性 key，按需补充。
3. 重新构建并重启 `short-link-project` 容器。
4. 测试短链接创建，验证 `t_link_*` 表行数正常增加。
5. 查询 `t_group_*` 表中分组名的十六进制值，判断乱码根因并按路径修复。

**回滚：** 若新生产者引发问题，可直接恢复之前的 Docker 镜像（旧 JAR 中的 class 文件运行时仍正常）。

## 待确认问题

- `ShortLinkCreateInitTxProducer` 中 `txTopic` 字段对应的 Nacos 属性 key 名称是什么？
- `ShortLinkCreateInitTxListener` 是否通过 `@RocketMQTransactionListener` 注解关联到同一 `RocketMQTemplate`？
- 数据库中中文分组名是否已乱码存储？
