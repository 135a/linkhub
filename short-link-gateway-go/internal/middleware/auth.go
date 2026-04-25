package middleware

import (
	"context"
	"log"
	"net/http"
	"net/url"
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
		method := c.Request.Method

		// Allow POST to /api/short-link/admin/v1/user for user registration
		if method == http.MethodPost && path == "/api/short-link/admin/v1/user" {
			c.Next()
			return
		}

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

		username := c.GetHeader("username")

		if token == "" || username == "" {
			traceID, _ := c.Get("trace_id")
			log.Printf("[WARN] AuthToken: missing token or username path=%s trace_id=%v", path, traceID)
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"code":    "UNAUTHORIZED",
				"message": "Missing authentication token or username",
			})
			return
		}

		slidingKey := "short-link:login:" + username
		absoluteKey := "short-link:login:abs:" + username
		ctx, cancel := context.WithTimeout(context.Background(), 500*time.Millisecond)
		defer cancel()

		// The Java backend stores tokens in a Hash where the field is the token UUID.
		// It also uses an absolute TTL key to enforce a hard session timeout.

		// First check if the absolute sentinel exists
		absExists, err := redisClient.Exists(ctx, absoluteKey).Result()
		if err != nil {
			log.Printf("[WARN] AuthToken: redis absolute check failed path=%s err=%v, fail open", path, err)
			c.Next()
			return
		}

		if absExists == 0 {
			traceID, _ := c.Get("trace_id")
			log.Printf("[WARN] AuthToken: absolute session expired path=%s trace_id=%v", path, traceID)
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"code":    "UNAUTHORIZED",
				"message": "Session expired (absolute)",
			})
			return
		}

		// Then check if the token exists in the sliding Hash
		tokenExists, err := redisClient.HExists(ctx, slidingKey, token).Result()
		if err != nil {
			log.Printf("[WARN] AuthToken: redis sliding check failed path=%s err=%v, fail open", path, err)
			c.Next()
			return
		}

		if !tokenExists {
			traceID, _ := c.Get("trace_id")
			log.Printf("[WARN] AuthToken: invalid/expired token path=%s trace_id=%v", path, traceID)
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"code":    "UNAUTHORIZED",
				"message": "Invalid or expired token",
			})
			return
		}

		// URL-encode user identity headers to ensure non-ASCII characters (like Chinese)
		// survive transmission to backend services.
		headersToEncode := []string{"username", "user-name", "real-name"}
		for _, h := range headersToEncode {
			if val := c.GetHeader(h); val != "" {
				c.Request.Header.Set(h, url.QueryEscape(val))
			}
		}

		c.Next()
	}
}
