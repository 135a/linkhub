package com.nym.shortlink.project.controller;

import com.nym.shortlink.project.common.convention.result.Result;
import com.nym.shortlink.project.common.convention.result.Results;
import com.nym.shortlink.project.service.CacheMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 缓存监控控制层
 */
@RestController
@RequestMapping("/api/short-link/v1/monitor")
@RequiredArgsConstructor
public class CacheMonitorController {

    private final CacheMonitoringService cacheMonitoringService;

    /**
     * 获取今日缓存命中率
     */
    @GetMapping("/cache-hit-rate/today")
    public Result<CacheMonitoringService.CacheHitRateDTO> getTodayHitRate() {
        return Results.success(cacheMonitoringService.getTodayHitRate());
    }

    /**
     * 获取最近7天缓存命中率趋势
     */
    @GetMapping("/cache-hit-rate/last-7-days")
    public Result<List<CacheMonitoringService.CacheHitRateDTO>> getLast7DaysHitRate() {
        return Results.success(cacheMonitoringService.getLast7DaysHitRate());
    }
}
