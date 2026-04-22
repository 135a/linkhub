package metrics

import (
	"fmt"
	"sort"
	"sync/atomic"
	"time"
)

// bucket boundaries: 1ms, 5ms, 10ms, 25ms, 50ms, 100ms, 250ms, 500ms, 1000ms, 2000ms, 5000ms
var latencyBuckets = []float64{1, 5, 10, 25, 50, 100, 250, 500, 1000, 2000, 5000}

type RouteMetrics struct {
	reqCount int64
	errCount int64
	// per-bucket counts, index = bucket boundary index
	latencyBuckets []int64
	totalLatency   float64 // atomic
}

type MetricsCollector struct {
	mu        sync.RWMutex
	routes    map[string]*RouteMetrics
	totalQPS  int64
	startTime time.Time
}

func NewMetricsCollector() *MetricsCollector {
	return &MetricsCollector{
		routes:    make(map[string]*RouteMetrics),
		startTime: time.Now(),
	}
}

func (m *MetricsCollector) getRoute(key string) *RouteMetrics {
	m.mu.RLock()
	r, ok := m.routes[key]
	m.mu.RUnlock()
	if ok {
		return r
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	r, ok = m.routes[key]
	if ok {
		return r
	}
	r = &RouteMetrics{latencyBuckets: make([]int64, len(latencyBuckets)+1)}
	m.routes[key] = r
	return r
}

func (m *MetricsCollector) RecordRequest(method, path string, statusCode int, latencyMs float64) {
	key := fmt.Sprintf("%s:%s", method, path)
	rm := m.getRoute(key)

	atomic.AddInt64(&rm.reqCount, 1)
	if statusCode >= 400 {
		atomic.AddInt64(&rm.errCount, 1)
	}
	atomic.AddInt64(&m.totalQPS, 1)

	// histogram bucket update (no lock, atomic)
	// find bucket index
	idx := sort.SearchFloat64s(latencyBuckets, latencyMs)
	atomic.AddInt64(&rm.latencyBuckets[idx], 1)
	// total latency sum for average
	atomic.AddFloat64(&rm.totalLatency, latencyMs)
}

func (m *MetricsCollector) GetMetrics() map[string]interface{} {
	m.mu.RLock()
	defer m.mu.RUnlock()

	totalReq := int64(0)
	totalErr := int64(0)
	allBuckets := make([]int64, len(latencyBuckets)+1)

	for _, rm := range m.routes {
		rc := atomic.LoadInt64(&rm.reqCount)
		ec := atomic.LoadInt64(&rm.errCount)
		totalReq += rc
		totalErr += ec
		for i, v := range rm.latencyBuckets {
			allBuckets[i] += v
		}
	}

	// compute percentiles from histogram
	var p50, p90, p99 float64
	if totalReq > 0 {
		p50 = percentileFromHist(allBuckets, totalReq, 0.50)
		p90 = percentileFromHist(allBuckets, totalReq, 0.90)
		p99 = percentileFromHist(allBuckets, totalReq, 0.99)
	}

	uptime := time.Since(m.startTime).Seconds()
	qps := float64(atomic.LoadInt64(&m.totalQPS)) / uptime
	errRate := 0.0
	if totalReq > 0 {
		errRate = float64(totalErr) / float64(totalReq) * 100
	}

	return map[string]interface{}{
		"qps":         qps,
		"total_req":   totalReq,
		"total_errors": totalErr,
		"error_rate":  errRate,
		"latency_p50": p50,
		"latency_p90": p90,
		"latency_p99": p99,
		"uptime_s":    uptime,
	}
}

func percentileFromHist(buckets []int64, total int64, p float64) float64 {
	target := int64(float64(total) * p)
	count := int64(0)
	for i, b := range buckets {
		count += b
		if count >= target {
			if i >= len(latencyBuckets) {
				return latencyBuckets[len(latencyBuckets)-1]
			}
			return latencyBuckets[i]
		}
	}
	return latencyBuckets[len(latencyBuckets)-1]
}
