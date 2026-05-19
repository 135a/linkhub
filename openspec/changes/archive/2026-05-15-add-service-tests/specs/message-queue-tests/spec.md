## ADDED Requirements

### Requirement: 消息幂等消费测试
系统 SHALL 验证 `MessageQueueIdempotentHandler` 的消息去重逻辑。

#### Scenario: 消息首次消费
- **WHEN** 消息 key 在 Redis 中不存在
- **THEN** Redis SETNX 成功
- **AND** 消息被正常处理

#### Scenario: 消息重复消费
- **WHEN** 相同消息 key 已在 Redis 中存在
- **THEN** 消息被跳过，不重复处理
- **AND** 记录日志"当前消息被重复消费"

#### Scenario: 消息消费异常时取消幂等标记
- **WHEN** 消息处理过程中抛出异常
- **THEN** 幂等标记从 Redis 中删除
- **AND** 消息可以被重新消费

### Requirement: 统计消息生产者测试
系统 SHALL 验证 `ShortLinkStatsSaveProducer.send` 方法的发送行为。

#### Scenario: 正常发送统计消息
- **WHEN** 传入包含 fullShortUrl、keys、statsRecord 的 Map 参数
- **THEN** RocketMQ 发送被调用，topic 为配置的统计 topic
- **AND** 发送为异步非阻塞模式

#### Scenario: 发送失败时的处理
- **WHEN** RocketMQ 发送返回 SEND_FLUSH_DISK_TIMEOUT
- **THEN** 记录告警日志
- **AND** 不阻塞当前请求

### Requirement: 统计消费者处理测试
系统 SHALL 验证 `ShortLinkStatsSaveConsumer` 的核心处理逻辑。

#### Scenario: UV 首次访问去重
- **WHEN** statsRecord.uv 在 Redis Set 中不存在
- **THEN** UV 计数器 +1
- **AND** uv 加入 Redis Set

#### Scenario: UV 重复访问去重
- **WHEN** statsRecord.uv 在 Redis Set 中已存在
- **THEN** UV 计数器不增加

#### Scenario: UIP 首次访问去重
- **WHEN** 同一 remoteAddr 在当天的 UIP Set 中不存在
- **THEN** UIP 计数器 +1

#### Scenario: 地理位置解析（高德 API）
- **WHEN** 消费消息中 remoteAddr 有效
- **THEN** 调用高德 API 解析省份和城市
- **AND** 地理位置信息写入统计记录

#### Scenario: 统计数据双写
- **WHEN** 消息消费成功
- **THEN** 基本统计写入 ClickHouse
- **AND** 汇总计数更新到 MySQL
