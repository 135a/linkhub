package middleware

import (
	"fmt"

	"github.com/gin-gonic/gin"
)

func LogFormatValidation() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Next()
	}
}

func HealthCheck() gin.HandlerFunc {
	return func(c *gin.Context) {
		fmt.Println("health check")
		c.Next()
	}
}
