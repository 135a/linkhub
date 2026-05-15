# ShortLink 压测套件

自动化压测 + 指标采集 + 面试数据卡片生成。

```
test/
├── README.md                           ← 你在这里
├── jmeter/                             ← JMeter 测试计划 (.jmx)
│   ├── redirect-cache-test.jmx         → 缓存性能压测（最核心）
│   ├── create-shortlink-test.jmx       → 创建接口: 无锁 vs 分布式锁
│   ├── full-flow-test.jmx              → 全链路端到端: 注册→登录→创建→重定向→统计
│   └── bloom-penetration-test.jmx      → 布隆过滤器防穿透验证
├── scripts/                            ← Python 自动化脚本
│   ├── config.py                       → 统一配置
│   ├── run_benchmark.py                → 编排器（四阶段压测）
│   ├── collect_metrics.py              → Prometheus 指标采集
│   ├── generate_report.py              → 面试数据卡片生成
│   └── requirements.txt                → Python 依赖
└── results/                            ← 输出目录（自动创建）
    ├── jmeter/                         → JMeter 原始结果 + HTML 报告
    ├── metrics/                        → Prometheus 采集数据 (JSON)
    └── interview_cards/                → 面试数据卡片 (Markdown)
```

---

## 环境要求

| 依赖 | 版本 | 用途 |
|------|------|------|
| Java | 17+ | JMeter 运行时 |
| JMeter | 5.6+ | HTTP 压测引擎 |
| Python | 3.10+ | 编排 + 指标采集 + 报告生成 |
| Docker Compose | - | 启动完整 ShortLink 栈 |

```bash
# Python 依赖
pip install -r test/scripts/requirements.txt
```

---

## 快速开始

### 1. 启动服务

```bash
docker compose up -d
# 等待所有服务 healthy
docker compose ps
```

### 2. 运行全部压测（约 1 小时）

```bash
python test/scripts/run_benchmark.py
```

### 3. 只运行某个阶段

```bash
# Phase 1: 基准测试 (10 分钟)
python test/scripts/run_benchmark.py --phase 1

# Phase 2: 递增压力测试 (20 分钟)
python test/scripts/run_benchmark.py --phase 2

# Phase 3: 长稳测试 (30 分钟)
python test/scripts/run_benchmark.py --phase 3

# Phase 4: 缓存预热测试 (5 分钟)
python test/scripts/run_benchmark.py --phase 4
```

### 4. 生成面试数据卡片

```bash
python test/scripts/generate_report.py
# 输出在 test/results/interview_cards/
```

### 5. 单独运行 JMeter

```bash
# 无 GUI 模式
jmeter -n -t test/jmeter/redirect-cache-test.jmx \
  -Jbase_url=http://localhost:8001 \
  -Jthreads=50 -Jloops=200 \
  -l results/result.csv \
  -e -o results/report/

# GUI 模式（调试用）
jmeter -t test/jmeter/redirect-cache-test.jmx
```

---

## JMeter 参数说明

所有 JMX 文件支持以下命令行参数：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `-Jbase_url` | `http://localhost:8001` | 后端服务地址 |
| `-Jthreads` | `10` | 并发线程数 |
| `-Jloops` | `100` | 每线程循环次数 |
| `-Jduration` | `0` | 持续时间（秒），0=用 loops 控制 |
| `-Jtest_username` | `benchmark_user` | 测试用户名 |
| `-Jtest_password` | `Benchmark123!` | 测试密码 |

---

## Python 配置

修改 `test/scripts/config.py` 或使用环境变量：

```bash
export SHORTLINK_BASE_URL=http://192.168.1.100:8001
export SHORTLINK_TEST_USER=myuser
export SHORTLINK_TEST_PASS=mypass
export JMETER_HOME=/opt/apache-jmeter-5.6.3
```

---

## 输出解读

### JMeter HTML 报告

打开 `test/results/jmeter/*_report/index.html` 查看：

- **APDEX** (Application Performance Index) — 综合满意度评分
- **Response Time Graph** — 响应时间随时间变化
- **Latency Percentiles** — P50/P90/P95/P99
- **Error Rate** — 每个时间窗口的错误率

### 面试数据卡片

文件在 `test/results/interview_cards/README.md`，包含：
1. **卡片 1: 性能总览** — QPS / P99 延迟 / 缓存命中率 / 系统拐点
2. **卡片 2: 架构决策表** — 每个决策的 A/B 数据支撑
3. **卡片 3: 瓶颈分析** — 已知瓶颈 + 改进方向 + 容量规划

---
