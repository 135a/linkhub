package middleware

import (
	"encoding/binary"
	"encoding/hex"
	"math/rand"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
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

func AccessLogger(mc interface{}) gin.HandlerFunc {
	return func(c *gin.Context) {
		traceID, _ := c.Get("trace_id")
		tid, _ := traceID.(string)

		c.Next()

		// record metrics
		if mc != nil {
			if collector, ok := mc.(interface {
				RecordRequest(string, string, int, float64)
			}); ok {
				latency := float64(c.Writer.ElapsedMs())
				collector.RecordRequest(c.Request.Method, c.Request.URL.Path, c.Writer.Status(), latency)
			}
		}
	}
}
