package metrics

import (
	"fmt"
	"sort"
	"sync"
	"time"
)

type MetricsCollector struct {
	mu           sync.RWMutex
	requestCount map[string]int
	latencies    map[string][]float64
	errorCount   map[string]int
	qpsCount     int
	startTime    time.Time
}

func NewMetricsCollector() *MetricsCollector {
	return &MetricsCollector{
		requestCount: make(map[string]int),
		latencies:    make(map[string][]float64),
		errorCount:   make(map[string]int),
		startTime:    time.Now(),
	}
}

func (m *MetricsCollector) RecordRequest(method, path string, statusCode int, latencyMs float64) {
	m.mu.Lock()
	defer m.mu.Unlock()

	key := fmt.Sprintf("%s:%s", method, path)
	m.requestCount[key]++
	m.latencies[key] = append(m.latencies[key], latencyMs)
	if statusCode >= 400 {
		m.errorCount[key]++
	}
	m.qpsCount++
}

func (m *MetricsCollector) GetLatencyPercentile(p float64) (p50, p90, p99 float64) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	var allLatencies []float64
	for _, lats := range m.latencies {
		allLatencies = append(allLatencies, lats...)
	}

	if len(allLatencies) == 0 {
		return 0, 0, 0
	}

	sort.Float64s(allLatencies)
	n := len(allLatencies)

	p50 = allLatencies[int(float64(n)*0.50)]
	p90 = allLatencies[int(float64(n)*0.90)]
	p99 = allLatencies[int(float64(n)*0.99)]

	return p50, p90, p99
}

func (m *MetricsCollector) GetMetrics() map[string]interface{} {
	m.mu.RLock()
	defer m.mu.RUnlock()

	p50, p90, p99 := m.GetLatencyPercentile(0)

	totalRequests := 0
	totalErrors := 0
	for _, cnt := range m.requestCount {
		totalRequests += cnt
	}
	for _, cnt := range m.errorCount {
		totalErrors += cnt
	}

	uptime := time.Since(m.startTime).Seconds()
	qps := float64(m.qpsCount) / uptime

	return map[string]interface{}{
		"qps":          qps,
		"total_req":    totalRequests,
		"total_errors": totalErrors,
		"error_rate":   float64(totalErrors) / float64(totalRequests) * 100,
		"latency_p50":  p50,
		"latency_p90":  p90,
		"latency_p99":  p99,
		"uptime_s":     uptime,
	}
}