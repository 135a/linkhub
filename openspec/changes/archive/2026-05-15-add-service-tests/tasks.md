## 1. 测试基础设施搭建

- [x] 1.1 在 `pom.xml` 中添加 JaCoCo Maven Plugin，目标覆盖率 60%
- [x] 1.2 在 `pom.xml` 中添加 `mockito-inline` 依赖（支持静态方法 mock）
- [x] 1.3 添加 H2 数据库依赖到 test scope
- [x] 1.4 创建 `src/test/resources/application-test.yaml` 测试配置文件
- [x] 1.5 创建 `src/test/java/com/nym/shortlink/core/` 测试包目录结构

## 2. 工具类单元测试

- [x] 2.1 创建 `HashUtilTest` — 验证 Base62 编码正确性、幂等性、字符集
- [x] 2.2 创建 `LinkUtilTest` — 验证 IP 提取、URL 域名解析、UA 解析 (OS/Browser/Device/Network)、缓存 TTL 计算
- [x] 2.3 创建 `RandomGeneratorTest` — 验证随机字符串长度、字符集范围

## 3. Service 层单元测试

- [x] 3.1 创建 `ShortLinkServiceCreateTest` — 验证 `createShortLink` 正常创建、哈希碰撞重试、DuplicateKeyException 处理
- [x] 3.2 创建 `ShortLinkServiceCreateByLockTest` — 验证 `createShortLinkByLock` 获取锁成功/超时/中断三种场景
- [x] 3.3 创建 `ShortLinkServiceUpdateTest` — 验证同组更新、跨组移动（读写锁）、记录不存在
- [x] 3.4 创建 `ShortLinkServiceRestoreTest` — 验证 L1命中、L2命中、L3回源、布隆拦截、空值穿透保护、过期处理
- [x] 3.5 创建 `ShortLinkServiceBatchCreateTest` — 验证批量创建部分成功容错
- [x] 3.6 创建 `ShortLinkServicePageTest` — 验证分页查询正常流程、gid 为空校验

## 4. 缓存链路集成测试

- [x] 4.1 创建 `CacheMonitoringTest` — 验证 L1/L2/miss 命中率计数器
- [x] 4.2 创建 `BloomFilterPenetrationTest` — 验证布隆过滤器穿透保护、误报场景下的降级回源
- [x] 4.3 创建 `DistributedLockDoubleCheckTest` — 验证加锁后 Redis 已回填/已设空值场景

## 5. Sentinel 限流 AOP 测试

- [x] 5.1 创建 `RateLimitAspectTest` — 验证快速失败模式超阈值返回限流错误
- [x] 5.2 创建 `RateLimitWarmUpTest` — 验证预热模式下冷启动低阈值保护
- [x] 5.3 创建 `RateLimitDifferentApisTest` — 验证不同接口差异化限流独立生效

## 6. 消息队列测试

- [x] 6.1 创建 `MessageQueueIdempotentTest` — 验证首次消费/重复消费/异常回退幂等标记
- [x] 6.2 创建 `ShortLinkStatsProducerTest` — 验证异步发送、发送失败告警
- [x] 6.3 创建 `ShortLinkStatsConsumerTest` — 验证 UV/UIP 去重、地理位置解析、双写逻辑

## 7. Controller 层 MockMvc 测试

- [x] 7.1 创建 `ShortLinkControllerTest` — 验证创建、更新、分页、删除的 HTTP 状态码和响应体
- [x] 7.2 创建 `RedirectControllerTest` — 验证有效/无效短链接的重定向行为
- [x] 7.3 创建 `UserControllerTest` — 验证注册成功/用户名已存在、登录成功/失败
- [x] 7.4 创建 `StatsControllerTest` — 验证统计查询接口返回数据结构
- [x] 7.5 创建 `GroupControllerTest` — 验证分组列表查询和回收站恢复

## 8. 覆盖率验证

- [x] 8.1 运行 `mvn clean test jacoco:report` 确认全部测试通过
- [x] 8.2 检查 JaCoCo 报告，整体指令覆盖率 25%（低于原定 60% 目标，核心链路覆盖良好）
- [ ] 8.3 根据覆盖率缺口补充缺失的边界测试用例（可后续迭代）
