## Context

ShortLink 项目拥有约 9000 行 Java 源码，采用标准 Controller → Service → DAO 分层架构，集成了 Spring Boot 3、MyBatis Plus、ShardingSphere、Redisson、RocketMQ、Sentinel、Caffeine 等中间件。当前 `src/test/` 仅有一个占位 `demo.java`，测试覆盖率为零。

项目已引入 `spring-boot-starter-test` 和 `mockito-junit-jupiter` 依赖，无需新增核心测试框架。

**约束：**
- 测试不能依赖真实的外部中间件（MySQL、Redis、RocketMQ、ClickHouse、Sentinel Dashboard）
- 分库分表配置（ShardingSphere）在测试环境下需隔离
- 现有代码全部使用构造器注入（`@RequiredArgsConstructor`），利于 mock 注入

## Goals / Non-Goals

**Goals:**
- 为 Service 层核心链路建立单元测试，覆盖短链接创建、重定向、更新、分页
- 为缓存链路（Caffeine/Redis/布隆过滤器回退）建立集成测试
- 为 Sentinel `@RateLimit` AOP 切面建立测试
- 为 RocketMQ 消费者幂等逻辑建立测试
- 为 Controller REST API 建立 MockMvc 测试
- 为工具类（HashUtil、LinkUtil、RandomGenerator）建立纯单元测试
- 项目初期目标行覆盖率 ≥ 60%

**Non-Goals:**
- 不追求 100% 覆盖率
- 不做端到端 (E2E) 测试（需真实浏览器环境）
- 不做性能/压力测试
- 不修改任何生产代码
- 不引入 Testcontainers（太重，使用 H2 + Mock 替代）

## Decisions

### D1: 测试框架选 JUnit 5 + Mockito + MockMvc

- **JUnit 5**: 项目已依赖，无需额外引入
- **Mockito**: 用于 mock 所有外部依赖（Mapper、RedissonClient、StringRedisTemplate、RocketMQ Producer 等）
- **MockMvc**: 用于 Controller 层测试，Spring Boot Test 自带
- **H2 内存数据库**: 用于需要真实 SQL 的测试场景（DAO 层、分页查询），替代 MySQL
- **H2 替代方案**: 大多数 Service 测试直接 mock Mapper 层，不连数据库，更快更纯粹

### D2: 测试分层策略

```
┌─────────────────────────────────────────────────┐
│  Controller 测试 (MockMvc)                       │
│  - mock Service，验证 HTTP 状态码/响应体           │
│  - 约 5 个测试类                                  │
├─────────────────────────────────────────────────┤
│  Service 单元测试 (Mockito)                       │
│  - mock Mapper/Redis/MQ/Redisson，测试业务逻辑     │
│  - 核心链路每个方法至少 3 个 case                   │
│  - 约 6-8 个测试类                                │
├─────────────────────────────────────────────────┤
│  工具类测试 (纯 JUnit 5)                          │
│  - 无外部依赖，纯输入输出断言                       │
│  - 约 3 个测试类                                  │
├─────────────────────────────────────────────────┤
│  集成测试 (@SpringBootTest + H2)                  │
│  - 缓存链路回退、MQ 幂等、限流 AOP                 │
│  - 约 3-4 个测试类                                │
└─────────────────────────────────────────────────┘
```

### D3: 缓存测试策略

缓存链路（L1 Caffeine → L2 Redis → L3 MySQL + 布隆过滤器）是项目最核心的技术亮点，测试策略：

| 场景 | 测试方式 | 验证点 |
|------|---------|--------|
| L1 命中 | 手动往 Caffeine put，调用 `restoreUrl` | 直接返回，不查 Redis/DB |
| L2 命中 | mock Redis 返回值，Caffeine 为空 | 返回正确 URL，L1 被回填 |
| L3 回源 | mock Redis 为空，mock DB 有数据 | 加锁查库，回填 Redis/L1 |
| 缓存穿透 | mock Redis 有 null 标记 | 返回 404，不查 DB |
| 布隆过滤器拦截 | mock 布隆返回 false | 直接返回 404 |

### D4: Sentinel AOP 测试策略

`@RateLimit` 通过 `SentinelResourceAspect` 实现，测试时：
- 使用 `@SpringBootTest` + `@TestPropertySource` 启用 Sentinel
- 模拟快速失败、预热、排队三种模式
- 验证触发限流后返回约定错误码而非 500

### D5: 覆盖率工具选 JaCoCo

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

## Risks / Trade-offs

- **[R1] H2 与 MySQL 语法差异**: H2 不支持部分 MySQL 特有语法（如 `LIMIT` 在子查询中的行为差异）。→ 大多数 Service 测试直接 mock Mapper，不连数据库；需要 SQL 的场景用 H2 MySQL 兼容模式。
- **[R2] ShardingSphere 测试隔离**: 项目有 16 分片配置，测试环境不想加载。→ 通过 `@SpringBootTest(properties = {"spring.profiles.active=dev"})` 使用 dev profile，测试时可额外排除 ShardingSphere 自动配置。
- **[R3] Sentinel 初始化可能耗时**: Sentinel 需要初始化规则，影响测试速度。→ Sentinel 相关测试独立分组，不在每次构建时运行（使用 `@Tag("slow")`）。
- **[R4] 静态方法 mock 困难**: `LinkUtil`、`HashUtil` 中有静态方法。→ mockito-inline 支持静态 mock（已在依赖中），或直接对纯函数做真调用。

## Open Questions

- JaCoCo 覆盖率初始阈值设为 60% 是否合理？（建议先跑完第一轮测试看实际覆盖率再调整）
- 是否需要 CI 中配置覆盖率检查门禁？（建议后续 PR 中单独配置）
