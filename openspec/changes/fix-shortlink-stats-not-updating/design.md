## Context

当前短链接统计逻辑在访问成功后无法更新数据。核心原因在于 RocketMQ 生产者和消费者之间的数据契约不匹配，以及对外部 API（高德地图）调用的依赖缺乏足够的健壮性。

## Goals / Non-Goals

**Goals:**
- 修复生产者发送的消息格式，补全缺失的元数据。
- 增强消费者对消息解析的健壮性，支持多种字段获取方式。
- 确保在未配置高德 API Key 或调用失败时，统计指标仍能正常落地。
- 修复幂等性校验逻辑，防止因 `keys` 缺失导致的消费阻塞或异常。

**Non-Goals:**
- 不涉及短链接跳转逻辑的修改（已在上一 Change 中完成）。
- 不涉及统计图表前端展示逻辑的优化。

## Decisions

### 1. 生产者数据补全
在 `ShortLinkServiceImpl.shortLinkStats` 中，除了发送 `statsRecord` JSON 字符串外，显式地在 Map 中添加 `fullShortUrl`、`gid` 和基于 `UUID` 生成的 `keys`。
- **Rationale**: 消费者目前的逻辑是从 Map 顶层获取这些字段。

### 2. 消费者解析容错
修改 `ShortLinkStatsSaveConsumer`：
- **数据来源补全**：如果 `fullShortUrl` 或 `gid` 在 Map 顶层缺失，尝试从已解析的 `statsRecord` 对象中重新获取。
- **地理位置降级**：捕获 `HttpUtil.get` 调用及其后续 JSON 解析的异常，确保异常发生时 `actualProvince` 和 `actualCity` 回退为“未知”，不中断后续的 `mybatis-plus` 写入操作。

### 3. 幂等性增强
确保生产端生成 `keys` 并放入消息。消费端若依然遇到 `keys` 为空（极特殊情况），应有默认的处理逻辑或抛出明确异常而非隐式失败。

## Risks / Trade-offs

- **MQ 负载**：在消息体中增加元数据会略微增加带宽占用，但对于该规模的系统影响可以忽略。
- **数据一致性**：在高德 API 降级期间，部分记录的地理位置为“未知”，这是为了保证核心统计指标（PV/UV）可用而做的权衡。
