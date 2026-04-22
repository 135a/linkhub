package middleware

import (
	"net/http"
	"shortlink-gateway-go/internal/config"

	"github.com/gin-gonic/gin"
)

func CORS() gin.HandlerFunc {
	return func(c *gin.Context) {
		cfg := config.Get()
		if cfg == nil {
			c.Next()
			return
		}

		origin := c.GetHeader("Origin")
		if isOriginAllowed(origin, cfg.CORS.AllowedOrigins) {
			c.Header("Access-Control-Allow-Origin", origin)
			c.Header("Access-Control-Allow-Credentials", boolToString(cfg.CORS.AllowCredentials))
			c.Header("Access-Control-Expose-Headers", joinStrings(cfg.CORS.ExposeHeaders, ", "))
		}

		if c.Request.Method == http.MethodOptions {
			c.Header("Access-Control-Allow-Methods", joinStrings(cfg.CORS.AllowedMethods, ", "))
			c.Header("Access-Control-Allow-Headers", joinStrings(cfg.CORS.AllowedHeaders, ", "))
			c.Header("Access-Control-Max-Age", intToString(cfg.CORS.MaxAge))
			c.AbortWithStatus(http.StatusNoContent)
			return
		}

		c.Next()
	}
}

func isOriginAllowed(origin string, allowedOrigins []string) bool {
	for _, allowed := range allowedOrigins {
		if allowed == "*" || allowed == origin {
			return true
		}
	}
	return false
}

func boolToString(b bool) string {
	if b {
		return "true"
	}
	return "false"
}

func joinStrings(strs []string, sep string) string {
	if len(strs) == 0 {
		return ""
	}
	result := strs[0]
	for i := 1; i < len(strs); i++ {
		result += sep + strs[i]
	}
	return result
}

func intToString(i int) string {
	return string(rune('0'+i))
}