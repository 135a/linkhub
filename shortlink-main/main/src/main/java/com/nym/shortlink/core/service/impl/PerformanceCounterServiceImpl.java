package com.nym.shortlink.core.service.impl;

import com.nym.shortlink.core.service.PerformanceCounterService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能计数服务实现类
 */
@Service
public class PerformanceCounterServiceImpl implements PerformanceCounterService {

    private final AtomicLong bloomFilterInterceptCount = new AtomicLong(0);
    private final AtomicLong sentinelBlockCount = new AtomicLong(0);

    @Override
    public void incrementBloomFilterIntercept() {
        bloomFilterInterceptCount.incrementAndGet();
    }

    @Override
    public long getBloomFilterInterceptCount() {
        return bloomFilterInterceptCount.get();
    }

    @Override
    public void incrementSentinelBlock() {
        sentinelBlockCount.incrementAndGet();
    }

    @Override
    public long getSentinelBlockCount() {
        return sentinelBlockCount.get();
    }
}
