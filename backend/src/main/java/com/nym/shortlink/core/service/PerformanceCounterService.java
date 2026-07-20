package com.nym.shortlink.core.service;

/**
 * 性能计数服务接口
 */
public interface PerformanceCounterService {

    /**
     * 增加布隆过滤器拦截计数
     */
    void incrementBloomFilterIntercept();

    /**
     * 获取布隆过滤器拦截总次数
     */
    long getBloomFilterInterceptCount();

    /**
     * 增加 Sentinel 限流触发计数
     */
    void incrementSentinelBlock();

    /**
     * 获取 Sentinel 限流触发总次数
     */
    long getSentinelBlockCount();
}
