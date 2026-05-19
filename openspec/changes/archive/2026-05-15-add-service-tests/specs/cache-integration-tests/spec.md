## ADDED Requirements

### Requirement: Caffeine L1 缓存命中率监控
系统 SHALL 验证 `CacheMonitoringService` 在各级缓存命中/未命中时的计数行为。

#### Scenario: L1 命中计数
- **WHEN** Caffeine 缓存中存在目标 key
- **THEN** `recordL1HitAsync()` 被调用一次
- **AND** `metrics.com.l1.hit` 计数器递增

#### Scenario: L2 命中计数
- **WHEN** Caffeine 未命中但 Redis 命中
- **THEN** `recordHitAsync()` 被调用一次
- **AND** L1 未被调用

#### Scenario: 缓存未命中计数
- **WHEN** 所有层级均未命中
- **THEN** `recordMissAsync()` 被调用
- **AND** miss 计数器递增

### Requirement: 布隆过滤器穿透保护
系统 SHALL 验证布隆过滤器在短链接不存在时的拦截行为。

#### Scenario: 布隆过滤器判否且 DB 也不存在
- **WHEN** 布隆过滤器返回 false，且数据库查不到记录
- **THEN** 直接返回 404 页面
- **AND** performanceCounterService.incrementBloomFilterIntercept() 被调用

#### Scenario: 布隆过滤器误报导致的额外查询
- **WHEN** 布隆过滤器返回 true 但实际记录不存在
- **THEN** 走完 L2 → 加锁 → 查库链路后设置空值缓存
- **AND** 不抛出异常

### Requirement: 分布式锁双重检查缓存回源
系统 SHALL 验证加锁后的二次缓存检查流程。

#### Scenario: 加锁后 Redis 已被其他线程回填
- **WHEN** 获取锁后再次查询 Redis，发现数据已存在
- **THEN** 直接使用 Redis 中的数据返回
- **AND** 不再查询数据库

#### Scenario: 加锁后空值缓存已被其他线程设置
- **WHEN** 获取锁后再次查询 Redis，发现空值标记已存在
- **THEN** 返回 404 不查询数据库

### Requirement: 多域名缓存兼容
系统 SHALL 验证不同域名访问同一短链接时的缓存行为。

#### Scenario: 请求域名与创建域名不一致
- **WHEN** Caffeine/Redis 未命中，但通过 shortUri 查到数据库记录
- **THEN** 使用数据库中的 fullShortUrl 作为缓存 key
- **AND** 同时将请求域名 key 和数据库域名 key 都回填到 Caffeine L1
