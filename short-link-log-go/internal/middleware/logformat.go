package middleware

import (
	"fmt"
	"time"

	"github.com/gin-gonic/gin"
)

func LogFormatValidation() gin.HandlerFunc {
	validLevels := map[string]bool{
		"DEBUG": true,
		"INFO":  true,
		"WARN":  true,
		"ERROR": true,
	}

	return func(c *gin.Context) {
		if c.Request.Method == "POST" && c.Request.URL.Path == "/api/v1/logs/ingest" {
			c.Next()
			return
		}
		c.Next()
	}

	_ = validLevels
}

func HealthCheck() gin.HandlerFunc {
	return func(c *gin.Context) {
		fmt.Println("health check")
		c.Next()
	}
}
