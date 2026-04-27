package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.service.CacheMonitoringService;
import com.nym.shortlink.core.service.PerformanceCounterService;
import com.nym.shortlink.core.service.CacheMonitoringService.CacheHitRateDTO;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 性能指标聚合接口
 */
@RestController
@RequiredArgsConstructor
public class PerformanceMetricsController {

    private final CacheMonitoringService cacheMonitoringService;
    private final PerformanceCounterService performanceCounterService;

    @GetMapping("/api/short-link/v1/metrics/summary")
    public Result<PerformanceSummaryRespDTO> getPerformanceSummary() {
        CacheHitRateDTO todayHitRate = cacheMonitoringService.getTodayHitRate();
        PerformanceSummaryRespDTO response = PerformanceSummaryRespDTO.builder()
                .cacheHitRate(todayHitRate.getHitRate() + "%")
                .cacheHitCount(todayHitRate.getHitCount())
                .cacheMissCount(todayHitRate.getMissCount())
                .bloomFilterInterceptCount(performanceCounterService.getBloomFilterInterceptCount())
                .sentinelBlockCount(performanceCounterService.getSentinelBlockCount())
                .todayRedirectTotal(todayHitRate.getTotalCount())
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
    }
}
