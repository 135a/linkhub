package com.nym.shortlink.core.service.impl;

import com.nym.shortlink.core.service.CacheMonitoringService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存命中率监控服务实现 — 同步 Micrometer Counter 替换异步 DiscardPolicy 线程池
 * 该服务用于监控和记录缓存的命中情况，包括L1缓存命中、L2缓存命中和缓存未命中情况
 * 使用Micrometer进行指标统计，使用Redis进行数据持久化
 */
@Slf4j
@Service
public class CacheMonitoringServiceImpl implements CacheMonitoringService {

    // 缓存命中统计的Redis键前缀
    private static final String CACHE_HIT_KEY = "short-link:stats:cache:hit:daily:";
    private static final String CACHE_MISS_KEY = "short-link:stats:cache:miss:daily:";
    private static final String CACHE_L1_HIT_KEY = "short-link:stats:cache:l1hit:daily:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final Counter l1HitCounter;
    private final Counter l2HitCounter;
    private final Counter missCounter;

    public CacheMonitoringServiceImpl(MeterRegistry meterRegistry) {
        this.l1HitCounter = Counter.builder("shortlink_cache_hit_total")
                .description("缓存命中总数")
                .tag("cache_type", "l1")
                .register(meterRegistry);
        this.l2HitCounter = Counter.builder("shortlink_cache_hit_total")
                .description("缓存命中总数")
                .tag("cache_type", "l2")
                .register(meterRegistry);
        this.missCounter = Counter.builder("shortlink_cache_miss_total")
                .description("缓存未命中总数")
                .register(meterRegistry);
    }

    @Override
    public void recordHitAsync() {
        l2HitCounter.increment();
        incrementRedisDaily(CACHE_HIT_KEY);
    }

    @Override
    public void recordL1HitAsync() {
        l1HitCounter.increment();
        l2HitCounter.increment();
        incrementRedisDaily(CACHE_HIT_KEY);
        incrementRedisDaily(CACHE_L1_HIT_KEY);
    }

    @Override
    public void recordMissAsync() {
        missCounter.increment();
        incrementRedisDaily(CACHE_MISS_KEY);
    }

    private void incrementRedisDaily(String keyPrefix) {
        try {
            String today = LocalDate.now().toString();
            String key = keyPrefix + today;
            stringRedisTemplate.opsForValue().increment(key);
            stringRedisTemplate.expire(key, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("记录缓存统计失败, keyPrefix={}", keyPrefix, e);
        }
    }

    @Override
    public CacheHitRateDTO getTodayHitRate() {
        String today = LocalDate.now().toString();
        String hitStr = stringRedisTemplate.opsForValue().get(CACHE_HIT_KEY + today);
        String missStr = stringRedisTemplate.opsForValue().get(CACHE_MISS_KEY + today);

        long hitCount = hitStr != null ? Long.parseLong(hitStr) : 0;
        long missCount = missStr != null ? Long.parseLong(missStr) : 0;
        long totalCount = hitCount + missCount;

        double hitRate = totalCount == 0 ? 0.0 : (double) hitCount / totalCount * 100;

        return CacheHitRateDTO.builder()
                .date(today)
                .hitCount(hitCount)
                .missCount(missCount)
                .totalCount(totalCount)
                .hitRate(Math.round(hitRate * 100.0) / 100.0)
                .build();
    }

    @Override
    public List<CacheHitRateDTO> getLast7DaysHitRate() {
        List<CacheHitRateDTO> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            String hitStr = stringRedisTemplate.opsForValue().get(CACHE_HIT_KEY + date);
            String missStr = stringRedisTemplate.opsForValue().get(CACHE_MISS_KEY + date);

            long hitCount = hitStr != null ? Long.parseLong(hitStr) : 0;
            long missCount = missStr != null ? Long.parseLong(missStr) : 0;
            long totalCount = hitCount + missCount;
            double hitRate = totalCount == 0 ? 0.0 : (double) hitCount / totalCount * 100;

            result.add(CacheHitRateDTO.builder()
                    .date(date)
                    .hitCount(hitCount)
                    .missCount(missCount)
                    .totalCount(totalCount)
                    .hitRate(Math.round(hitRate * 100.0) / 100.0)
                    .build());
        }
        return result;
    }
}
