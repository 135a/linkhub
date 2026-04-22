package shortlink

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
)

func TestTraceIDMiddleware(t *testing.T) {
	gin.SetMode(gin.TestMode)

	t.Run("generates trace ID when not provided", func(t *testing.T) {
		router := gin.New()
		router.Use(TraceIDInjector())
		router.GET("/test", func(c *gin.Context) {
			traceID, exists := c.Get("trace_id")
			assert.True(t, exists)
			assert.NotEmpty(t, traceID)
			c.String(http.StatusOK, traceID.(string))
		})

		w := httptest.NewRecorder()
		req, _ := http.NewRequest("GET", "/test", nil)
		router.ServeHTTP(w, req)

		assert.Equal(t, http.StatusOK, w.Code)
		assert.NotEmpty(t, w.Header().Get("X-Trace-ID"))
	})

	t.Run("uses provided trace ID", func(t *testing.T) {
		router := gin.New()
		router.Use(TraceIDInjector())
		router.GET("/test", func(c *gin.Context) {
			traceID, _ := c.Get("trace_id")
			c.String(http.StatusOK, traceID.(string))
		})

		w := httptest.NewRecorder()
		req, _ := http.NewRequest("GET", "/test", nil)
		req.Header.Set("X-Trace-ID", "custom-trace-id")
		router.ServeHTTP(w, req)

		assert.Equal(t, http.StatusOK, w.Code)
		assert.Equal(t, "custom-trace-id", w.CodeToString())
	})
}

func TestRateLimiter(t *testing.T) {
	t.Run("allows requests within limit", func(t *testing.T) {
		limiter := NewRateLimiter(nil, 100)
		for i := 0; i < 50; i++ {
			allowed, _ := limiter.Allow("test-endpoint", 100)
			assert.True(t, allowed)
		}
	})

	t.Run("blocks requests over limit", func(t *testing.T) {
		limiter := NewRateLimiter(nil, 10)
		for i := 0; i < 10; i++ {
			limiter.Allow("test-limit", 10)
		}
		allowed, _ := limiter.Allow("test-limit", 10)
		assert.False(t, allowed)
	})
}

func TestCORSMiddleware(t *testing.T) {
	gin.SetMode(gin.TestMode)

	cfg := &Config{
		CORS: CORSConfig{
			AllowedOrigins:   []string{"http://localhost:3000"},
			AllowedMethods:   []string{"GET", "POST"},
			AllowedHeaders:   []string{"Content-Type"},
			ExposeHeaders:    []string{"X-Trace-ID"},
			AllowCredentials: true,
			MaxAge:           86400,
		},
	}

	t.Run("allows valid origin", func(t *testing.T) {
		router := gin.New()
		router.Use(func(c *gin.Context) {
			c.Set("config", cfg)
			CORS()(c)
			c.Next()
		})
		router.GET("/test", func(c *gin.Context) {
			c.String(http.StatusOK, "ok")
		})

		w := httptest.NewRecorder()
		req, _ := http.NewRequest("GET", "/test", nil)
		req.Header.Set("Origin", "http://localhost:3000")
		router.ServeHTTP(w, req)

		assert.Equal(t, http.StatusOK, w.Code)
		assert.Equal(t, "http://localhost:3000", w.Header().Get("Access-Control-Allow-Origin"))
	})

	t.Run("handles OPTIONS preflight", func(t *testing.T) {
		router := gin.New()
		router.Use(func(c *gin.Context) {
			c.Set("config", cfg)
			CORS()(c)
			c.Next()
		})
		router.OPTIONS("/test", func(c *gin.Context) {
			c.String(http.StatusNoContent, "")
		})

		w := httptest.NewRecorder()
		req, _ := http.NewRequest("OPTIONS", "/test", nil)
		req.Header.Set("Origin", "http://localhost:3000")
		router.ServeHTTP(w, req)

		assert.Equal(t, http.StatusNoContent, w.Code)
	})
}

func TestMetricsCollector(t *testing.T) {
	mc := NewMetricsCollector()

	t.Run("records requests", func(t *testing.T) {
		mc.RecordRequest("GET", "/api/test", 200, 10.5)
		mc.RecordRequest("GET", "/api/test", 200, 15.0)
		mc.RecordRequest("GET", "/api/test", 400, 5.0)

		metrics := mc.GetMetrics()
		assert.Equal(t, 3.0, metrics["total_req"])
		assert.Equal(t, 1.0, metrics["total_errors"])
	})

	t.Run("calculates latency percentiles", func(t *testing.T) {
		for i := 0; i < 100; i++ {
			mc.RecordRequest("GET", "/api/latency", 200, float64(i))
		}

		p50, p90, p99 := mc.GetLatencyPercentile(0)
		assert.True(t, p50 > 0)
		assert.True(t, p90 > p50)
		assert.True(t, p99 > p90)
	})
}

func TestAccessLogger(t *testing.T) {
	gin.SetMode(gin.TestMode)

	mc := NewMetricsCollector()

	t.Run("logs request details", func(t *testing.T) {
		router := gin.New()
		router.Use(TraceIDInjector())
		router.Use(AccessLogger(mc))
		router.GET("/test", func(c *gin.Context) {
			c.String(http.StatusOK, "ok")
		})

		w := httptest.NewRecorder()
		req, _ := http.NewRequest("GET", "/test", nil)
		router.ServeHTTP(w, req)

		assert.Equal(t, http.StatusOK, w.Code)
	})
}

func TestHealthEndpoint(t *testing.T) {
	gin.SetMode(gin.TestMode)

	router := gin.New()
	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status":  "healthy",
			"service": "shortlink-gateway-go",
		})
	})

	w := httptest.NewRecorder()
	req, _ := http.NewRequest("GET", "/health", nil)
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response map[string]string
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.Equal(t, "healthy", response["status"])
	assert.Equal(t, "shortlink-gateway-go", response["service"])
}

func TestTokenBucket(t *testing.T) {
	t.Run("refills tokens over time", func(t *testing.T) {
		tb := NewTokenBucket(10, 10)

		for i := 0; i < 10; i++ {
			assert.True(t, tb.Allow())
		}

		assert.False(t, tb.Allow())

		time.Sleep(100 * time.Millisecond)

		assert.True(t, tb.Allow())
	})

	t.Run("reports remaining tokens", func(t *testing.T) {
		tb := NewTokenBucket(5, 5)

		tb.Allow()
		tb.Allow()

		remaining := tb.Remaining()
		assert.True(t, remaining < 5)
		assert.True(t, remaining > 0)
	})
}