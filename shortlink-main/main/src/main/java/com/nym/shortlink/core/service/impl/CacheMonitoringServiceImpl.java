package com.nym.shortlink.core.service.impl;

import com.nym.shortlink.core.service.CacheMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 缓存命中率监控服务实现
 */
@Slf4j
@Service
public class CacheMonitoringServiceImpl implements CacheMonitoringService {

    private static final String CACHE_HIT_KEY = "short-link:stats:cache:hit:daily:";
    private static final String CACHE_MISS_KEY = "short-link:stats:cache:miss:daily:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService MONITOR_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread thread = new Thread(r);
        thread.setName("cache-monitor-" + thread.getId());
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void recordHitAsync() {
        MONITOR_EXECUTOR.submit(() -> {
            try {
                String today = LocalDate.now().toString();
                String key = CACHE_HIT_KEY + today;
                stringRedisTemplate.opsForValue().increment(key);
                stringRedisTemplate.expire(key, 7, TimeUnit.DAYS);
            } catch (Exception e) {
                log.error("记录缓存命中失败", e);
            }
        });
    }

    @Override
    public void recordMissAsync() {
        MONITOR_EXECUTOR.submit(() -> {
            try {
                String today = LocalDate.now().toString();
                String key = CACHE_MISS_KEY + today;
                stringRedisTemplate.opsForValue().increment(key);
                stringRedisTemplate.expire(key, 7, TimeUnit.DAYS);
            } catch (Exception e) {
                log.error("记录缓存未命中失败", e);
            }
        });
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
