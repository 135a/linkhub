"""
压测编排器 — 使用 JMeter + Python 自动化执行完整的四阶段压测流程
"""
import os
import sys
import time
import json
import subprocess
import argparse
from datetime import datetime

sys.path.insert(0, os.path.dirname(__file__))
from config import (
    BASE_URL, JMETER_BIN, JMETER_PLANS_DIR, RESULT_DIR,
    TEST_USERNAME, TEST_PASSWORD,
    BENCHMARK_THREADS, BENCHMARK_LOOPS,
    RAMP_UP_STEPS, RAMP_UP_DURATION_SEC, RAMP_UP_WARMUP_SEC,
    SOAK_THREADS, SOAK_DURATION_SEC,
    COLD_START_THREADS, COLD_START_DURATION_SEC,
)
from collect_metrics import collect_during_benchmark, save_samples, samples_to_markdown_table


def ensure_dirs():
    os.makedirs(RESULT_DIR, exist_ok=True)
    os.makedirs(os.path.join(RESULT_DIR, "jmeter"), exist_ok=True)
    os.makedirs(os.path.join(RESULT_DIR, "metrics"), exist_ok=True)


def health_check() -> bool:
    """检查目标服务是否可达"""
    import requests
    try:
        r = requests.get(f"{BASE_URL}/api/short-link/v1/metrics/summary", timeout=5)
        if r.status_code == 200:
            print(f"[OK] 服务 {BASE_URL} 可达")
            return True
    except Exception as e:
        pass
    print(f"[FAIL] 无法连接 {BASE_url}")
    return False


def register_test_user() -> str:
    """注册测试用户并返回 token"""
    import requests

    # 先检查用户是否已存在
    check_url = f"{BASE_URL}/api/short-link/admin/v1/user/has-username"
    r = requests.get(check_url, params={"username": TEST_USERNAME})
    exists = r.json().get("data", False) if r.status_code == 200 else False

    if not exists:
        # 注册
        reg_url = f"{BASE_URL}/api/short-link/admin/v1/user"
        payload = {
            "username": TEST_USERNAME,
            "password": TEST_PASSWORD,
            "phone": "13800000001",
        }
        r = requests.post(reg_url, json=payload)
        if r.status_code == 200:
            print(f"[OK] 注册测试用户: {TEST_USERNAME}")
        else:
            print(f"[WARN] 注册返回 {r.status_code}: {r.text[:200]}")
    else:
        print(f"[INFO] 测试用户 {TEST_USERNAME} 已存在，跳过注册")

    # 登录
    login_url = f"{BASE_URL}/api/short-link/admin/v1/user/login"
    payload = {"username": TEST_USERNAME, "password": TEST_PASSWORD}
    r = requests.post(login_url, json=payload)
    if r.status_code == 200:
        token = r.json().get("data", {}).get("token", "")
        print(f"[OK] 登录成功, token={token[:8]}...")
        return token
    else:
        print(f"[FAIL] 登录失败: {r.text[:200]}")
        sys.exit(1)


def run_jmeter(plan_name: str, props: dict = None,
               threads: int = None, loops: int = None,
               duration_sec: int = None) -> str:
    """
    运行 JMeter 测试计划

    Args:
        plan_name: JMeter 计划文件名（不含路径）
        props: 额外的 JMeter 属性
        threads: 覆盖线程数
        loops: 覆盖循环次数
        duration_sec: 覆盖持续时间

    Returns:
        JMeter 结果文件路径
    """
    plan_path = os.path.join(JMETER_PLANS_DIR, plan_name)
    if not os.path.exists(plan_path):
        print(f"[FAIL] JMX 文件不存在: {plan_path}")
        return ""

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    result_file = os.path.join(RESULT_DIR, "jmeter", f"{plan_name}_{timestamp}.csv")
    report_dir = os.path.join(RESULT_DIR, "jmeter", f"{plan_name}_{timestamp}_report")

    cmd = [
        JMETER_BIN,
        "-n",
        "-t", plan_path,
        "-l", result_file,
        "-e",
        "-o", report_dir,
    ]

    # 传参覆盖默认值
    jmeter_props = []
    if threads is not None:
        jmeter_props.append(f"-Jthreads={threads}")
    if loops is not None:
        jmeter_props.append(f"-Jloops={loops}")
    if duration_sec is not None:
        jmeter_props.append(f"-Jduration={duration_sec}")
    jmeter_props.append(f"-Jbase_url={BASE_URL}")
    jmeter_props.append(f"-Jtest_username={TEST_USERNAME}")
    jmeter_props.append(f"-Jtest_password={TEST_PASSWORD}")

    cmd = cmd[:5] + jmeter_props + cmd[5:]

    print(f"\n[执行] jmeter {' '.join(cmd[1:4])}...")
    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode != 0:
        print(f"[FAIL] JMeter 执行失败")
        print(result.stderr[-500:] if result.stderr else "(no stderr)")
        return ""

    print(f"[OK] JMeter 完成 → {result_file}")
    return result_file


def phase1_benchmark(token: str) -> dict:
    """Phase 1: 基准测试 — 测量各接口基线性能"""
    print("\n" + "=" * 60)
    print("Phase 1/4: 基准测试")
    print("=" * 60)

    results = {}

    # 1.1 重定向压测
    print("\n--- 1.1 重定向接口基准 ---")
    metrics_start = collect_during_benchmark(10, interval_sec=5, label="before_redirect")
    redirect_file = run_jmeter(
        "redirect-cache-test.jmx",
        threads=BENCHMARK_THREADS,
        loops=BENCHMARK_LOOPS,
    )
    metrics_after = collect_during_benchmark(10, interval_sec=5, label="after_redirect")
    results["redirect"] = {
        "jmeter_file": redirect_file,
        "config": f"{BENCHMARK_THREADS} threads × {BENCHMARK_LOOPS} loops",
    }

    # 1.2 创建接口基准
    print("\n--- 1.2 创建接口基准 ---")
    create_file = run_jmeter(
        "create-shortlink-test.jmx",
        threads=BENCHMARK_THREADS,
        loops=BENCHMARK_LOOPS,
    )
    results["create"] = {
        "jmeter_file": create_file,
        "config": f"{BENCHMARK_THREADS} threads × {BENCHMARK_LOOPS} loops",
    }

    results["metrics_before"] = metrics_start
    results["metrics_after"] = metrics_after
    return results


def phase2_ramp_up(token: str) -> list:
    """Phase 2: 递增压力测试 — 找到系统拐点"""
    print("\n" + "=" * 60)
    print("Phase 2/4: 递增压力测试")
    print("=" * 60)

    all_samples = []

    for step, threads in enumerate(RAMP_UP_STEPS, 1):
        print(f"\n--- Step {step}/{len(RAMP_UP_STEPS)}: {threads} 并发 ---")
        label = f"ramp_{threads}threads"

        # 启动 Prometheus 采集（后台）
        metrics = collect_during_benchmark(
            RAMP_UP_DURATION_SEC, interval_sec=10, label=label
        )
        all_samples.append({
            "threads": threads,
            "metrics": metrics,
        })

        # 运行 JMeter 压测
        run_jmeter(
            "redirect-cache-test.jmx",
            threads=threads,
            duration_sec=RAMP_UP_DURATION_SEC,
        )

        # 间歇冷却
        if step < len(RAMP_UP_STEPS):
            print(f"  冷却 15s...")
            time.sleep(15)

    save_samples(all_samples, "phase2_ramp_up.json")
    return all_samples


def phase3_soak(token: str) -> list:
    """Phase 3: 长稳测试 — 30 分钟持续压测"""
    print("\n" + "=" * 60)
    print("Phase 3/4: 长稳测试 (30 分钟)")
    print("=" * 60)

    # 每 30 秒采集一次，总共 60 个采样点
    samples = collect_during_benchmark(
        SOAK_DURATION_SEC, interval_sec=30, label="soak_test"
    )

    run_jmeter(
        "redirect-cache-test.jmx",
        threads=SOAK_THREADS,
        duration_sec=SOAK_DURATION_SEC,
    )

    save_samples(samples, "phase3_soak.json")
    return samples


def phase4_cold_start(token: str) -> list:
    """Phase 4: 缓存失效测试 — 记录预热曲线"""
    print("\n" + "=" * 60)
    print("Phase 4/4: 缓存预热测试")
    print("=" * 60)

    # 清空缓存需要后端支持——调用专用 endpoint（如果有）
    import requests
    try:
        r = requests.post(f"{BASE_URL}/api/short-link/v1/cache/clear", timeout=5)
        print(f"[INFO] 缓存清空: {r.status_code}")
    except Exception:
        print("[WARN] 无法清空缓存（/api/short-link/v1/cache/clear 不存在），"
              "请手动重启服务或通过 Redis CLI FLUSHALL")

    # 高频采集（每秒一次）以记录预热曲线
    samples = collect_during_benchmark(
        COLD_START_DURATION_SEC, interval_sec=2, label="cold_start"
    )

    run_jmeter(
        "redirect-cache-test.jmx",
        threads=COLD_START_THREADS,
        duration_sec=COLD_START_DURATION_SEC,
    )

    save_samples(samples, "phase4_cold_start.json")
    return samples


def print_summary(phase_results: dict):
    """打印压测总结"""
    print("\n" + "=" * 60)
    print("压测完成！汇总")
    print("=" * 60)
    print(f"\n结果文件目录: {RESULT_DIR}")
    print(f"  JMeter 原始数据: {os.path.join(RESULT_DIR, 'jmeter')}/")
    print(f"  Prometheus 指标: {os.path.join(RESULT_DIR, 'metrics')}/")
    print(f"\n下一步: 运行 generate_report.py 生成面试数据卡片")
    print("=" * 60)


def main():
    parser = argparse.ArgumentParser(description="ShortLink 压测编排器")
    parser.add_argument("--phase", type=str, default="all",
                        choices=["all", "1", "2", "3", "4"],
                        help="指定运行阶段 (default: all)")
    parser.add_argument("--skip-health", action="store_true",
                        help="跳过健康检查")
    args = parser.parse_args()

    ensure_dirs()

    if not args.skip_health and not health_check():
        print("[FAIL] 服务不可达，请确认 Docker Compose 已启动: docker compose up -d")
        sys.exit(1)

    # 获取 token
    token = register_test_user()

    results = {}

    if args.phase in ("all", "1"):
        results["phase1"] = phase1_benchmark(token)

    if args.phase in ("all", "2"):
        results["phase2"] = phase2_ramp_up(token)

    if args.phase in ("all", "3"):
        results["phase3"] = phase3_soak(token)

    if args.phase in ("all", "4"):
        results["phase4"] = phase4_cold_start(token)

    print_summary(results)


if __name__ == "__main__":
    main()
