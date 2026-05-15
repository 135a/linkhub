package com.nym.shortlink.core.service.impl;

import com.nym.shortlink.core.service.PerformanceCounterService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能计数服务实现类 — Micrometer Counter 替换 AtomicLong
 */
@Service
public class PerformanceCounterServiceImpl implements PerformanceCounterService {

    private final Counter bloomFilterInterceptCounter;
    private final Counter sentinelBlockCounter;

    public PerformanceCounterServiceImpl(MeterRegistry meterRegistry) {
        this.bloomFilterInterceptCounter = Counter.builder("shortlink_bloom_filter_intercept_total")
                .description("布隆过滤器拦截次数")
                .register(meterRegistry);
        this.sentinelBlockCounter = Counter.builder("shortlink_sentinel_block_total")
                .description("Sentinel 限流触发次数")
                .register(meterRegistry);
    }

    @Override
    public void incrementBloomFilterIntercept() {
        bloomFilterInterceptCounter.increment();
    }

    @Override
    public long getBloomFilterInterceptCount() {
        return (long) bloomFilterInterceptCounter.count();
    }

    @Override
    public void incrementSentinelBlock() {
        sentinelBlockCounter.increment();
    }

    @Override
    public long getSentinelBlockCount() {
        return (long) sentinelBlockCounter.count();
    }
}
