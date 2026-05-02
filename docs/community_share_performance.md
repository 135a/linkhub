# 🚀 从 170 QPS 到 3000 QPS：一次高并发短链系统的"死亡螺旋"调优实录

> **项目地址**：[github.com/135a/shortlink](https://github.com/135a/shortlink)  
> **测试时间**：2026-04-30  
> **测试工具**：JMeter（300 线程，持续 60s）  
> **测试接口**：`GET /{shortUri}` 短链跳转接口

---

## 📖 前言

最近在做个人项目"高并发短链系统"（类 TinyURL）的 JMeter 压测，打算把整个踩坑调优的过程完整记录下来分享给大家。

这次调优历时约十轮迭代，遇到了多个**反直觉的性能陷阱**，每一轮不仅没有简单地线性增长，还多次出现"**改了反而变差**"的情况。最终从 170 QPS / 1700ms 优化到 **2962 QPS / 98ms**，提升了将近 **17 倍**。

希望这份记录对同样在做高并发项目的同学有所帮助。

---

## 🏗️ 系统架构简介

```
JMeter ──► Nginx 网关 ──► Spring Boot 服务
                              ├── Caffeine (L1 本地缓存)
                              ├── Redis (L2 分布式缓存)
                              ├── RocketMQ (异步统计)
                              ├── MySQL + ShardingSphere (分库分表)
                              └── ClickHouse (统计数据落库)
```

---

## 📊 最终成果（先上结果）

| 指标 | 优化前 | 优化后 | 提升幅度 |
| :--- | :--- | :--- | :--- |
| **QPS（吞吐量）** | 173 / sec | **2962 / sec** | 🚀 提升 **17 倍** |
| **平均响应时间** | ~1662 ms | **98 ms** | ⚡ 降低 **94%** |
| **最低响应时间** | 11 ms | **0 ms** | 🎯 达到物理极限 |
| **错误率** | 2.00% | **0.00%** | 🛡️ 完全稳定 |
| **60s 总处理请求** | < 50,000 | **848,826** | 💪 提升 17+ 倍 |

---

## 🕵️ 十二轮调优全程记录

### 各轮压测数据汇总

| 轮次 | 关键变更 | QPS | 平均 RT | 错误率 | 结论 |
|:---|:---|---:|---:|---:|:---|
| 第1轮（基线） | 默认配置（syncSend + 200线程 + 8 Redis连接）| **~160** | 1800ms | 2.51% | syncSend 阻塞主链路 |
| 第2轮 | MQ 改 asyncSend | **~250** | ~1000ms | 2.51% | 有效，但线程不够 |
| 第3轮 | Tomcat 扩到 500 线程 | **~180** | 1770ms | 0.52% | QPS **反降**！Redis 连接池成瓶颈 |
| 第4轮 | Tomcat 300线程 + Redis pool=64 | **130** | 2174ms | 2.44% | 配置未生效 + 遗漏同步写 |
| 第5轮 | UV/UIP 去重移到 MQ Consumer | **196** | 1490ms | 0.66% | 有效，但快慢路径双峰暴露 |
| 第6轮 | 关 SQL 日志 + 延长 Caffeine TTL + tryLock | **198** | 1453ms | **0.00%** | 错误清零，稳定性翻倍 |
| 第7轮 | Lua 脚本 / AntPathMatcher 静态化 | **245** | 1199ms | 0.41% | QPS +24%，标准差 -32% |
| 第8轮 | 跳转路径排除用户流控 | **253** | 1156ms | 0.51% | 微弱提升但 Max RT 爆涨 |
| 第9轮 | sendRedirect 提前到统计之前 | **352** | 818ms | **0.00%** | 重大突破 |
| 第10轮 | CompletableFuture 异步解耦 + 移除日志 | **149** | 高 | - | 反向崩盘，暴露更深瓶颈 |
| 第11轮 | 自定义 STATS_EXECUTOR + 彻底清除主链路 I/O | 待测 | - | - | 理论上解除所有阻塞 |
| **第12轮（终）** | 双重缓存回填，修复"幽灵穿透死循环" | **2962** | **98ms** | **0.00%** | 🏆 史诗级突破 |

---

## 🔍 深度瓶颈分析

### 瓶颈一：RocketMQ `syncSend` 阻塞关键路径

**现象**：QPS 只有 160，远低于缓存命中率理论上限。

分析主链路：
```
GET /{shortUri}（主线程同步执行）
  └── L1 Caffeine 查询（内存, ~0ms）✅
  └── Redis 查询缓存（~1ms）
  └── 统计埋点
        ├── opsForSet().add(UV_KEY)   ← ⚠️ 同步 Redis Write
        ├── opsForSet().add(UIP_KEY)  ← ⚠️ 同步 Redis Write
        └── rocketMQTemplate.syncSend() ← ⚠️⚠️ 同步等待 MQ ACK（10~50ms）
  └── sendRedirect()
```

`syncSend` 每次等待 Broker ACK 耗时 10~50ms，直接叠加在用户 RT 上。

**修复**：改为 `asyncSend`，MQ 发送不阻塞主线程。

**结果**：QPS 160 → 250（+56%）

---

### 瓶颈二：扩大线程池 QPS 反降——Redis 连接池饥饿

**现象**：Tomcat 从 200 线程扩到 500 线程，QPS 从 250 降到了 180！

**反直觉推理**：
```
Tomcat 500 线程全部活跃
  → 每个线程都要访问 Redis
  → Lettuce 默认连接池 max-active = 8
  → 500 线程同时抢 8 个 Redis 连接
  → 492 个线程在连接队列里等待
  → 平均 RT 急剧拉高

公式验证：500线程 / 2.8s ≈ 178 QPS ← 与实测 180 高度吻合 ✓
```

**修复**：
1. Tomcat max-threads 降回 300（与压测并发数匹配）
2. Redis 连接池扩大至 64
3. **关键**：添加 `commons-pool2` 依赖（否则 `lettuce.pool` 配置被静默忽略！）

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

> ⚠️ **陷阱**：没有 commons-pool2，`lettuce.pool` 配置会被 Spring Boot **静默忽略**，Redis 仍用单连接模式。

---

### 瓶颈三：主链路隐藏的 2 次同步 Redis 写

**现象**：加了连接池，QPS 没有改善，甚至降到了 130。

**排查发现**：代码里漏了 2 次在主线程里同步写 Redis Set 的操作。

```java
// 这些藏在 buildLinkStatsRecordAndSetUser() 内，之前分析时被忽略！
stringRedisTemplate.opsForSet().add(UV_KEY + fullShortUrl, uv);   // ⚠️ 同步写
stringRedisTemplate.opsForSet().add(UIP_KEY + fullShortUrl, ip);  // ⚠️ 同步写
```

**修复**：将 UV/UIP 去重判断逻辑完全移到 MQ Consumer 侧执行，主链路只传 Cookie 值和 IP，不做任何 Redis 写操作。

**结果**：QPS 130 → 196（+50%）

---

### 瓶颈四：Redisson `lock.lock()` 无限等待——数学推导与精确吻合

**现象**：平均 RT 精确为 1490ms，但最小 RT 只有 11ms。双峰分布。

**数学推导**（这是本次调优最精彩的一段分析）：
```
前提：Caffeine 30s 过期，压测后段大量 key 失效
      300 线程同时进入 lock.lock()，每个线程处理时间 ≈ 10ms

第  1 个线程：等待   0ms，总 RT =   10ms
第  2 个线程：等待  10ms，总 RT =   20ms
第  3 个线程：等待  20ms，总 RT =   30ms
...
第300个线程：等待2990ms，总 RT = 3000ms

平均等待 = (0+10+...+2990) / 300 = 1495ms ≈ 实测 1490ms ✓ 精确吻合！
```

**修复**：改用 `tryLock(200ms)` + 降级策略：
```java
boolean locked = lock.tryLock(200, 5000, TimeUnit.MILLISECONDS);
if (!locked) {
    // 降级：再读一次 Redis，可能其他线程已写入
    String fallback = stringRedisTemplate.opsForValue().get(GOTO_KEY);
    if (StrUtil.isNotBlank(fallback)) {
        // 正常返回
    } else {
        response.sendRedirect("/page/notfound");
    }
    return;
}
```

---

### 瓶颈五：Lua 脚本每次请求重新解析（Filter 层隐藏开销）

**现象**：主链路已经很快（11ms），但平均 RT 仍然 1453ms。

**发现**：流控 Filter 内部，每个请求都在重新创建对象：
```java
// 每次请求都执行！
DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();         // new 对象
redisScript.setScriptSource(new ResourceScriptSource(                     // 读文件
    new ClassPathResource("lua/user_flow_risk_control.lua")));            // 解析脚本
AntPathMatcher antPathMatcher = new AntPathMatcher();                     // new 对象
```

**修复**：提升为静态常量，类加载时初始化一次：
```java
private static final DefaultRedisScript<Long> FLOW_LIMIT_SCRIPT;
static {
    FLOW_LIMIT_SCRIPT = new DefaultRedisScript<>();
    FLOW_LIMIT_SCRIPT.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("lua/user_flow_risk_control.lua"))
    );
    FLOW_LIMIT_SCRIPT.setResultType(Long.class);
}
private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
```

> 💡 额外收益：`DefaultRedisScript` 静态复用后，Redis 可以缓存 SHA1，后续自动用 `EVALSHA` 替代 `EVAL`，减少约 90% 的网络传输。

**结果**：QPS 198 → 245（+24%，标准差 -32%）

---

### 瓶颈六：Redis 全局热点 Key——匿名请求串行化

**发现**：流控 Filter 中，所有匿名请求的 `username` 被统一降级为字符串 `"other"`：
```java
if (!StringUtils.hasText(username)) {
    username = "other";  // ⚠️ 300 个线程，全用同一个 Key！
}
result = stringRedisTemplate.execute(FLOW_LIMIT_SCRIPT, List.of(username), timeWindow);
```

Redis 是**单线程模型**：
```
300 个线程并发对同一个 Key 执行 Lua INCR 脚本
  → Redis 单线程串行处理
  → 第 300 个线程等待 ≈ 299ms
  → 平均等待 ≈ 150ms（仅流控这一项）
```

**修复**：跳转接口已有 Sentinel QPS 保护，将跳转路径加入流控排除名单。

---

### 瓶颈七：Tomcat 缓冲区陷阱——sendRedirect 不是真的"立即返回"

**现象**：第 9 轮仅把 `sendRedirect` 移到了 `shortLinkStats` 前面，QPS 从 253 → 352，但发现 UV Cookie 全部丢失！

**原因**：
1. `sendRedirect()` 只是把 302 写入 Tomcat 的**内存 Buffer**，实际的网络 flush 要等整个方法结束。如果后续的 `asyncSend` 因为 MQ 积压而等待，Tomcat 工作线程依然被占用。
2. `sendRedirect` 会设置 `response.committed = true`，之后的 `addCookie()` 会**被 Servlet 容器静默忽略**，Cookie 全部丢失！

**终极修复方案**：
```java
// 1. 必须在 sendRedirect 之前生成 statsRecord（含 Cookie 写入）
ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(
    fullShortUrl, request, response);

// 2. 立刻发 302 响应
((HttpServletResponse) response).sendRedirect(l1CachedLink);

// 3. 彻底从 Tomcat 工作线程剥离统计任务
STATS_EXECUTOR.submit(() -> shortLinkStats(statsRecord));
```

自定义线程池（带背压保护）：
```java
private static final ExecutorService STATS_EXECUTOR = new ThreadPoolExecutor(
    50, 200,
    60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(2000),
    new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时反压 Tomcat，优雅降级
);
```

---

### 瓶颈八：史诗级 Bug——"幽灵缓存穿透死循环"

**这是整个调优过程中隐藏最深、最关键的 Bug，也是最终实现 17 倍提升的核心所在。**

**复盘**：

1. JMeter 发送的请求域名是 `127.0.0.1:8000`
2. MySQL 数据库中短链记录的域名是 `shortlink.nym.asia`
3. 请求进来，L1/L2 缓存和布隆过滤器全部未命中（因为域名不一致）
4. 系统查数据库，拿到正确数据后执行了一句致命赋值：

```java
fullShortUrl = shortLinkByUri.getFullShortUrl(); // 变量被替换成了数据库里的域名！
```

5. 把 `shortlink.nym.asia/xyz` 存入 L1 缓存
6. **下一个请求**进来，仍然是 `127.0.0.1:8000/xyz`，L1 缓存又未命中！
7. 无限循环：查 Redis → 查布隆 → 查 MySQL → 加 Redisson 锁 → 写缓存 → 下次仍穿透

**300 个线程，每一个请求都在走全量慢路径，QPS 永远只有 160！**

**修复（双重缓存回填）**：

```java
String requestFullShortUrl = fullShortUrl; // 记住请求来时的"异构域名"

// ... 查库逻辑 ...

// 正常回填
redirectCache.put(fullShortUrl, originalLink);

// 核心修复：把异构域名也强行塞进本地缓存！
if (!Objects.equals(requestFullShortUrl, fullShortUrl)) {
    redirectCache.put(requestFullShortUrl, originalLink);
}
```

只要解析成功一次，下次相同的"异构域名"请求就能 **0ms 本地内存拦截**，彻底断绝数据库访问。

---

## 🏆 反直觉陷阱总结

| 陷阱 | 现象 | 真实原因 |
|:---|:---|:---|
| 扩大 Tomcat 线程，QPS 反降 | 500线程 → QPS 180 | 下游 Redis 只有 8 连接，线程越多竞争越激烈 |
| 配置了连接池，没有效果 | `lettuce.pool` 无效 | 缺少 `commons-pool2` jar，配置被**静默忽略** |
| 改了配置，压测没变化 | 改 yaml/pom 无效 | 容器跑的是旧 jar，根本没用新配置启动 |
| asyncSend 改完还是慢 | QPS 130 | 统计方法里还藏着 2 次同步 Redis 写 |
| sendRedirect 移前了，Cookie 全丢 | UV 统计全变新用户 | `response.committed` 后 `addCookie` 被容器忽略 |
| CompletableFuture 异步化反而崩盘 | QPS 从 352 降到 149 | 默认用 ForkJoinPool，核心数 -1 个线程，瞬间打爆 |
| 缓存加长反而对 QPS 有益 | Caffeine 30s→300s | 压测 60s 内 30s 触发全量失效，精确造成 RT 双峰 |
| lock.lock() 平均 RT = N×处理时间/2 | 1490ms | 串行等待服从等差数列，平均 = 最大/2，数学推导完全吻合 |

---

## 📐 性能调优核心原则

### 原则一：主链路只做主链路的事

> **跳转接口的职责是：读缓存 → 返回 302。**  
> 统计、埋点、UV 去重等旁路操作，一行代码都不应出现在主链路的同步执行路径上。

### 原则二：线程数与连接池必须匹配

```
有效 QPS ≈ min(Tomcat线程数, Redis连接池) / 单次 Redis RTT

若 Tomcat线程 >> Redis连接池：
  多余线程只会在连接队列空转，QPS 受连接池限速。
```

### 原则三：改了代码，必须验证新版本真的在跑

```bash
# 无损热更新（不销毁数据库数据）
mvn clean package && docker compose build project && docker compose up -d project
```

每次压测前确认：jar 打包时间、容器启动日志中的配置加载信息。

### 原则四：Redis 单线程与热点 Key

```
Redis 单线程模型 + 热点 Key = 全局串行瓶颈

预防原则：
  - 限流的 Key 应该分散（按用户/IP），而非聚合（所有匿名 = same key）
  - 公开接口不应使用用户级流控
  - 每个请求必经的代码路径，任何 Redis 调用都必须仔细评估 Key 分布
```

---

## 🗺️ 瓶颈演进全景图

```
[第1轮] QPS 160  ← syncSend 同步阻塞 MQ（10~50ms/次）
    ↓ 修复：改 asyncSend
[第2轮] QPS 250  ← Tomcat 200线程 < JMeter 300并发 → Socket closed
    ↓ 修复：Tomcat max-threads=500（过激了）
[第3轮] QPS 180  ← 500线程竞争 8个Redis连接 → RT膨胀（违反直觉！）
    ↓ 修复：Tomcat=300 + Redis pool=64 + commons-pool2
[第4轮] QPS 130  ← 应用未重打包 + 遗漏2次主链路同步 Redis 写
    ↓ 修复：重打包 + UV/UIP去重迁移到Consumer
[第5轮] QPS 196  ← 快慢路径双峰（lock.lock() 死等 + Caffeine 30s TTL）
    ↓ 修复：关SQL日志 + Caffeine TTL延长 + tryLock降级
[第6轮] QPS 198  ← 错误清零，但 QPS 卡死（Filter 层每次重建对象）
    ↓ 修复：Lua 脚本 + AntPathMatcher 静态化
[第7轮] QPS 245  ← 所有匿名请求打同一个 Redis Key → 全局串行热点
    ↓ 修复：跳转路径排除用户流控
[第8轮] QPS 253  ← sendRedirect 不是立即返回，asyncSend 积压仍阻塞
    ↓ 修复：sendRedirect 提前，统计后置
[第9轮] QPS 352  ← CompletableFuture 用 ForkJoinPool 被打爆 + 残存 Redis 同步写
    ↓ 修复：自定义 STATS_EXECUTOR + 彻底清除主链路 I/O
[第11轮] QPS ~300+ ← "幽灵缓存穿透死循环"（域名不一致导致 L1/L2 永久失效）
    ↓ 修复：双重缓存回填（alias mapping）
[第12轮] QPS 2962 🏆 系统实现 3000 QPS 稳定运行
```

---

## 📌 写在最后

这次调优最大的体会：

1. **高并发系统没有银弹**。每一个看似简单的改动背后，都可能触发下一层的瓶颈，需要持续迭代。
2. **数学推导是最有力的工具**。每次"RT 和 QPS 精确吻合公式"的时刻，是最有成就感的。
3. **测试环境的坑比代码 Bug 更难发现**。Docker 容器旧 jar、JMeter 跟随重定向、域名不一致，都是让人摸不着头脑的假信号。
4. **反直觉的现象往往指向最深层的真相**。扩线程 QPS 反降、异步化反而崩盘，这些都是系统告诉你"还有更深的瓶颈没解决"。

如果你也在做类似项目，欢迎 Star 或提 Issue：

**👉 [github.com/135a/shortlink](https://github.com/135a/shortlink)**

---

*如果这篇文章对你有帮助，欢迎点赞收藏，也欢迎在评论区分享你遇到过的高并发调优坑！*
