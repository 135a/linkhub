"""
压测配置文件 — 统一管理所有可调参数
"""

import os

# ============================================================
# 服务地址
# ============================================================
BASE_URL = os.getenv("SHORTLINK_BASE_URL", "http://localhost:8001")
PROMETHEUS_URL = os.getenv("SHORTLINK_PROMETHEUS_URL", "http://localhost:8001/actuator/prometheus")
METRICS_URL = f"{BASE_URL}/api/short-link/v1/metrics/summary"

# ============================================================
# API 路径
# ============================================================
API = {
    "register":  "/api/short-link/admin/v1/user",
    "login":     "/api/short-link/admin/v1/user/login",
    "check_user":"/api/short-link/admin/v1/user/has-username",
    "create":    "/api/short-link/admin/v1/create",
    "create_by_lock": "/api/short-link/admin/v1/create/by-lock",
    "batch_create":   "/api/short-link/admin/v1/create/batch",
    "page":      "/api/short-link/admin/v1/page",
    "stats":     "/api/short-link/admin/v1/stats",
    "metrics":   "/api/short-link/v1/metrics/summary",
    "cache_hit": "/api/short-link/v1/monitor/cache-hit-rate/today",
}

# ============================================================
# 测试参数
# ============================================================
# 基准测试
BENCHMARK_THREADS = 10
BENCHMARK_LOOPS = 100

# 递增压力测试
RAMP_UP_STEPS = [50, 100, 200, 300, 400, 500]
RAMP_UP_DURATION_SEC = 120   # 每阶梯持续时间
RAMP_UP_WARMUP_SEC = 30      # 每阶梯预热时间

# 长稳测试
SOAK_THREADS = 200
SOAK_DURATION_SEC = 1800     # 30 分钟

# 缓存失效测试
COLD_START_THREADS = 50
COLD_START_DURATION_SEC = 300  # 5 分钟，观察预热曲线

# ============================================================
# JMeter 配置
# ============================================================
JMETER_HOME = os.getenv("JMETER_HOME", "")
JMETER_BIN = os.path.join(JMETER_HOME, "bin", "jmeter") if JMETER_HOME else "jmeter"
JMETER_PLANS_DIR = os.path.join(os.path.dirname(__file__), "..", "jmeter")
RESULT_DIR = os.path.join(os.path.dirname(__file__), "..", "results")

# ============================================================
# 测试用户凭据
# ============================================================
TEST_USERNAME = os.getenv("SHORTLINK_TEST_USER", "benchmark_user")
TEST_PASSWORD = os.getenv("SHORTLINK_TEST_PASS", "Benchmark123!")
