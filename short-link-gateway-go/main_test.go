package main

import (
	"shortlink-gateway-go/internal/config"
	"shortlink-gateway-go/internal/metrics"
	"shortlink-gateway-go/internal/middleware"
	"shortlink-gateway-go/internal/proxy"
	"shortlink-gateway-go/internal/ratelimit"

	"github.com/gin-gonic/gin"
	"github.com/go-redis/redis/v8"
)

func main() {
	cfg, err := config.Load("config.yaml")
	if err != nil {
		panic("failed to load config: " + err.Error())
	}

	redisClient := redis.NewClient(&redis.Options{
		Addr: config.GetRedisAddr(),
	})

	mc := metrics.NewMetricsCollector()
	rl := ratelimit.NewRateLimiter(redisClient)

	gin.SetMode(gin.ReleaseMode)
	r := gin.New()
	r.Use(gin.Recovery())

	r.Use(middleware.CORS())
	r.Use(middleware.TraceIDInjector())
	r.Use(middleware.AccessLogger(mc))

	rateLimitMw := middleware.NewRateLimitMiddleware(rl, mc, cfg.Routes)
	r.Use(rateLimitMw.Handler())

	r.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "healthy"})
	})

	r.GET("/api/v1/metrics", func(c *gin.Context) {
		m := mc.GetMetrics()
		c.JSON(200, m)
	})

	router := proxy.NewRouter(cfg.Routes)
	r.Any("/*path", gin.WrapH(router))

	if err := r.Run(":" + cfg.Server.Port); err != nil {
		panic("server failed: " + err.Error())
	}
}