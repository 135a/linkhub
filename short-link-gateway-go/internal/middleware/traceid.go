package middleware

import (
	"shortlink-gateway-go/internal/config"
	"shortlink-gateway-go/internal/metrics"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

const (
	TraceIDHeader = "X-Trace-ID"
	TraceIDKey    = "trace_id"
)

func TraceIDInjector() gin.HandlerFunc {
	return func(c *gin.Context) {
		traceID := c.GetHeader(TraceIDHeader)
		if traceID == "" {
			traceID = uuid.NewV4().String()
		}
		c.Set(TraceIDKey, traceID)
		c.Header(TraceIDHeader, traceID)
		c.Next()
	}
}

func AccessLogger(mc *metrics.MetricsCollector) gin.HandlerFunc {
	return func(c *gin.Context) {
		traceID, _ := c.Get(TraceIDKey)
		tid, ok := traceID.(string)
		if !ok {
			tid = ""
		}

		c.Next()

		latency := float64(c.Writer.ElapsedMs())
		statusCode := c.Writer.Status()
		method := c.Request.Method
		path := c.Request.URL.Path

		mc.RecordRequest(method, path, statusCode, latency)

		accessLog := map[string]interface{}{
			"method":      method,
			"path":        path,
			"status_code": statusCode,
			"latency_ms":  latency,
			"trace_id":    tid,
		}

		cfg := config.Get()
		if cfg != nil {
			go sendAccessLog(accessLog)
		}
	}
}

func sendAccessLog(log map[string]interface{}) {
}