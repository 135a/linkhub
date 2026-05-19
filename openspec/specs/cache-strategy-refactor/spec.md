# Cache Strategy Refactor

## Purpose

Refactor the monolithic `restoreUrl()` method into a clean orchestrator pattern with single-responsibility methods for each cache layer and supporting logic.

## Requirements

### Requirement: restoreUrl 拆分为可读的缓存层方法

系统 MUST 将 `ShortLinkServiceImpl.restoreUrl()` 拆分为编排方法 + 5 个缓存层方法，每个方法单一职责。

#### Scenario: L1 Caffeine 缓存命中时直接重定向

- **WHEN** `fullShortUrl` 在 Caffeine `redirectCache` 中存在
- **THEN** 记录 L1 命中统计
- **AND** 直接 `sendRedirect` 到原始 URL
- **AND** 不再查询更底层的缓存

#### Scenario: L2 Redis 缓存命中时回填 L1 并重定向

- **WHEN** `fullShortUrl` 在 Caffeine 中不存在
- **AND** 在 Redis `short-link:goto:{url}` 中存在
- **THEN** 记录 L2 命中统计
- **AND** 将值回填到 Caffeine L1 缓存
- **AND** `sendRedirect` 到原始 URL

#### Scenario: 空值缓存命中时返回 404

- **WHEN** L1 未命中、L2 未命中
- **AND** Redis `short-link:is-null:goto_{url}` 存在（值为 `-`）
- **THEN** 记录缓存未命中
- **AND** `sendRedirect` 到 `/page/notfound`
- **AND** 不查询数据库

#### Scenario: Bloom filter 未命中时查库

- **WHEN** L1/L2/空值缓存均未命中
- **AND** Bloom filter 返回 false（URL 不存在）
- **THEN** 通过 `shortLinkMapper` 按 `shortUri` 查询数据库
- **AND** 若查到记录，走数据库回填逻辑

#### Scenario: Bloom filter 可能命中时走分布式锁

- **WHEN** Bloom filter 返回 true（可能存在）
- **THEN** 通过分布式锁 + 双重检查 Redis 后查询数据库

### Requirement: 消除重复的统计记录代码

系统 MUST 将各缓存层中重复的 "构建统计记录 → sendRedirect → 提交统计任务" 提取为统一的私有方法。

#### Scenario: 任意缓存层命中后的统计记录

- **WHEN** 任意缓存层确认了原始 URL
- **THEN** 调用统一的 `recordStatsAndRedirect(originUrl, cacheLevel)` 方法
- **AND** 该方法内部完成 redirect 和统计任务提交
