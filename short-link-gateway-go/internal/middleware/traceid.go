package middleware

import (
	"encoding/binary"
	"encoding/hex"
	"math/rand"
	logclient "shortlink-gateway-go/internal/log"
	"time"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

// fastUUID generates a UUID-like string using timestamp + random bytes.
// Much faster than crypto/rand-based uuid.NewV4().
func fastUUID() string {
	b := make([]byte, 16)
	now := time.Now().UnixNano()
	binary.LittleEndian.PutUint64(b[:8], uint64(now))
	// fill remaining 8 bytes with random
	for i := 8; i < 16; i += 4 {
		v := rand.Int31()
		b[i] = byte(v)
		b[i+1] = byte(v >> 8)
		b[i+2] = byte(v >> 16)
		b[i+3] = byte(v >> 24)
	}
	// format as hex string
	return hex.EncodeToString(b)
}

func TraceIDInjector() gin.HandlerFunc {
	return func(c *gin.Context) {
		traceID := c.GetHeader("X-Trace-ID")
		if traceID == "" {
			traceID = fastUUID()
		}
		c.Set("trace_id", traceID)
		c.Header("X-Trace-ID", traceID)
		c.Next()
	}
}

func AccessLogger(mc interface{}, logClient interface{}) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		c.Next()
		latency := time.Since(start).Milliseconds()

		// record metrics
		if mc != nil {
			if collector, ok := mc.(interface {
				RecordRequest(string, string, int, float64)
			}); ok {
				collector.RecordRequest(c.Request.Method, c.Request.URL.Path, c.Writer.Status(), float64(latency))
			}
		}

		// get trace ID
		traceID, exists := c.Get("trace_id")
		if !exists {
			traceID = ""
		}

		// log access details
		if logclient.Logger != nil {
			// Mask sensitive headers
			headers := make(map[string][]string)
			for k, v := range c.Request.Header {
				if k == "Authorization" || k == "Token" || k == "Password" || k == "Secret" {
					headers[k] = []string{"******"}
				} else {
					headers[k] = v
				}
			}

			logclient.Logger.Debug("Access log detail",
				zap.String("method", c.Request.Method),
				zap.String("path", c.Request.URL.Path),
				zap.Any("headers", headers),
				zap.String("trace_id", traceID.(string)),
				zap.String("client_ip", c.ClientIP()),
			)

			logclient.Logger.Info("Access log",
				zap.String("method", c.Request.Method),
				zap.String("path", c.Request.URL.Path),
				zap.Int("status", c.Writer.Status()),
				zap.Int64("latency", latency),
				zap.String("trace_id", traceID.(string)),
			)
		}

		// send to log collector asynchronously to avoid blocking response
		if logClient != nil {
			if client, ok := logClient.(interface {
				Send(logclient.LogEntry)
			}); ok {
				go client.Send(logclient.LogEntry{
					Level:     "info",
					Timestamp: time.Now(),
					Message:   "Access log",
					Service:   "gateway",
					TraceID:   traceID.(string),
					Method:    c.Request.Method,
					Path:      c.Request.URL.Path,
					Status:    c.Writer.Status(),
					Latency:   latency,
				})
			}
		}
	}
}
