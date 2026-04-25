## ADDED Requirements

### Requirement: 短链接创建端到端成功
系统 **必须** 能够完整执行短链接创建流程：将记录写入 `t_link` 和 `t_link_goto` 分片表，写入 Redis 缓存，并发送 MQ 消息（tx-init 事务消息 + favicon 异步消息），全程无异常，事务正常提交，最终向调用方返回包含 `fullShortUrl` 的成功响应。

#### Scenario: 正常创建短链接
- **WHEN** 已登录用户提交有效的原始 URL、分组 gid 和创建参数
- **THEN** 系统应在对应分片表（`t_link_*`）中插入一条记录，在 `t_link_goto_*` 中插入路由记录，在 Redis 中写入缓存，向 RocketMQ 发送 tx-init 和 favicon-update 两条消息，并返回 HTTP 200 及包含 `fullShortUrl` 的响应体

#### Scenario: 事务完整性保障
- **WHEN** MQ 消息发送失败（如 RocketMQ Broker 不可达）
- **THEN** 系统 **必须** 回滚 `t_link` 和 `t_link_goto` 的数据库写入，向调用方返回错误响应，不产生孤立数据库记录

#### Scenario: Bloom 过滤器短 URI 去重
- **WHEN** 生成的短链 URI 已存在于布隆过滤器中
- **THEN** 系统 **必须** 重新生成短 URI，最多重试 10 次；超过限制时返回"短链接频繁生成"错误

---

### Requirement: FaviconUpdateProducer 异步发送 Favicon 更新消息
系统 **必须** 在短链接创建成功后，向 `short-link_project-service_favicon-update_topic` 异步发送包含 `fullShortUrl` 和 `originUrl` 的消息，以便消费者异步获取目标页面的 Favicon 并回写。

#### Scenario: Favicon 消息发送成功
- **WHEN** 短链接数据库写入成功、缓存预热完成
- **THEN** `FaviconUpdateProducer.send(fullShortUrl, originUrl)` **必须** 向 MQ 发送消息，消息 payload 包含 `fullShortUrl` 和 `originUrl` 两个字段

#### Scenario: Favicon 消息发送失败不阻塞主流程
- **WHEN** MQ 消息发送过程中抛出异常
- **THEN** 系统 **必须** 记录错误日志，但 **不得** 将异常向上传播至 `createShortLink()` 事务方法，以避免触发不必要的事务回滚

---

### Requirement: ShortLinkCreateInitTxProducer 发送事务初始化消息
系统 **必须** 在短链接创建完成后，向 `short-link_project-service_tx-init_topic` 发送事务消息，用于初始化统计链路（如 `t_link_stats_today` 等统计表的初始化记录）。

#### Scenario: 事务消息发送成功
- **WHEN** 短链接数据库写入完成、Favicon 消息已发送
- **THEN** `ShortLinkCreateInitTxProducer.sendCreateInitTransaction(fullShortUrl, gid)` **必须** 通过 `sendMessageInTransaction` 向 RocketMQ 发送事务消息，并关联本地事务监听器 `ShortLinkCreateInitTxListener`

#### Scenario: 事务消息回查
- **WHEN** RocketMQ Broker 在超时后向本地事务监听器发起回查
- **THEN** `ShortLinkCreateInitTxListener` **必须** 能够根据 `fullShortUrl` 查询数据库，返回正确的本地事务状态（COMMIT / ROLLBACK）
