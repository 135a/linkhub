package ratelimit

import (
	"context"
	"fmt"
	"sync"
	"time"

	"github.com/go-redis/redis/v8"
)

type Limiter interface {
	Allow(key string, qps int) (bool, error)
	Algorithm() string
}

type TokenBucket struct {
	mu       sync.Mutex
	capacity int
	tokens   float64
	rate     float64
	lastTime time.Time
}

func NewTokenBucket(capacity int, qps int) *TokenBucket {
	return &TokenBucket{
		capacity: capacity,
		tokens:   float64(capacity),
		rate:     float64(qps),
		lastTime: time.Now(),
	}
}

func (tb *TokenBucket) Allow() bool {
	tb.mu.Lock()
	defer tb.mu.Unlock()

	now := time.Now()
	elapsed := now.Sub(tb.lastTime).Seconds()
	tb.lastTime = now

	tb.tokens += elapsed * tb.rate
	if tb.tokens > float64(tb.capacity) {
		tb.tokens = float64(tb.capacity)
	}

	if tb.tokens >= 1 {
		tb.tokens--
		return true
	}
	return false
}

func (tb *TokenBucket) Remaining() float64 {
	tb.mu.Lock()
	defer tb.mu.Unlock()
	return tb.tokens
}

type TokenBucketRateLimiter struct {
	mu           sync.RWMutex
	buckets      map[string]*TokenBucket
	redisClient  *redis.Client
	redisEnabled bool
	ctx          context.Context
}

func NewTokenBucketRateLimiter(redisClient *redis.Client) *TokenBucketRateLimiter {
	rl := &TokenBucketRateLimiter{
		buckets:     make(map[string]*TokenBucket),
		redisClient: redisClient,
		ctx:         context.Background(),
	}
	if redisClient != nil {
		rl.redisEnabled = true
	}
	return rl
}

func NewRateLimiter(redisClient *redis.Client) *TokenBucketRateLimiter {
	return NewTokenBucketRateLimiter(redisClient)
}

func (rl *TokenBucketRateLimiter) Algorithm() string {
	return "token_bucket"
}

func (rl *TokenBucketRateLimiter) Allow(key string, qps int) (bool, error) {
	rl.mu.RLock()
	bucket, exists := rl.buckets[key]
	rl.mu.RUnlock()

	if !exists {
		rl.mu.Lock()
		if _, exists = rl.buckets[key]; !exists {
			rl.buckets[key] = NewTokenBucket(qps*2, qps)
		}
		bucket = rl.buckets[key]
		rl.mu.Unlock()
	}

	if bucket.Allow() {
		return true, nil
	}

	if rl.redisEnabled {
		return rl.allowRedis(key, qps)
	}

	return false, nil
}

func (rl *TokenBucketRateLimiter) allowRedis(key string, qps int) (bool, error) {
	redisKey := fmt.Sprintf("ratelimit:%s", key)

	allowed, err := rl.redisClient.SetNX(rl.ctx, redisKey, 1, time.Second).Result()
	if err != nil {
		return false, err
	}

	countKey := fmt.Sprintf("%s:count", redisKey)
	count, err := rl.redisClient.Incr(rl.ctx, countKey).Result()
	if err != nil {
		return false, err
	}

	ttl, _ := rl.redisClient.TTL(rl.ctx, countKey).Result()
	if ttl > 0 {
		rl.redisClient.Expire(rl.ctx, countKey, time.Second)
	}

	if count > int64(qps) {
		return false, nil
	}

	return allowed, nil
}

func (rl *TokenBucketRateLimiter) GetRemaining(key string) float64 {
	rl.mu.RLock()
	defer rl.mu.RUnlock()

	if bucket, exists := rl.buckets[key]; exists {
		return bucket.Remaining()
	}
	return 0
}