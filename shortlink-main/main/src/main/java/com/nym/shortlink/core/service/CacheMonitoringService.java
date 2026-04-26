package com.nym.shortlink.core.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 缓存命中率监控服务
 */
public interface CacheMonitoringService {

    /**
     * 异步记录缓存命中
     */
    void recordHitAsync();

    /**
     * 异步记录缓存未命中
     */
    void recordMissAsync();

    /**
     * 获取今日缓存命中率
     *
     * @return 缓存命中率统计
     */
    CacheHitRateDTO getTodayHitRate();

    /**
     * 获取最近7天缓存命中率趋势
     *
     * @return 7天缓存命中率列表
     */
    List<CacheHitRateDTO> getLast7DaysHitRate();

    /**
     * 缓存命中率数据传输对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CacheHitRateDTO {
        /**
         * 日期
         */
        private String date;

        /**
         * 缓存命中次数
         */
        private Long hitCount;

        /**
         * 缓存未命中次数
         */
        private Long missCount;

        /**
         * 总请求次数
         */
        private Long totalCount;

        /**
         * 缓存命中率（百分比）
         */
        private Double hitRate;
    }
}
