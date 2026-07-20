package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.service.CacheMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 缓存监控控制层
 */
@RestController
@RequestMapping("/api/short-link/v1/monitor")
@RequiredArgsConstructor
@Slf4j
public class CacheMonitorController {

    private final CacheMonitoringService cacheMonitoringService;

    /**
     * 获取今日缓存命中率
     */
    @GetMapping("/cache-hit-rate/today")
    public Result<CacheMonitoringService.CacheHitRateDTO> getTodayHitRate() {
        Result<CacheMonitoringService.CacheHitRateDTO> result = Results.success(cacheMonitoringService.getTodayHitRate());
        return result;
    }

    /**
     * 获取最近7天缓存命中率趋势
     */
    @GetMapping("/cache-hit-rate/last-7-days")
    public Result<List<CacheMonitoringService.CacheHitRateDTO>> getLast7DaysHitRate() {
        Result<List<CacheMonitoringService.CacheHitRateDTO>> result = Results.success(cacheMonitoringService.getLast7DaysHitRate());
        return result;
    }
}
