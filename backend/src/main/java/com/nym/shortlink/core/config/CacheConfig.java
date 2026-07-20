package com.nym.shortlink.core.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine L1 本地缓存配置
 * <p>
 * 三级缓存链路：L1 Caffeine（JVM 内存）→ L2 Redis（远程缓存）→ L3 MySQL（数据库）
 * key 格式与 Redis 保持一致：goto:{fullShortUrl}
 */
@Configuration
public class CacheConfig {

    @Value("${caffeine.short-link-redirect.maximum-size:10000}")
    private long maximumSize;

    @Value("${caffeine.short-link-redirect.expire-after-write:30s}")
    private Duration expireAfterWrite;

    /**
     * 短链接跳转 L1 本地缓存 Bean
     * key: fullShortUrl（与 Redis GOTO_SHORT_LINK_KEY 保持相同的 fullShortUrl 值）
     * value: originUrl（原始 URL）
     */
    @Bean
    public Cache<String, String> redirectCache() {
        return Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expireAfterWrite)
                .recordStats()  // 开启命中率统计，供 /metrics/summary 使用
                .build();
    }
}
