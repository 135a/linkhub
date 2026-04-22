package middleware

import (
	"log"
	"net/http"
	"shortlink-gateway-go/internal/config"
	"shortlink-gateway-go/internal/metrics"
	"shortlink-gateway-go/internal/ratelimit"

	"github.com/gin-gonic/gin"
)

type RateLimitMiddleware struct {
	limiter  *ratelimit.RateLimiter
	metrics  *metrics.MetricsCollector
	routeCfg map[string]*config.RouteConfig
}

func NewRateLimitMiddleware(limiter *ratelimit.RateLimiter, mc *metrics.MetricsCollector, routes []config.RouteConfig) *RateLimitMiddleware {
	routeCfg := make(map[string]*config.RouteConfig)
	for i := range routes {
		routeCfg[routes[i].PathPrefix] = &routes[i]
	}
	return &RateLimitMiddleware{
		limiter:  limiter,
		metrics:  mc,
		routeCfg: routeCfg,
	}
}

func (r *RateLimitMiddleware) Handler() gin.HandlerFunc {
	return func(c *gin.Context) {
		path := c.Request.URL.Path

		var matchedRoute *config.RouteConfig
		for prefix, cfg := range r.routeCfg {
			if len(path) >= len(prefix) && path[:len(prefix)] == prefix {
				matchedRoute = cfg
				break
			}
		}

		if matchedRoute == nil || !matchedRoute.RateLimit.Enabled {
			c.Next()
			return
		}

		allowed, err := r.limiter.Allow(path, matchedRoute.RateLimit.QPS)
		if err != nil {
			log.Printf("rate limit check error: %v", err)
			c.Next()
			return
		}

		if !allowed {
			traceID, _ := c.Get("trace_id")
			log.Printf("rate_limit_exceeded path=%s trace_id=%v qps=%d",
				path, traceID, matchedRoute.RateLimit.QPS)

			r.metrics.RecordRequest(c.Request.Method, path, http.StatusTooManyRequests, float64(c.Writer.ElapsedMs()))

			c.AbortWithStatusJSON(http.StatusTooManyRequests, gin.H{
				"error":   "rate_limit_exceeded",
				"message": "Too Many Requests",
				"trace_id": traceID,
			})
			return
		}

		c.Next()
	}
}