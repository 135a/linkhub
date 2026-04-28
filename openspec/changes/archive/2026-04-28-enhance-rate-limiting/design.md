## Context

项目已引入 Alibaba Sentinel 并在 `SentinelRuleConfig`（实现 `InitializingBean`）中注册了 `create_short-link` 的 QPS 限流规则（上限 1）。但由于 Controller 层缺少资源埋点，且 Spring 容器中未注册 `SentinelResourceAspect`，导致限流规则始终悬空、不生效。

用户要求：
1. 限流覆盖**所有接口**（用户模块、分组模块、短链接模块、回收站模块、统计模块）
2. 通过**自定义注解**在不同接口上设置不同 QPS 阈值，取代硬编码的 `SentinelRuleConfig`

## Goals / Non-Goals

**Goals:**

- 设计自定义注解 `@RateLimit`，可在方法上声明资源名和 QPS 阈值
- 通过 Spring AOP 切面扫描 `@RateLimit` 注解，自动向 Sentinel 注册规则并执行埋点拦截
- 将限流覆盖到系统中所有 Controller 接口
- 限流触发时返回统一的 HTTP 429 响应
- 不引入新的外部中间件，保持单机本地限流

**Non-Goals:**

- 分布式限流（集群维度 QPS 聚合）
- 动态规则推送（Nacos / ZooKeeper 规则中心）
- 熔断降级（Circuit Breaker）规则

## Decisions

### 决策 1：自定义 `@RateLimit` 注解取代 `SentinelRuleConfig` 硬编码

**选择**：自定义注解 `@RateLimit(resource, qps)`  
**备选**：在 `SentinelRuleConfig` 中手动逐一配置每个接口规则

**理由**：
- 注解声明式，与接口定义紧密绑定，直观可读，维护成本低
- 新增接口只需加注解，无需修改配置类
- QPS 值作为注解参数，一目了然，不会产生「规则和代码脱节」问题

**注解定义**：
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    String resource();       // Sentinel 资源名，全局唯一
    double qps() default 10; // QPS 阈值，默认 10
}
```

---

### 决策 2：AOP 切面承担「规则注册」+「埋点拦截」双重职责

**选择**：单一 `RateLimitAspect` 切面  
**备选**：保留 `SentinelRuleConfig` 注册规则 + `@SentinelResource` 做埋点（双重机制）

**理由**：
- 切面在**应用启动后第一次被调用时**动态注册规则，避免启动时批量加载所有规则（`FlowRuleManager.loadRules` 覆盖式加载问题）
- 规则注册与埋点在同一切面中，逻辑内聚，减少配置类数量
- `SentinelRuleConfig` 可保留作历史文档参考，或直接废弃

**切面核心逻辑**：
```
@Around 拦截所有带 @RateLimit 的方法
  ↓
首次调用时：向 FlowRuleManager 追加注册该资源的 FlowRule
  ↓
SphU.entry(resource) 执行埋点
  ↓
捕获 BlockException → 写 429 响应
```

> 规则追加（而非覆盖）：切面维护已注册资源名的本地缓存 `Set<String>`，避免重复注册。

---

### 决策 3：各类接口的 QPS 阈值策略

| 分类 | 接口 | 资源名 | QPS |
|---|---|---|---|
| 短链接 | 创建 | `create_short-link` | 1 |
| 短链接 | 批量创建 | `batch-create_short-link` | 1 |
| 短链接 | 跳转 | `redirect_short-link` | 100 |
| 短链接 | 修改 | `update_short-link` | 5 |
| 短链接 | 分页查询 | `page_short-link` | 20 |
| 分组 | 新增分组 | `save_group` | 5 |
| 分组 | 查询分组 | `list_group` | 20 |
| 分组 | 修改分组 | `update_group` | 5 |
| 分组 | 删除分组 | `delete_group` | 5 |
| 分组 | 排序分组 | `sort_group` | 10 |
| 用户 | 注册 | `user_register` | 1 |
| 用户 | 登录 | `user_login` | 5 |
| 用户 | 查询用户 | `get_user` | 20 |
| 用户 | 修改用户 | `update_user` | 5 |
| 用户 | 退出登录 | `user_logout` | 10 |
| 回收站 | 移入回收站 | `recycle_save` | 5 |
| 回收站 | 恢复 | `recycle_recover` | 5 |
| 回收站 | 删除 | `recycle_remove` | 5 |
| 回收站 | 分页查询 | `recycle_page` | 20 |
| 统计 | 单链接统计 | `stats_single` | 10 |
| 统计 | 分组统计 | `stats_group` | 10 |
| 统计 | 访问记录 | `stats_access_record` | 10 |
| 统计 | 分组访问记录 | `stats_group_access_record` | 10 |

---

### 决策 4：`void` 类型接口的 429 响应处理

批量创建短链接接口返回 `void`，通过 `HttpServletResponse` 输出。AOP 切面捕获 `BlockException` 后：
- 通过 `RequestContextHolder` 获取当前 `HttpServletResponse`
- 设置 status=429，Content-Type=application/json
- 写入统一 JSON Body

---

### 决策 5：`SentinelRuleConfig` 的处理

废弃 `SentinelRuleConfig`（删除或标注 `@Deprecated`）。所有规则由 `RateLimitAspect` 在首次请求时动态注册，无需启动预加载。

## Risks / Trade-offs

- **风险：首次请求时才注册规则，存在极短窗口无规则保护** → 可接受，首次请求本身即在 `SphU.entry` 之前完成注册；若需启动预加载可在切面 `@PostConstruct` 中扫描带注解的方法
- **风险：`sentinel-annotation-aspectj` 不再需要，可移除该依赖** → 确认 pom 中不依赖该包后移除
- **风险：规则追加时并发安全** → 使用 `ConcurrentHashMap` 或 `CopyOnWriteArraySet` 管理已注册资源集合
- **Trade-off：本地限流在多实例部署时各节点独立计数** → 当前接受此限制，分布式限流留作后续演进

## Migration Plan

1. 确认/移除 pom.xml 中 `sentinel-annotation-aspectj` 依赖（如果存在）
2. 新建 `@RateLimit` 注解类
3. 新建 `RateLimitAspect` AOP 切面（含规则动态注册 + 埋点 + BlockException 处理）
4. 废弃 `SentinelRuleConfig`（删除或注释）
5. 为所有 Controller 方法添加 `@RateLimit` 注解
6. 启动验证：正常请求正常响应，连续高频请求触发 429

**回滚**：删除所有 `@RateLimit` 注解及切面类即可恢复原状，无数据库/配置中心变更。

## Open Questions

- 错误码格式需对照项目现有 `IErrorCode` 枚举，确认是否需要新增限流专用错误码。
- 若后续演进为分布式限流，`@RateLimit` 注解可扩展 `cluster` 字段，兼容当前设计。
