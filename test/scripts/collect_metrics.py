"""
Prometheus 指标采集器 — 在压测期间定期抓取 /actuator/prometheus 并解析关键指标
"""
import time
import requests
import json
import os
from datetime import datetime
from config import PROMETHEUS_URL, METRICS_URL, RESULT_DIR


def scrape_prometheus() -> dict:
    """抓取 Prometheus text 格式并解析为 dict"""
    try:
        resp = requests.get(PROMETHEUS_URL, timeout=5)
        resp.raise_for_status()
    except Exception as e:
        print(f"[WARN] Prometheus 抓取失败: {e}")
        return {}

    metrics = {}
    for line in resp.text.split("\n"):
        if line.startswith("#") or not line.strip():
            continue
        # 格式: metric_name{labels} value timestamp
        if "{" in line:
            name, rest = line.split("{", 1)
            labels, rest2 = rest.rsplit("}", 1)
            try:
                value = float(rest2.strip().split()[0])
            except (ValueError, IndexError):
                continue
            # 展开 labels 为子键
            label_dict = {}
            for pair in labels.split(","):
                if "=" in pair:
                    k, v = pair.split("=", 1)
                    label_dict[k.strip()] = v.strip('"')
            key = f"{name}[{json.dumps(label_dict, sort_keys=True)}]"
            metrics[key] = value
        else:
            parts = line.split()
            if len(parts) >= 2:
                try:
                    metrics[parts[0]] = float(parts[1])
                except ValueError:
                    continue
    return metrics


def scrape_metrics_api() -> dict:
    """抓取自定义 /api/short-link/v1/metrics/summary 接口"""
    try:
        resp = requests.get(METRICS_URL, timeout=5)
        resp.raise_for_status()
        data = resp.json()
        if data.get("success"):
            return data["data"]
    except Exception as e:
        print(f"[WARN] Metrics API 抓取失败: {e}")
    return {}


def collect_during_benchmark(duration_sec: int, interval_sec: float = 5.0,
                              label: str = "benchmark") -> list:
    """
    在压测期间定期采集指标

    Args:
        duration_sec: 采集总时长（秒）
        interval_sec: 采集间隔（秒）
        label: 本次采集的标签

    Returns:
        [{timestamp, prometheus_metrics, api_metrics}, ...]
    """
    samples = []
    deadline = time.time() + duration_sec
    print(f"\n[采集] 开始采集指标，持续 {duration_sec}s，间隔 {interval_sec}s")

    while time.time() < deadline:
        ts = datetime.now().isoformat()
        prom = scrape_prometheus()
        api = scrape_metrics_api()
        sample = {
            "timestamp": ts,
            "label": label,
            "prometheus": prom,
            "api_metrics": api,
        }
        samples.append(sample)

        # 打印当前快照摘要
        redirect_total = sum(
            v for k, v in prom.items()
            if k.startswith("shortlink_redirect_total")
        )
        cache_hits = sum(
            v for k, v in prom.items()
            if k.startswith("shortlink_cache_hit_total")
        )
        print(f"  [{ts}] redirect_total={redirect_total:.0f}, "
              f"cache_hits={cache_hits:.0f}, "
              f"l1_hit_rate={api.get('l1CacheHitRate', 'N/A')}")

        time.sleep(interval_sec)

    print(f"[采集] 完成，共 {len(samples)} 个采样点")
    return samples


def samples_to_markdown_table(samples: list, title: str = "指标采集快照") -> str:
    """将采集样本转为 Markdown 表格"""
    if not samples:
        return f"## {title}\n\n(无数据)\n"

    lines = [f"## {title}\n"]
    lines.append("| 时间 | L1命中率 | 总命中率 | 布隆拦截 | 今日重定向 |")
    lines.append("|------|----------|----------|----------|-----------|")

    for s in samples:
        ts = s["timestamp"][11:19]  # HH:MM:SS
        api = s.get("api_metrics", {})
        l1 = api.get("l1CacheHitRate", "-")
        total = api.get("cacheHitRate", "-")
        bloom = api.get("bloomFilterInterceptCount", "-")
        redirect = api.get("todayRedirectTotal", "-")
        lines.append(f"| {ts} | {l1} | {total} | {bloom} | {redirect} |")

    return "\n".join(lines)


def save_samples(samples: list, filename: str):
    """保存采集数据为 JSON"""
    os.makedirs(RESULT_DIR, exist_ok=True)
    path = os.path.join(RESULT_DIR, filename)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(samples, f, indent=2, ensure_ascii=False)
    print(f"[保存] 指标数据 → {path}")


if __name__ == "__main__":
    # 快速测试：采集 30 秒
    samples = collect_during_benchmark(30, interval_sec=5, label="quick_test")
    print(samples_to_markdown_table(samples, "Quick Test"))
    save_samples(samples, "quick_test_metrics.json")
