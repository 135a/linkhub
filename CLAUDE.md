# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

短链接系统（ShortLink），用于大厂实习面试展示的独立 Spring Boot 单体项目。采用前后端分离架构：`main/` 为后端 Java 17 + Spring Boot 3 模块，`console-vue/` 为前端 Vue 3 + Vite 模块。Docker Compose 一键部署全栈。

## 常用命令

### 后端（Maven 单模块 `main/`）

```bash
# 编译（含 spotless 代码格式化）
mvn compile -pl main

# 运行全部测试 + JaCoCo 覆盖率报告
mvn test -pl main

# 运行单个测试
mvn test -pl main -Dtest="ShortLinkServiceRestoreTest"

# 运行单个测试方法
mvn test -pl main -Dtest="ShortLinkServiceRestoreTest#l1CacheHitRedirectsImmediately"

# 跳过测试打包
mvn -DskipTests clean package -pl main

# 查看 JaCoCo 覆盖率报告（先跑 test 再跑 report）
mvn jacoco:report -pl main
# 然后打开 main/target/site/jacoco/index.html

# 仅做代码格式化（spotless）
mvn spotless:apply -pl main
```

### 前端

```bash
cd console-vue
npm ci
npm run dev      # 开发模式
npm run build    # 生产构建
npm run lint     # 代码检查
```

### Docker Compose

```bash
docker compose up -d --build   # 启动全栈
docker compose down            # 停止所有服务
```

## 核心架构

### 短链接重定向链路（三级缓存）

```
请求 -> L1 Caffeine (redirectCache) -> L2 Redis (short-link:goto:{url})
     -> 布隆过滤器 -> 分布式锁 -> L3 MySQL (t_link_goto / t_link)
```

关键文件：`main/src/main/java/com/nym/shortlink/core/service/impl/ShortLinkServiceImpl.java` 的 `restoreUrl()` 方法。

**Redis 键规范**（定义在 `RedisKeyConstant.java`）：
- `short-link:goto:%s` — 短链接 → 原始 URL 映射
- `short-link:is-null:goto_%s` — 空值缓存防穿透（30分钟TTL）
- `short-link:lock:goto:%s` — 回源时的分布式锁
- `short-link:lock:update-gid:%s` — 跨组移动时的读写锁

### 数据库分片

4 个表各 16 个分片，按 `HASH_MOD` 分片：
- `t_link_0~15` — 按 `gid` 分片
- `t_link_goto_0~15` — 按 `full_short_url` 分片
- `t_user_0~15` — 按 `username` 分片
- `t_group_0~15` — 按 `username` 分片

配置：`shardingsphere-config-{dev|prod}.yaml`，通过 `-Ddatabase.env=dev` 切换。`t_user.phone` 和 `t_user.mail` 使用 AES 列加密。

### 统计链路（幂等消费）

ShortLinkStatsSaveProducer (RocketMQ asyncSend) → ShortLinkStatsSaveConsumer
- 幂等：Redis `short-link:idempotent:{msgKey}`，首次返回 false 会让 MQ 重试，减少后才真正处理
- 双写：MySQL 分表 + ClickHouse 统计库
- 统计字段：PV/UV/UIP + 操作系统/浏览器/设备/网络 + 地区（高德 API 解析）

### MQ 幂等逻辑注意点

`MessageQueueIdempotentHandler.isMessageBeingConsumed()` 返回值含义：
- `true` = 消息已存在（重复消息，跳过处理）
- `false` = 首次消费（抛出 ServiceException 触发 RocketMQ 重试，减少后再处理）

这跟直觉相反，写测试和调试时务必注意。

### Sentinel 限流 AOP

`@RateLimit` 注解 + `RateLimitAspect` 切面，支持三种模式：
- FAST_FAIL — 超阈值直接抛异常
- RATE_LIMITER — 令牌桶排队
- WARM_UP — 冷启动渐进放量

所有 Controller 接口都标注了差异化 QPS 阈值。

### 哈希生成

短链标识符通过 MurmurHash 对原始 URL 做哈希后 Base62 编码，取前 6 位（最长 6 位，实际可能更短）。工具类：`HashUtil.java`。

## 测试规范

### 测试基础设施

- **框架**：JUnit 5 + Mockito（strict mode）+ H2 内存数据库
- **配置**：`main/src/test/resources/application-test.yaml`（禁用 ShardingSphere/Redisson/ClickHouse 自动配置）
- **Schema**：`main/src/test/resources/schema-h2.sql`（无分片版本的 H2 DDL）
- **JaCoCo**：已集成，`mvn test` 自动生成 `target/jacoco.exec`

### 测试写法约束

1. **不含 Spring Context**：所有 Service/Controller 测试使用 `@ExtendWith(MockitoExtension.class)` + 纯 Mockito，不加载 Spring Boot 上下文
2. **Controller 测试用 standalone setup**：用 `MockMvcBuilders.standaloneSetup(controller).build()` 而非 `@WebMvcTest`（后者会初始化 ShardingSphere/Redis 并失败）
3. **ServiceImpl 需要手动注入 baseMapper**：MyBatis Plus 的 `ServiceImpl` 父类中的 `baseMapper` 字段不会被 `@InjectMocks` 自动注入，需在 `@BeforeEach` 中调用：
   ```java
   ReflectionTestUtils.setField(serviceImpl, "baseMapper", shortLinkMapper);
   ```
4. **lenient stubs**：若 setUp 中定义的 stub 不是每个测试方法都会用到，用 `lenient().when(...)` 避免 `UnnecessaryStubbingException`
5. **Redis key 匹配**：生产代码使用 `String.format()` 生成 Redis key，测试中用 `anyString()` 或 `contains("关键字")` 匹配时需注意实际 key 格式。例如 `GOTO_IS_NULL_SHORT_LINK_KEY` 生成的 key 是 `"short-link:is-null:goto_%s"`，用 `contains("is-null:goto_")` 而非 `contains("goto:is-null:")`

### 运行全部测试当前状态

92 个测试，0 失败，约 25% 指令覆盖率。核心链路（三级缓存、布隆过滤器、分布式锁、MQ 幂等、Sentinel 限流 AOP、Controller MockMvc）已覆盖。
