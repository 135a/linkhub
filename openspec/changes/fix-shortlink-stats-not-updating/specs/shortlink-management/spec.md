## ADDED Requirements

### Requirement: 统计数据的实时性与可靠性
系统 MUST 确保在短链接被访问后，相关的统计数据（PV、UV、IP）及访问日志能够被准确、实时地记录到数据库中。

#### Scenario: 访问短链接后统计数据更新
- **WHEN** 用户成功访问并跳转一个短链接
- **THEN** 系统 SHALL 发送包含完整元数据（fullShortUrl, gid, statsRecord）的消息至消息队列，且消费者能够成功解析并更新相关统计表

### Requirement: 统计逻辑的容错性与健壮性
统计消费逻辑 MUST 具备容错能力，在外部 API（如高德地图）不可用或元数据部分缺失时，仍能完成核心统计指标（如 PV、UV）的记录。

#### Scenario: 地理位置解析失败时降级处理
- **WHEN** 高德地图 API 调用失败或未配置 API Key
- **THEN** 系统 SHALL 将地理位置记录为“未知”，并确保其余统计数据（浏览器、操作系统、访问日志等）正常存入数据库
