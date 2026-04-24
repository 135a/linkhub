package main

import (
	"log"
	"shortlink-log-go/internal/config"
	"shortlink-log-go/internal/handler"
	"shortlink-log-go/internal/service"
	"shortlink-log-go/internal/store"

	"github.com/gin-gonic/gin"
)

func main() {
	cfg := config.Load()

	chStore, err := store.NewClickHouseStore(cfg.ClickHouseHost, cfg.ClickHousePort, cfg.ClickHouseUser, cfg.ClickHousePassword)
	if err != nil {
		log.Fatalf("failed to init clickhouse: %v", err)
	}
	if err := chStore.Ping(); err != nil {
		log.Fatalf("failed to ping clickhouse at %s:%s: %v", cfg.ClickHouseHost, cfg.ClickHousePort, err)
	}

	mysqlStore, err := store.NewMySQLStore(cfg.MySQLHost, cfg.MySQLPort, cfg.MySQLUser, cfg.MySQLPassword, cfg.MySQLDatabase)
	if err != nil {
		log.Fatalf("failed to init mysql: %v", err)
	}

	logSvc := service.NewLogService(chStore)
	authSvc := service.NewAuthService(mysqlStore, cfg.JWTSecret)

	// Initialize default accounts
	if err := authSvc.Register("admin", "admin123"); err != nil {
		log.Printf("admin account already exists: %v", err)
	}
	if err := authSvc.Register("test", "test123"); err != nil {
		log.Printf("test account already exists: %v", err)
	}
	log.Println("Default accounts initialized")

	logHandler := handler.NewLogHandler(logSvc)
	authHandler := handler.NewAuthHandler(authSvc)
	healthHandler := handler.NewHealthHandler(chStore)
	metricsHandler := handler.NewMetricsHandler(chStore)

	r := gin.Default()

	r.GET("/health", healthHandler.Health)
	r.GET("/metrics", metricsHandler.Metrics)

	v1 := r.Group("/api/v1")
	{
		v1.POST("/logs/ingest", logHandler.Ingest)
		v1.GET("/logs/query", logHandler.Query)
		v1.GET("/logs/services", logHandler.ListServices)
		v1.POST("/auth/register", authHandler.Register)
		v1.POST("/auth/login", authHandler.Login)
	}

	log.Printf("log collector starting on :%s", cfg.Port)
	r.Run(":" + cfg.Port)
}
