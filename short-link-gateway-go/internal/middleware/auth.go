package middleware

import (
	"context"
	"log"
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/go-redis/redis/v8"
)

// publicPrefixes lists path prefixes that bypass token validation.
var publicPrefixes = []string{
	"/api/short-link/v1/",
	"/api/short-link/admin/v1/user/login",
	"/api/short-link/admin/v1/user/has-username",
	"/api/short-link/admin/v1/user/register",
	"/health",
	"/api/v1/metrics",
}

// AuthToken returns a gin middleware that validates the custom session token
// stored in Redis by the Java admin service.
func AuthToken(redisClient *redis.Client) gin.HandlerFunc {
	return func(c *gin.Context) {
		path := c.Request.URL.Path

		for _, prefix := range publicPrefixes {
			if strings.HasPrefix(path, prefix) {
				c.Next()
				return
			}
		}

		if redisClient == nil {
			log.Printf("[WARN] AuthToken: redis unavailable, skipping token check for %s", path)
			c.Next()
			return
		}

		token := c.GetHeader("token")
		if token == "" {
			auth := c.GetHeader("Authorization")
			if strings.HasPrefix(auth, "Bearer ") {
				token = strings.TrimPrefix(auth, "Bearer ")
			}
		}

		if token == "" {
			traceID, _ := c.Get("trace_id")
			log.Printf("[WARN] AuthToken: missing token path=%s trace_id=%v", path, traceID)
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"code":    "UNAUTHORIZED",
				"message": "Missing authentication token",
			})
			return
		}

		redisKey := "login:" + token
		ctx, cancel := context.WithTimeout(context.Background(), 500*time.Millisecond)
		defer cancel()

		exists, err := redisClient.Exists(ctx, redisKey).Result()
		if err != nil {
			log.Printf("[WARN] AuthToken: redis check failed path=%s err=%v, fail open", path, err)
			c.Next()
			return
		}

		if exists == 0 {
			traceID, _ := c.Get("trace_id")
			log.Printf("[WARN] AuthToken: invalid/expired token path=%s trace_id=%v", path, traceID)
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"code":    "UNAUTHORIZED",
				"message": "Invalid or expired token",
			})
			return
		}

		c.Next()
	}
}
