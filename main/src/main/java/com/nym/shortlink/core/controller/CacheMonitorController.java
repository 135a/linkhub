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
 * 提供缓存监控相关的HTTP接口，包括获取今日缓存命中率和最近7天缓存命中率趋势
 */
@RestController
@RequestMapping("/api/short-link/v1/monitor")
@RequiredArgsConstructor
@Slf4j
public class CacheMonitorController {

    private final CacheMonitoringService cacheMonitoringService;

    /**
     * 获取今日缓存命中率
     * @return Result<CacheMonitoringService.CacheHitRateDTO> 包含今日缓存命中率的数据传输对象
     */
    @GetMapping("/cache-hit-rate/today")
    public Result<CacheMonitoringService.CacheHitRateDTO> getTodayHitRate() {
        // 调用服务层方法获取今日缓存命中率，并封装成统一返回结果
        Result<CacheMonitoringService.CacheHitRateDTO> result = Results.success(cacheMonitoringService.getTodayHitRate());
        return result;
    }

    /**
     * 获取最近7天缓存命中率趋势
     * @return Result<List<CacheMonitoringService.CacheHitRateDTO>> 包含最近7天缓存命中率趋势的数据传输对象列表
     */
    @GetMapping("/cache-hit-rate/last-7-days")
    public Result<List<CacheMonitoringService.CacheHitRateDTO>> getLast7DaysHitRate() {
        // 调用服务层方法获取最近7天缓存命中率趋势，并封装成统一返回结果
        Result<List<CacheMonitoringService.CacheHitRateDTO>> result = Results.success(cacheMonitoringService.getLast7DaysHitRate());
        return result;
    }
}
