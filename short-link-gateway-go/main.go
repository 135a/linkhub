package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"shortlink-gateway-go/internal/config"
	"shortlink-gateway-go/internal/handler"
	"shortlink-gateway-go/internal/metrics"
	"shortlink-gateway-go/internal/middleware"
	"shortlink-gateway-go/internal/proxy"
	"shortlink-gateway-go/internal/ratelimit"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/go-redis/redis/v8"
)

func main() {
	configPath := os.Getenv("CONFIG_PATH")
	if configPath == "" {
		configPath = "config.yaml"
	}

	cfg, err := config.Load(configPath)
	if err != nil {
		log.Fatalf("failed to load config: %v", err)
	}

	var redisClient *redis.Client
	if cfg.Redis.Host != "" {
		redisClient = redis.NewClient(&redis.Options{
			Addr:     config.GetRedisAddr(),
			Password: cfg.Redis.Password,
			DB:       cfg.Redis.DB,
		})
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		if err := redisClient.Ping(ctx).Err(); err != nil {
			log.Printf("redis connection failed: %v, using memory-only rate limit", err)
			redisClient = nil
		}
	}

	mc := metrics.NewMetricsCollector()
	rl := ratelimit.NewRateLimiter(redisClient)
	healthHandler := handler.NewHealthHandler()
	metricsHandler := handler.NewMetricsHandler(mc)

	gin.SetMode(gin.ReleaseMode)
	r := gin.New()
	r.Use(gin.Recovery())

	r.Use(middleware.CORS())
	r.Use(middleware.TraceIDInjector())
	r.Use(middleware.AccessLogger(mc))

	rateLimitMw := middleware.NewRateLimitMiddleware(rl, mc, cfg.Routes)
	r.Use(rateLimitMw.Handler())

	r.GET("/health", healthHandler.Health)
	r.GET("/api/v1/metrics", metricsHandler.GetMetrics)

	router := proxy.NewRouter(cfg.Routes)
	go func() {
		if err := http.ListenAndServe(":"+cfg.Server.Port, r); err != nil {
			log.Fatalf("server failed: %v", err)
		}
	}()

	log.Printf("gateway started on :%s", cfg.Server.Port)

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("shutting down server...")
}