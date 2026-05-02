# 短链接系统创建接口压力测试报告：无锁 vs 分布式锁

## 1. 测试背景与目的
在短链接系统的核心链路中，【创建短链接】属于高频写操作。为了防止同一用户在极端并发情况下对同一个长链接进行重复创建导致数据冗余或缓存雪崩，我们在系统中引入了 **Redis 分布式锁 (Redisson)** 进行并发控制。

本次压力测试旨在：
1. 建立短链接创建接口的性能基准。
2. 对比评估“无锁版本”与“加分布式锁版本”下的 QPS、RT（响应时间）及系统稳定性差异。
3. 记录压测过程中暴露出的环境及代码层面的配置瓶颈。

---

## 2. 测试环境与工具
* **压测工具**：Apache JMeter
* **目标接口**：
  * 无锁版：`POST /api/short-link/admin/v1/create`
  * 分布式锁版：`POST /api/short-link/admin/v1/create/by-lock`
* **硬件及部署环境**：单机 Windows Docker-Compose (含 MySQL, Redis, RocketMQ, ClickHouse, Backend-Project)
* **JMeter 参数**：
  * 线程数：300
  * 请求数据：动态生成随机长链接 (`https://google.com/${__RandomString(10,a-z)}`)，有效 Token
* **后端基础配置**：
  * Tomcat 并发线程数：`max=300`
  * Redis 连接池配置：`max-active=64`, `max-idle=32`

---

## 3. 测试场景一：无锁创建模式 (No Lock)

无锁模式下，请求在经过网关与拦截器验证后，直接进行数据库与 Redis 的写入操作，不阻塞等待。

### 测试数据表现
* **吞吐量 (Throughput)**：稳定在约 **170 QPS** 左右（受限于单机数据库 I/O 与测试机性能）。
* **响应时间 (Average RT)**：极低（几毫秒到几十毫秒不等）。
* **错误率 (Error Rate)**：**0.00%**。

### 结论分析
系统处于无锁状态时，Tomcat 线程池能全速发挥作用。但这种状态在生产环境中存在**并发安全隐患**：若前端未能有效防抖，瞬间发起的数十个相同创建请求，将无视唯一性约束，直接打穿逻辑层导致短链接重复生成。

---

## 4. 测试场景二：分布式加锁模式 (With Global Lock)

在此次测试中，我们使用了 Redisson 的全局锁：
`RLock lock = redissonClient.getLock("SHORT_LINK_CREATE_LOCK_KEY");`

### 4.1 压测踩坑与排雷记录
在跑通加锁压测前，我们遇到并解决了一系列导致 **假阳性 (False Positive)** 和系统崩溃的问题：

1. **死锁与线程耗尽**：最初使用无限期阻塞的 `lock.lock()`。在高并发下由于争抢严重，后方排队的请求占满了全部 Tomcat 线程并全部挂起，最终导致拒绝服务。
   * **解决方案**：引入超时释放与尝试机制 `lock.tryLock(5, 10, TimeUnit.SECONDS)`。5秒内未拿到锁的请求快速失败，抛出客户端异常（400）。
2. **JMeter “成功”但数据库未增加**：JMeter 诡异地显示 200 成功，但 Body 返回 95 bytes 的 HTML。
   * **根本原因**：并发触发了**用户操作流量风控过滤器 (`UserFlowRiskControlFilter`)** 默认的 1000/s 拦截阈值，且过滤器写死了返回 HTML 及 200 状态码。
   * **解决方案**：将返回格式修正为 JSON 并设置 `response.setStatus(429)`，同时将测试环境 `max-access-count` 风控阈值扩大至 50000。
3. **隐蔽的路径空格导致 404**：在 JMeter 配置时，URL 末尾多出空格导致 Spring Boot 识别为无效路由，触发 404 JSON 返回。去除空格后恢复正常。

### 4.2 调优后的真实测试数据
在扫清所有阻碍后，真实的分布式锁压测数据如下：
* **吞吐量 (Throughput)**：**49.4 QPS**。
* **响应时间 (Average RT)**：平均 **200ms** (99% Line 约 348ms)。
* **错误率 (Error Rate)**：**0.00%**。

### 结论分析
分布式锁**起到了完美的串行化保护作用**。
但为什么 QPS 从 170 掉到了 50，RT 从个位数飙升到 200ms？
* 因为我们加的是**“全局唯一锁”**。300个并发线程全部在抢同一把锁，导致全系统的短链接创建动作变成了一条**“单行道”**。
* 假设单个链接创建原本耗时 20ms，那么单通道一秒最多只能放行 `1000 / 20 = 50` 个请求，这与 JMeter 测出的 **49.4 QPS** 物理极限完美吻合。

---

## 5. 总结与后续优化方向

本次压测成功验证了后端链路的强健性以及 Redisson 分布式锁的有效性（在预期负载内达到了零错误率的安全执行）。

**下一步极致性能优化方案 —— 细粒度锁（Granular Locking）**
目前的性能瓶颈源于“粗粒度”的全局锁。在真实生产场景中，我们**不需要阻挡不同用户创建不同的链接，只需要阻挡同一用户短时间重复创建相同的链接**。

**优化代码建议**：
将锁的 Key 拼接上具体的业务特征标识（如：原始长链接 `originUrl` 或 用户 ID）：
```java
// 优化前：单行道
RLock lock = redissonClient.getLock("SHORT_LINK_CREATE_LOCK_KEY");

// 优化后：多车道
RLock lock = redissonClient.getLock("SHORT_LINK_CREATE_LOCK_KEY:" + requestParam.getOriginUrl());
```
采用细粒度锁后，不同 URL 的并发请求将互不干涉，系统的理论 QPS 将瞬间回升至无锁模式下的高性能水平（约 170+ QPS），同时完美保留并发防重的安全特性！
