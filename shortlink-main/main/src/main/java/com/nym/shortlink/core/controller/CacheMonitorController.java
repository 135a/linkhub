/*
 * Copyright © 2026 NageOffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.service.CacheMonitoringService;
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
