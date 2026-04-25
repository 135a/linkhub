package ratelimit

import "github.com/go-redis/redis/v8"

// NewLimiter returns a Limiter for the given algorithm name.
// Currently only "token_bucket" is supported; unknown names also use token_bucket.
func NewLimiter(algorithm string, redisClient *redis.Client) Limiter {
	switch algorithm {
	case "token_bucket", "":
		return NewTokenBucketRateLimiter(redisClient)
	default:
		return NewTokenBucketRateLimiter(redisClient)
	}
}
