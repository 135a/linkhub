package metrics

import (
	"sort"
	"sync"
	"time"
)

type MetricsCollector struct {
	mu        sync.RWMutex
	reqCount  int64
	errCount  int64
	latencies []float64
	startTime time.Time
}

func NewMetricsCollector() *MetricsCollector {
	return &MetricsCollector{
		latencies: make([]float64, 0, 10000),
		startTime: time.Now(),
	}
}

func (m *MetricsCollector) RecordRequest(method, path string, statusCode int, latencyMs float64) {
	m.mu.Lock()
	defer m.mu.Unlock()

	m.reqCount++
	if statusCode >= 400 {
		m.errCount++
	}
	m.latencies = append(m.latencies, latencyMs)
	if len(m.latencies) > 50000 {
		m.latencies = m.latencies[len(m.latencies)/2:]
	}
}

func (m *MetricsCollector) GetMetrics() map[string]interface{} {
	m.mu.RLock()
	defer m.mu.RUnlock()

	totalReq := m.reqCount
	totalErr := m.errCount

	var p50, p90, p99 float64
	if totalReq > 0 {
		sorted := make([]float64, len(m.latencies))
		copy(sorted, m.latencies)
		sort.Float64s(sorted)
		n := len(sorted)
		if n > 0 {
			p50 = sorted[n/2]
			p90 = sorted[int(float64(n)*0.9)]
			p99 = sorted[int(float64(n)*0.99)]
		}
	}

	uptime := time.Since(m.startTime).Seconds()
	qps := 0.0
	if uptime > 0 {
		qps = float64(totalReq) / uptime
	}
	errRate := 0.0
	if totalReq > 0 {
		errRate = float64(totalErr) / float64(totalReq) * 100
	}

	return map[string]interface{}{
		"qps":          qps,
		"total_req":    totalReq,
		"total_errors": totalErr,
		"error_rate":   errRate,
		"latency_p50":  p50,
		"latency_p90":  p90,
		"latency_p99":  p99,
		"uptime_s":     uptime,
	}
}
