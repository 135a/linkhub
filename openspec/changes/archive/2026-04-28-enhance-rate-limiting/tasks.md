## 1. 依赖与基础配置

- [x] 1.1 检查 `pom.xml` 是否已包含 `com.alibaba.csp:sentinel-core`；若缺失则添加
- [x] 1.2 确认 `pom.xml` 中**不存在** `sentinel-annotation-aspectj` 依赖（本方案不使用，避免切面冲突）；若存在则移除
- [x] 1.3 确认 Spring AOP 依赖已引入（`spring-boot-starter-aop`）；若缺失则添加

## 2. 自定义限流注解

- [x] 2.1 在 `com.nym.shortlink.core.common.biz.ratelimit`（或 `config`）包下新建 `@RateLimit` 注解
  - `String resource()`：Sentinel 资源名，必填
  - `double qps() default 10`：QPS 阈值，默认 10
  - `@Target(ElementType.METHOD)`、`@Retention(RetentionPolicy.RUNTIME)`、`@Documented`
- [x] 2.2 为注解添加 Javadoc，说明参数含义和使用示例

## 3. 限流 AOP 切面

- [x] 3.1 新建 `RateLimitAspect` 类，添加 `@Aspect`、`@Component` 注解
- [x] 3.2 使用 `ConcurrentHashMap<String, Boolean>` 维护已注册资源名的本地缓存，防止重复注册
- [x] 3.3 实现 `@Around` 切点，拦截所有带 `@RateLimit` 注解的方法
- [x] 3.4 在切面中实现首次调用时动态注册规则：读取注解的 `resource` 和 `qps`，向 `FlowRuleManager` **追加**（而非覆盖）FlowRule（`FLOW_GRADE_QPS`）
- [x] 3.5 实现 `SphU.entry(resource)` 埋点调用，捕获 `BlockException`
- [x] 3.6 捕获 `BlockException` 后：
  - 通过 `RequestContextHolder` 获取 `HttpServletResponse`
  - 设置 HTTP status = 429、`Content-Type: application/json;charset=UTF-8`
  - 写入统一 JSON Body（错误码 + "接口限流，请稍后再试"）
- [x] 3.7 为 `void` 类型方法（批量创建接口）的 429 响应做兼容处理（直接写 response，方法返回 `null`）
- [x] 3.8 为切面添加完整 Javadoc 和关键步骤注释

## 4. 废弃 SentinelRuleConfig

- [x] 4.1 删除 `SentinelRuleConfig.java`（或添加 `@Deprecated` 注解并注释掉 `@Component`，保留代码供参考）
- [x] 4.2 确认删除后应用启动无 Bean 注入异常

## 5. ShortLinkController 接口限流标注

- [x] 5.1 `createShortLink`：添加 `@RateLimit(resource = "create_short-link", qps = 1)`
- [x] 5.2 `batchCreateShortLink`：添加 `@RateLimit(resource = "batch-create_short-link", qps = 1)`
- [x] 5.3 `updateShortLink`：添加 `@RateLimit(resource = "update_short-link", qps = 5)`
- [x] 5.4 `pageShortLink`：添加 `@RateLimit(resource = "page_short-link", qps = 20)`
- [x] 5.5 `restoreUrl`（短链接跳转）：添加 `@RateLimit(resource = "redirect_short-link", qps = 100)`

## 6. GroupController 接口限流标注

- [x] 6.1 `save`（新增分组）：添加 `@RateLimit(resource = "save_group", qps = 5)`
- [x] 6.2 `listGroup`（查询分组）：添加 `@RateLimit(resource = "list_group", qps = 20)`
- [x] 6.3 `updateGroup`（修改分组）：添加 `@RateLimit(resource = "update_group", qps = 5)`
- [x] 6.4 `updateGroup`（删除分组，`@DeleteMapping`）：添加 `@RateLimit(resource = "delete_group", qps = 5)`
- [x] 6.5 `sortGroup`（排序分组）：添加 `@RateLimit(resource = "sort_group", qps = 10)`

## 7. UserController 接口限流标注

- [x] 7.1 `register`（用户注册）：添加 `@RateLimit(resource = "user_register", qps = 1)`
- [x] 7.2 `login`（用户登录）：添加 `@RateLimit(resource = "user_login", qps = 5)`
- [x] 7.3 `getUserByUsername`：添加 `@RateLimit(resource = "get_user", qps = 20)`
- [x] 7.4 `getActualUserByUsername`：添加 `@RateLimit(resource = "get_actual_user", qps = 20)`
- [x] 7.5 `hasUsername`：添加 `@RateLimit(resource = "check_username", qps = 20)`
- [x] 7.6 `update`（修改用户）：添加 `@RateLimit(resource = "update_user", qps = 5)`
- [x] 7.7 `logout`（退出登录）：添加 `@RateLimit(resource = "user_logout", qps = 10)`

## 8. RecycleBinController 接口限流标注

- [x] 8.1 `saveRecycleBin`（移入回收站）：添加 `@RateLimit(resource = "recycle_save", qps = 5)`
- [x] 8.2 `pageShortLink`（回收站分页）：添加 `@RateLimit(resource = "recycle_page", qps = 20)`
- [x] 8.3 `recoverRecycleBin`（恢复短链接）：添加 `@RateLimit(resource = "recycle_recover", qps = 5)`
- [x] 8.4 `removeRecycleBin`（删除短链接）：添加 `@RateLimit(resource = "recycle_remove", qps = 5)`

## 9. ShortLinkStatsController 接口限流标注

- [x] 9.1 `shortLinkStats`（单链接统计）：添加 `@RateLimit(resource = "stats_single", qps = 10)`
- [x] 9.2 `groupShortLinkStats`（分组统计）：添加 `@RateLimit(resource = "stats_group", qps = 10)`
- [x] 9.3 `shortLinkStatsAccessRecord`（单链接访问记录）：添加 `@RateLimit(resource = "stats_access_record", qps = 10)`
- [x] 9.4 `groupShortLinkStatsAccessRecord`（分组访问记录）：添加 `@RateLimit(resource = "stats_group_access_record", qps = 10)`

## 10. 验证与测试

- [ ] 10.1 启动应用，确认无报错，所有接口正常响应
- [ ] 10.2 对 `POST /api/short-link/admin/v1/create` 在 1 秒内发送 2 次请求，验证第 2 次返回 HTTP 429
- [ ] 10.3 对 `GET /{shortUri}` 正常请求，验证跳转功能不受影响
- [ ] 10.4 对 `POST /api/short-link/admin/v1/create/batch` 超限，验证 `HttpServletResponse` 写入 429 Body 正确
- [ ] 10.5 检查 Sentinel 控制台（或日志）确认资源已正确注册

## 11. 注解支持限流算法选择

- [x] 11.1 在 `@RateLimit` 注解中新增 `controlBehavior` 参数（`int`，默认 `RuleConstant.CONTROL_BEHAVIOR_DEFAULT` 即快速失败）
- [x] 11.2 在 `@RateLimit` 注解中新增 `maxQueueingTimeMs` 参数（`int`，默认 `500`ms，仅漏桶模式生效）
- [x] 11.3 更新 `RateLimitAspect#registerRuleIfAbsent`，将 `controlBehavior` 和 `maxQueueingTimeMs` 传递给 `FlowRule`
- [x] 11.4 为写接口（创建、批量创建）指定漏桶算法：`controlBehavior = RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER`
- [x] 11.5 其余接口保持快速失败默认值，无需修改

## 12. 注解支持自定义限流提示信息

- [x] 12.1 在 `@RateLimit` 注解中新增 `message` 参数，默认值为 "当前网站访问人数过多,请耐心等待"
- [x] 12.2 更新 `RateLimitAspect#handleBlockException`，从注解读取 `message` 并写入 429 响应体
- [x] 12.3 为安全敏感接口（登录、注册）设置专属提示："操作过于频繁，请稍后再试"
- [x] 12.4 为创建接口设置专属提示："创建请求过于频繁，请稍后再试"
