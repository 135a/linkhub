## ADDED Requirements

### Requirement: 短链接创建测试
系统 SHALL 验证 `ShortLinkServiceImpl.createShortLink` 方法在正常输入和异常输入下的行为。

#### Scenario: 正常创建短链接
- **WHEN** 传入有效的 `ShortLinkCreateReqDTO`（含 originUrl、gid、validDateType 等必填字段）
- **THEN** 返回 `ShortLinkCreateRespDTO`，包含生成的 fullShortUrl、originUrl 和 gid
- **AND** Mapper.insert 被调用 2 次（ShortLinkDO + ShortLinkGotoDO）
- **AND** Redis 缓存被写入（goto key）
- **AND** 布隆过滤器添加新 URL

#### Scenario: 创建短链接时发生哈希碰撞
- **WHEN** 布隆过滤器连续返回"已存在"，超过 10 次重试
- **THEN** 抛出 `ServiceException`，消息包含"短链接频繁生成，请稍后再试"

#### Scenario: 创建短链接时 DuplicateKeyException
- **WHEN** Mapper.insert 抛出 `DuplicateKeyException`，且布隆过滤器返回 false
- **THEN** 布隆过滤器补加该 URL
- **AND** 抛出 `ServiceException`，消息包含"生成重复"

### Requirement: 分布式锁创建短链接测试
系统 SHALL 验证 `createShortLinkByLock` 方法在获取锁和锁超时场景下的行为。

#### Scenario: 成功获取分布式锁并创建
- **WHEN** `redissonClient.getLock().tryLock()` 返回 true
- **THEN** 短链接创建成功
- **AND** finally 块中 `lock.unlock()` 被调用

#### Scenario: 获取分布式锁超时
- **WHEN** `redissonClient.getLock().tryLock()` 返回 false
- **THEN** 抛出 `ClientException`，消息包含"系统繁忙"

#### Scenario: 获取分布式锁被中断
- **WHEN** `redissonClient.getLock().tryLock()` 抛出 `InterruptedException`
- **THEN** 当前线程中断标志被设置
- **AND** 抛出 `ServiceException`，消息包含"被中断"

### Requirement: 短链接更新测试
系统 SHALL 验证 `updateShortLink` 在同组更新和跨组更新两种场景下的行为。

#### Scenario: 同组内更新短链接
- **WHEN** 请求的 `gid` 与数据库记录相同
- **THEN** 直接更新 `t_link` 表
- **AND** 不涉及分布式读写锁

#### Scenario: 跨组移动短链接
- **WHEN** 请求的 `gid` 与数据库记录不同
- **THEN** 使用 Redisson RReadWriteLock 加写锁
- **AND** 逻辑删除旧记录（delFlag=1, delTime 非零）
- **AND** 插入新记录（新 gid）
- **AND** 更新 `t_link_goto` 表的 gid

#### Scenario: 更新不存在的短链接
- **WHEN** 数据库中查不到对应记录
- **THEN** 抛出 `ClientException`，消息包含"短链接记录不存在"

### Requirement: 短链接重定向（多级缓存）测试
系统 SHALL 验证 `restoreUrl` 方法在各级缓存命中/未命中时的正确回退和缓存回填行为。

#### Scenario: L1 Caffeine 缓存命中
- **WHEN** Caffeine 缓存中存在该 URL 的原始链接
- **THEN** 直接 302 重定向到原始链接
- **AND** 不查询 Redis
- **AND** 异步发送统计数据到 MQ

#### Scenario: L2 Redis 缓存命中
- **WHEN** Caffeine 未命中但 Redis 中存在 `goto` key
- **THEN** 302 重定向到原始链接
- **AND** Caffeine L1 缓存被回填

#### Scenario: L2 未命中，布隆过滤器命中，L3 数据库回源成功
- **WHEN** Redis 未命中、布隆过滤器命中、数据库中存在有效记录
- **THEN** 加分布式锁双重检查后查库
- **AND** 回填 Redis 和 Caffeine
- **AND** 302 重定向到原始链接

#### Scenario: 布隆过滤器未命中，按 shortUri 查库找到记录
- **WHEN** 布隆过滤器因域名不一致未命中，但 shortUri 在数据库中存在
- **THEN** 使用数据库中的 fullShortUrl 作为缓存 key 继续后续流程

#### Scenario: 空值缓存命中（缓存穿透保护）
- **WHEN** Redis 中存在 `goto_is_null` 标记
- **THEN** 直接返回 302 到 `/page/notfound`
- **AND** 不查询数据库

#### Scenario: 短链接已过期
- **WHEN** 数据库记录的 validDate 早于当前时间
- **THEN** 设置空值缓存
- **AND** 302 重定向到 `/page/notfound`

### Requirement: 批量创建短链接测试
系统 SHALL 验证 `batchCreateShortLink` 的容错行为。

#### Scenario: 批量创建部分成功
- **WHEN** 传入 3 条 URL，其中第 2 条创建失败
- **THEN** 返回结果 total=2，包含成功创建的 2 条
- **AND** 失败的不影响其他条目的创建

### Requirement: 短链接分页查询测试
系统 SHALL 验证 `pageShortLink` 的分页和参数校验行为。

#### Scenario: 正常分页查询
- **WHEN** 传入有效 gid 和分页参数
- **THEN** 返回 `IPage<ShortLinkPageRespDTO>`，domain 字段拼接 http:// 前缀

#### Scenario: gid 为空的分页查询
- **WHEN** 传入空 gid
- **THEN** 抛出 `ClientException`，消息包含"分组标识不能为空"

### Requirement: 分组短链接计数测试
系统 SHALL 验证 `listGroupShortLinkCount` 的边界情况。

#### Scenario: 空集合输入
- **WHEN** 传入空的 gid 列表
- **THEN** 返回空 ArrayList（不执行 SQL）

#### Scenario: 正常计数
- **WHEN** 传入有效的 gid 列表
- **THEN** 返回各组的短链接计数
