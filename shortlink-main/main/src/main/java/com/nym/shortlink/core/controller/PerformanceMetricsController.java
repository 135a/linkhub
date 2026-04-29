package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.service.CacheMonitoringService;
import com.nym.shortlink.core.service.PerformanceCounterService;
import com.nym.shortlink.core.service.CacheMonitoringService.CacheHitRateDTO;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 性能指标聚合接口
 */
@RestController
@RequiredArgsConstructor
public class PerformanceMetricsController {

    private static final String CACHE_HIT_KEY = "short-link:stats:cache:hit:daily:";
    private static final String CACHE_L1_HIT_KEY = "short-link:stats:cache:l1hit:daily:";
    private static final String CACHE_MISS_KEY = "short-link:stats:cache:miss:daily:";

    private final CacheMonitoringService cacheMonitoringService;
    private final PerformanceCounterService performanceCounterService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/api/short-link/v1/metrics/summary")
    public Result<PerformanceSummaryRespDTO> getPerformanceSummary() {
        CacheHitRateDTO todayHitRate = cacheMonitoringService.getTodayHitRate();

        // 计算 L1 命中率
        String today = LocalDate.now().toString();
        String l1HitStr = stringRedisTemplate.opsForValue().get(CACHE_L1_HIT_KEY + today);
        long l1HitCount = l1HitStr != null ? Long.parseLong(l1HitStr) : 0;
        long totalCount = todayHitRate.getTotalCount();
        double l1HitRate = totalCount == 0 ? 0.0 : Math.round((double) l1HitCount / totalCount * 10000.0) / 100.0;
        double l2HitRate = todayHitRate.getHitRate() != null ? todayHitRate.getHitRate() : 0.0;

        PerformanceSummaryRespDTO response = PerformanceSummaryRespDTO.builder()
                .cacheHitRate(todayHitRate.getHitRate() + "%")
                .cacheHitCount(todayHitRate.getHitCount())
                .cacheMissCount(todayHitRate.getMissCount())
                .bloomFilterInterceptCount(performanceCounterService.getBloomFilterInterceptCount())
                .sentinelBlockCount(performanceCounterService.getSentinelBlockCount())
                .todayRedirectTotal(totalCount)
                .l1CacheHitCount(l1HitCount)
                .l1CacheHitRate(l1HitRate + "%")
                .l2CacheHitRate(l2HitRate + "%")
                .build();
        return Results.success(response);
    }

    @Data
    @Builder
    public static class PerformanceSummaryRespDTO {
        private String cacheHitRate;
        private long cacheHitCount;
        private long cacheMissCount;
        private long bloomFilterInterceptCount;
        private long sentinelBlockCount;
        private long todayRedirectTotal;
        /** L1 Caffeine 本地缓存命中次数 */
        private long l1CacheHitCount;
        /** L1 Caffeine 命中率（百分比字符串） */
        private String l1CacheHitRate;
        /** L2 Redis 命中率（百分比字符串，含 L1 命中） */
        private String l2CacheHitRate;
    }
}
