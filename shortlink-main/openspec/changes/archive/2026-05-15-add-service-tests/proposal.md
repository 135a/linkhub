## Why

项目当前测试覆盖率几乎为零——源码约 9000+ 行 Java 代码，`src/test/` 下仅有一个占位 `demo.java`。这与项目自身定位"展示可测试性建设能力"严重不符，也是大厂面试中最容易被质疑的硬伤。补全核心链路测试不仅能堵住这个漏洞，更能为后续重构和迭代提供安全网。

## What Changes

- 新增 Service 层核心业务的单元测试：短链接创建、跳转重定向、统计消费者
- 新增缓存链路集成测试：Caffeine L1 → Redis L2 → MySQL L3 逐级回退
- 新增限流 AOP 切面的单元测试：`@RateLimit` 注解的快速失败、漏桶、预热三种模式
- 新增工具类测试：MurmurHash Base62 编码、短链接生成器、随机字符串生成
- 新增 MQ 消费者幂等处理测试
- 新增 Controller 层 MockMvc 集成测试：核心 API 端点
- 配置 JaCoCo 插件，设置初期目标行覆盖率 ≥ 60%

## Capabilities

### New Capabilities

- `service-unit-tests`: Service 层核心业务逻辑的单元测试，覆盖短链接 CRUD、重定向缓存链路、统计处理
- `cache-integration-tests`: 多级缓存链路集成测试，验证 L1/L2/L3 逐级回退和缓存击穿防护
- `sentinel-rate-limit-tests`: Sentinel 限流 AOP 切面测试，验证 @RateLimit 三种流控模式
- `message-queue-tests`: RocketMQ 消费者幂等处理和异步统计管道测试
- `controller-api-tests`: Controller 层 MockMvc 集成测试，覆盖核心 REST API 端点
- `toolkit-unit-tests`: 工具类测试，覆盖 Hash 编码、Base62 转换、随机生成器

### Modified Capabilities

<!-- No existing specs to modify -->

## Impact

- 新增依赖：`spring-boot-starter-test`（已有）、`mockito-inline`（静态方法 mock）、`h2`（测试用内存数据库）、`testcontainers`（可选，Redis/MySQL 集成测试）
- 新增文件：`src/test/java/com/nym/shortlink/core/` 下约 15-20 个测试类
- 影响构建：`pom.xml` 添加 JaCoCo 插件配置
- 不影响任何生产代码
