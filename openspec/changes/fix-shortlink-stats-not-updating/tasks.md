## 1. 生产者端 (ShortLinkServiceImpl) 消息发送优化

- [x] 1.1 修改 `shortLinkStats` 方法，在发送消息的 Map 中补全 `fullShortUrl` 和 `gid`。
- [x] 1.2 生成唯一的 `keys` 并放入消息 Map 中，用于消费端幂等性校验。

## 2. 消费者端 (ShortLinkStatsSaveConsumer) 健壮性增强

- [x] 2.1 优化 `onMessage` 方法，增加对 `fullShortUrl` 和 `gid` 的二次校验（从 `statsRecord` 解析）。
- [x] 2.2 优化 `actualSaveShortLinkStats` 方法中的地理位置解析逻辑。
- [x] 2.3 为高德地图 API 调用添加 `try-catch` 块，确保解析失败时能够平滑降级为“未知”。
- [x] 2.4 确保数据库写入操作（如 `linkLocaleStatsMapper` 和 `linkAccessLogsMapper`）在 API 调用失败时仍能按降级数据执行。
- [x] 2.5 修复前端日期工具类，防止日期字符串中出现重复的时间后缀。
- [x] 2.6 优化后端统计查询接口，增强日期格式解析的健壮性。

## 3. 功能验证

- [x] 3.1 重新启动后端服务，访问短链接。
- [x] 3.2 观察控制台日志，确认消费逻辑是否触发。
- [x] 3.3 检查数据库表 `t_link_access_stats`、`t_link_access_logs` 等，确认数据是否更新。
- [x] 3.4 在不配置高德 Key 的情况下，确认地理位置是否显示为“未知”且流程不中断。
