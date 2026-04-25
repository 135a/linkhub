## 1. 恢复缺失的消息生产者源文件

- [x] 1.1 创建 `ShortLinkCreateInitTxProducer.java` 源文件，实现事务消息发送逻辑
- [x] 1.2 创建 `FaviconUpdateProducer.java` 源文件，实现异步 Favicon 更新消息发送逻辑
- [x] 1.3 验证 `ShortLinkServiceImpl` 中的 Bean 注入是否正常，确保不再抛出 NoSuchBeanDefinitionException

## 2. 修复中文乱码与请求头处理

- [x] 2.1 修改 `short-link-gateway-go` 的认证中间件，在转发 `username`、`real-name` 等 Header 前进行 URL 编码
- [x] 2.2 修改 `short-link-java/admin` 中的 `UserTransmitFilter.java`，增加对请求头是否包含 `%` 的判断，防止重复解码
- [x] 2.3 检查 MySQL 数据库 `t_group` 相关表的字符集配置，确保为 `utf8mb4`

## 3. 配置与环境验证

- [x] 3.1 检查并更新 Nacos 配置文件，确保 `rocketmq.producer.tx-init-topic` 和 `rocketmq.producer.favicon-topic` 配置正确
- [x] 3.2 重新构建 `short-link-project` 服务镜像并重启容器 (已额外修复缺失的 LockService/CacheProducer/UrlFormat)
- [x] 3.3 重新构建 `short-link-gateway-go` 网关镜像并重启容器

## 4. 端到端测试验证

- [ ] 4.1 测试创建短链接流程，验证数据库 `t_link_*` 表记录成功插入且未回滚
- [ ] 4.2 验证创建包含中文名称的分组，检查前端及数据库存储是否均无乱码
- [ ] 4.3 观察 RocketMQ 消费者日志，确认 tx-init 和 favicon-update 消息被成功消费
