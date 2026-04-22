package middleware

import (
	"log"
	"net/http"
	"shortlink-gateway-go/internal/config"
	"shortlink-gateway-go/internal/metrics"
	"shortlink-gateway-go/internal/ratelimit"
	"sort"

	"github.com/gin-gonic/gin"
)

type RateLimitMiddleware struct {
	limiter   *ratelimit.RateLimiter
	metrics   *metrics.MetricsCollector
	prefixes  []rlPrefix // sorted by length desc for longest-prefix match
}

type rlPrefix struct {
	Prefix string
	Config *config.RouteConfig
}

func NewRateLimitMiddleware(limiter *ratelimit.RateLimiter, mc *metrics.MetricsCollector, routes []config.RouteConfig) *RateLimitMiddleware {
	prefixes := make([]rlPrefix, 0, len(routes))
	for i := range routes {
		if routes[i].RateLimit.Enabled {
			prefixes = append(prefixes, rlPrefix{
				Prefix: routes[i].PathPrefix,
				Config: &routes[i],
			})
		}
	}
	sort.Slice(prefixes, func(i, j int) bool {
		return len(prefixes[i].Prefix) > len(prefixes[j].Prefix)
	})
	return &RateLimitMiddleware{
		limiter:  limiter,
		metrics:  mc,
		prefixes: prefixes,
	}
}

func (r *RateLimitMiddleware) Handler() gin.HandlerFunc {
	return func(c *gin.Context) {
		path := c.Request.URL.Path

		var matched *config.RouteConfig
		for i := range r.prefixes {
			p := &r.prefixes[i]
			if len(path) >= len(p.Prefix) && path[:len(p.Prefix)] == p.Prefix {
				matched = p.Config
				break
			}
		}

		if matched == nil {
			c.Next()
			return
		}

		allowed, err := r.limiter.Allow(path, matched.RateLimit.QPS)
		if err != nil {
			log.Printf("rate limit check error: %v", err)
			c.Next()
			return
		}

		if !allowed {
			traceID, _ := c.Get("trace_id")
			log.Printf("rate_limit_exceeded path=%s trace_id=%v qps=%d",
				path, traceID, matched.RateLimit.QPS)

			r.metrics.RecordRequest(c.Request.Method, path, http.StatusTooManyRequests, float64(c.Writer.ElapsedMs()))

			c.AbortWithStatusJSON(http.StatusTooManyRequests, gin.H{
				"error":    "rate_limit_exceeded",
				"message":  "Too Many Requests",
				"trace_id": traceID,
			})
			return
		}

		c.Next()
	}
}
