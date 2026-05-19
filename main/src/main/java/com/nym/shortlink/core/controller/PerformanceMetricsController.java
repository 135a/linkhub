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
 * 提供系统性能相关的汇总数据，包括缓存命中率、布隆过滤器拦截次数、熔断器拦截次数等
 */
@RestController
@RequiredArgsConstructor
public class PerformanceMetricsController {



    // Redis 缓存命中相关键名常量
    private static final String CACHE_L1_HIT_KEY = "short-link:stats:cache:l1hit:daily:";    // L1缓存命中键名



    // 依赖服务注入
    private final CacheMonitoringService cacheMonitoringService;    // 缓存监控服务
    private final PerformanceCounterService performanceCounterService;  // 性能计数器服务

    @Autowired
    private StringRedisTemplate stringRedisTemplate;  // Redis操作模板

    /**
     * 获取性能指标汇总信息
     * @return 包含缓存命中率、布隆过滤器拦截次数、熔断器拦截次数等信息的响应对象
     */
    @GetMapping("/api/short-link/v1/metrics/summary")
    public Result<PerformanceSummaryRespDTO> getPerformanceSummary() {
        // 获取今日缓存命中率数据
        CacheHitRateDTO todayHitRate = cacheMonitoringService.getTodayHitRate();

        // 计算 L1 命中率
        String today = LocalDate.now().toString();  // 获取当前日期字符串
        // 从Redis获取L1缓存命中次数
        String l1HitStr = stringRedisTemplate.opsForValue().get(CACHE_L1_HIT_KEY + today);
        long l1HitCount = l1HitStr != null ? Long.parseLong(l1HitStr) : 0;
        long totalCount = todayHitRate.getTotalCount();
        // 计算L1命中率，保留两位小数
        double l1HitRate = totalCount == 0 ? 0.0 : Math.round((double) l1HitCount / totalCount * 10000.0) / 100.0;
        double l2HitRate = todayHitRate.getHitRate() != null ? todayHitRate.getHitRate() : 0.0;

        // 构建响应对象
        PerformanceSummaryRespDTO response = PerformanceSummaryRespDTO.builder()
                .cacheHitRate(todayHitRate.getHitRate() + "%")                    // L2缓存命中率
                .cacheHitCount(todayHitRate.getHitCount())                         // L2缓存命中次数
                .cacheMissCount(todayHitRate.getMissCount())                       // 缓存未命中次数
                .bloomFilterInterceptCount(performanceCounterService.getBloomFilterInterceptCount())  // 布隆过滤器拦截次数
                .sentinelBlockCount(performanceCounterService.getSentinelBlockCount())                // 熔断器拦截次数
                .todayRedirectTotal(totalCount)                                   // 今日重定向总数
                .l1CacheHitCount(l1HitCount)                                     // L1缓存命中次数
                .l1CacheHitRate(l1HitRate + "%")                                 // L1缓存命中率
                .l2CacheHitRate(l2HitRate + "%")                                 // L2缓存命中率
                .build();
        return Results.success(response);
    }

    /**
     * 性能指标汇总响应DTO
     * 包含系统各项性能指标的汇总数据
     */
    @Data
    @Builder
    public static class PerformanceSummaryRespDTO {
        private String cacheHitRate;          // L2缓存命中率（百分比字符串）
        private long cacheHitCount;           // L2缓存命中次数
        private long cacheMissCount;          // 缓存未命中次数
        private long bloomFilterInterceptCount; // 布隆过滤器拦截次数
        private long sentinelBlockCount;      // 熔断器拦截次数
        private long todayRedirectTotal;      // 今日重定向总数
        /** L1 Caffeine 本地缓存命中次数 */
        private long l1CacheHitCount;
        /** L1 Caffeine 命中率（百分比字符串） */
        private String l1CacheHitRate;
        /** L2 Redis 命中率（百分比字符串，含 L1 命中） */
        private String l2CacheHitRate;
    }
}
