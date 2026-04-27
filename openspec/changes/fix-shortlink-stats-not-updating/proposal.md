## Why

用户反馈访问短链接成功，但后台的访问数据（PV/UV/IP）和历史记录没有任何变化。
经过代码排查，发现存在以下关键问题：
1. **消息队列数据格式不一致**：生产者（`ShortLinkServiceImpl`）仅向消息队列发送了 `statsRecord` 字段，而消费者（`ShortLinkStatsSaveConsumer`）期望从 Map 中获取 `fullShortUrl` 和 `keys`（用于幂等性校验）等顶层字段。由于字段缺失，消费者逻辑直接跳过或抛出异常。
2. **幂等性逻辑阻塞**：消费者中使用了 `messageQueueIdempotentHandler`，如果 `keys` 为空，可能会导致幂等性校验逻辑出现偏差。
3. **高德地图 API 依赖**：当前统计逻辑强依赖高德地图 API 返回的结果。如果未配置或配置错误，虽然代码有“未知”降级处理，但如果前面的数据解析逻辑报错，也会导致后续的数据库写入（如 `LinkAccessLogs`）失败。

## What Changes

1. **修正消息发送逻辑**：在 `ShortLinkServiceImpl` 中发送消息时，补全 `fullShortUrl`、`gid` 和用于幂等校验的 `keys` 字段。
2. **优化消费者数据解析**：在 `ShortLinkStatsSaveConsumer` 中增加对字段缺失的容错处理，确保即使部分元数据缺失，也能通过解析 `statsRecord` 获取必要信息。
3. **增强高德 API 容错**：优化地理位置解析逻辑，确保在 API 调用失败或未配置时，能够稳健地降级为“未知”，并继续执行后续的统计指标更新。
4. **完善幂等性处理**：确保幂等键的生成规则在生产和消费两端保持一致。

## Capabilities

### Modified Capabilities
- `shortlink-management`: 修复短链接统计数据不更新的 Bug，确保统计逻辑的健壮性和数据一致性。

## Impact

- `ShortLinkServiceImpl.java`: 修改 `shortLinkStats` 方法，发送完整的消息体。
- `ShortLinkStatsSaveConsumer.java`: 优化消息处理流程和字段解析。
- `LinkUtil.java`: (可选) 优化 IP 解析相关的工具类以配合统计。
