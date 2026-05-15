# 基础设施与中间件技能

## 技能描述
处理项目中 Redis 缓存、消息队列、限流、布隆过滤器、分库分表、ClickHouse 等基础设施和中间件相关任务。

## 适用场景
- Redis 缓存策略调整
- 限流规则配置与修改
- 布隆过滤器相关开发
- ShardingSphere 分库分表配置
- ClickHouse 数据源配置
- Sentinel 流控规则调整
- 消息队列（Redis Stream）开发
- Lua 脚本开发与修改
- 全局异常处理
- 过滤器与拦截器开发

## 项目上下文

### 配置类
- **缓存配置**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/config/CacheConfig.java`
- **ClickHouse 数据源**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/config/ClickHouseDataSourceConfig.java`
- **数据库配置**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/config/DataBaseConfiguration.java`
- **布隆过滤器配置**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/config/RBloomFilterConfiguration.java`
- **Sentinel 规则配置**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/config/SentinelRuleConfig.java`
- **用户配置**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/config/UserConfiguration.java`
- **用户风控配置**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/config/UserFlowRiskControlConfiguration.java`
- **Web MVC 配置**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/config/WebMvcConfiguration.java`
- **域名白名单配置**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/config/GotoDomainWhiteListConfiguration.java`

### 限流与风控
- **限流注解**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/biz/ratelimit/RateLimit.java`
- **限流切面**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/biz/ratelimit/RateLimitAspect.java`
- **用户风控过滤器**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/biz/user/UserFlowRiskControlFilter.java`
- **Lua 风控脚本**: `shortlink-main/main/src/main/resources/lua/user_flow_risk_control.lua`
- **Sentinel 降级处理**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/handler/CustomBlockHandler.java`

### 过滤器与拦截器
- **IP 日志过滤器**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/filter/IpLoggingFilter.java`
- **用户信息传输过滤器**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/biz/user/UserTransmitFilter.java`
- **用户 Token 拦截器**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/biz/user/UserTokenInterceptor.java`
- **全局异常处理**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/web/GlobalExceptionHandler.java`

### 工具类
- **应用上下文持有者**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/toolkit/ApplicationContextHolder.java`
- **哈希工具**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/toolkit/HashUtil.java`
- **链接工具**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/toolkit/LinkUtil.java`
- **随机生成器**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/toolkit/RandomGenerator.java`
- **Excel 工具**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/toolkit/EasyExcelWebUtil.java`

### 常量定义
- **Redis Key 常量**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/constant/RedisKeyConstant.java`
- **Redis 缓存常量**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/constant/RedisCacheConstant.java`
- **短链接常量**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/constant/ShortLinkConstant.java`
- **用户常量**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/constant/UserConstant.java`

### 配置文件
- **应用配置**: `shortlink-main/main/src/main/resources/application.yaml`
- **ShardingSphere 开发环境**: `shortlink-main/main/src/main/resources/shardingsphere-config-dev.yaml`
- **ShardingSphere 生产环境**: `shortlink-main/main/src/main/resources/shardingsphere-config-prod.yaml`

### 异常体系
- **基础错误码**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/convention/errorcode/BaseErrorCode.java`
- **错误码接口**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/convention/errorcode/IErrorCode.java`
- **客户端异常**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/convention/exception/ClientException.java`
- **服务端异常**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/convention/exception/ServiceException.java`
- **集成异常**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/convention/exception/IntegrationException.java`
- **统一返回**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/common/convention/result/Result.java`, `Results.java`

## 技术栈详情

### Redis 使用场景
| 场景 | Key 格式 | 说明 |
|------|----------|------|
| 短链接跳转缓存 | `short-link:goto:%s` | 存储短链接→长链接映射 |
| 空值缓存 | `short-link:is-null:goto_%s` | 防止缓存穿透 |
| 跳转分布式锁 | `short-link:lock:goto:%s` | 防止缓存击穿 |
| 创建短链接锁 | `short-link:lock:create` | 防止并发创建重复短链接 |
| 修改分组锁 | `short-link:lock:update-gid:%s` | 修改分组ID时加锁 |
| UV 统计 | `short-link:stats:uv:` | 判断是否新访客 |
| UIP 统计 | `short-link:stats:uip:` | 判断是否新 IP |
| 统计消息队列 | `short-link:stats-stream` | Redis Stream 异步统计 |
| 延迟队列统计 | `short-link:delay-queue:stats` | 延迟统计任务 |
| 用户登录 Token | Redis 缓存 | 用户登录状态管理 |

### 限流机制
- 基于 Sentinel 实现分布式限流
- `@RateLimit` 注解声明式限流，支持参数：
  - `resource`: 资源名称
  - `qps`: 限流阈值
  - `controlBehavior`: 流控策略（默认直接拒绝，可选匀速排队）
  - `maxQueueingTimeMs`: 最大排队等待时间
  - `message`: 限流提示信息
- 用户风控通过 Lua 脚本实现（`user_flow_risk_control.lua`）

### 布隆过滤器
- 配置在 `RBloomFilterConfiguration` 中
- 用于判断短链接是否已存在，避免重复创建
- 初始化时从数据库加载已有短链接

### ShardingSphere 分库分表
- 配置文件：`shardingsphere-config-dev.yaml` / `shardingsphere-config-prod.yaml`
- 短链接表按 `gid` 分片
- 统计相关表按日期分片

### ClickHouse
- 数据源配置：`ClickHouseDataSourceConfig`
- Mapper XML：`mapper/clickhouse/ClickHouseStatsMapper.xml`
- 用于存储和查询历史访问记录，适合大数据量分析

## 代码规范

### 异常处理
- 客户端错误抛出 `ClientException`
- 服务端错误抛出 `ServiceException`
- 外部集成错误抛出 `IntegrationException`
- 所有异常由 `GlobalExceptionHandler` 统一捕获并转换为 `Result` 响应

### 缓存规范
- 所有 Redis Key 必须定义在 `RedisKeyConstant` 中
- 缓存必须设置过期时间
- 查询时使用「缓存→数据库→缓存」模式
- 空值使用特殊 Key 缓存，防止缓存穿透
- 热点 Key 使用分布式锁，防止缓存击穿

### 限流规范
- 所有 API 必须添加 `@RateLimit` 注解
- 读接口 QPS 一般为 20-1000
- 写接口 QPS 一般为 1-10
- 跳转接口 QPS 最高（5000）

## 常见任务指南

### 新增 Redis 缓存 Key
1. 在 `RedisKeyConstant` 中定义常量
2. 如有过期时间需求，在 `RedisCacheConstant` 中定义
3. 使用 `String.format()` 格式化 Key
4. 确保缓存更新与数据库操作的一致性

### 新增限流资源
1. 在 Controller 方法上添加 `@RateLimit` 注解
2. 设置合理的 `qps` 值
3. 对于写操作建议设置 `message` 提示
4. 高并发场景考虑使用 `CONTROL_BEHAVIOR_RATE_LIMITER`（匀速排队）

### 修改分库分表规则
1. 修改 `shardingsphere-config-dev.yaml` 或 `shardingsphere-config-prod.yaml`
2. 注意分片键和分片算法的一致性
3. 修改后需要重新初始化数据

### 新增全局过滤器
1. 实现 `Filter` 接口或继承 `OncePerRequestFilter`
2. 在配置类中注册过滤器
3. 注意过滤器的执行顺序（`@Order`）
4. 考虑是否需要排除某些路径
