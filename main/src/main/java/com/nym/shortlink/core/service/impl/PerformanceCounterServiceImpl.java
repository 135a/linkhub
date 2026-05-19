package com.nym.shortlink.core.service.impl;

import com.nym.shortlink.core.service.PerformanceCounterService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能计数服务实现类 — Micrometer Counter 替换 AtomicLong
 * 该服务使用 Micrometer 库来提供计数功能，用于监控系统中的关键指标
 */
@Service
public class PerformanceCounterServiceImpl implements PerformanceCounterService {

    // 布隆过滤器拦截计数器
    private final Counter bloomFilterInterceptCounter;
    // Sentinel 限流拦截计数器
    private final Counter sentinelBlockCounter;

    /**
     * 构造函数，用于初始化计数器
     * @param meterRegistry Micrometer 注册表，用于注册和获取计数器
     */
    public PerformanceCounterServiceImpl(MeterRegistry meterRegistry) {
        // 创建布隆过滤器拦截计数器
        this.bloomFilterInterceptCounter = Counter.builder("shortlink_bloom_filter_intercept_total")
                .description("布隆过滤器拦截次数")
                .register(meterRegistry);
        // 创建 Sentinel 限流拦截计数器
        this.sentinelBlockCounter = Counter.builder("shortlink_sentinel_block_total")
                .description("Sentinel 限流触发次数")
                .register(meterRegistry);
    }

    /**
     * 增加布隆过滤器拦截计数
     */
    @Override
    public void incrementBloomFilterIntercept() {
        bloomFilterInterceptCounter.increment();
    }

    /**
     * 获取布隆过滤器拦截计数
     * @return 布隆过滤器拦截次数
     */
    @Override
    public long getBloomFilterInterceptCount() {
        return (long) bloomFilterInterceptCounter.count();
    }

    /**
     * 增加 Sentinel 限流拦截计数
     */
    @Override
    public void incrementSentinelBlock() {
        sentinelBlockCounter.increment();
    }

    /**
     * 获取 Sentinel 限流拦截计数
     * @return Sentinel 限流触发次数
     */
    @Override
    public long getSentinelBlockCount() {
        return (long) sentinelBlockCounter.count();
    }
}
