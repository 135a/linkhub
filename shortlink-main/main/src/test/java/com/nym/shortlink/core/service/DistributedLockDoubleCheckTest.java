package com.nym.shortlink.core.service;

import com.nym.shortlink.core.dao.entity.ShortLinkDO;
import com.nym.shortlink.core.dao.entity.ShortLinkGotoDO;
import com.nym.shortlink.core.dao.mapper.ShortLinkGotoMapper;
import com.nym.shortlink.core.dao.mapper.ShortLinkMapper;
import com.nym.shortlink.core.mq.producer.ShortLinkStatsSaveProducer;
import com.github.benmanes.caffeine.cache.Cache;
import com.nym.shortlink.core.service.impl.ShortLinkServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DistributedLock - 分布式锁双重检查")
class DistributedLockDoubleCheckTest {

    @Mock
    private ShortLinkMapper shortLinkMapper;
    @Mock
    private ShortLinkGotoMapper shortLinkGotoMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private Cache<String, String> redirectCache;
    @Mock
    private RBloomFilter<String> bloomFilter;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock rLock;
    @Mock
    private ShortLinkStatsSaveProducer statsSaveProducer;
    @Mock
    private CacheMonitoringService cacheMonitoringService;
    @Mock
    private PerformanceCounterService performanceCounterService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private ShortLinkServiceImpl shortLinkService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(request.getServerName()).thenReturn("s.test");
        lenient().when(request.getServerPort()).thenReturn(80);
        lenient().when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0) Chrome/120.0");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
        lenient().when(request.getCookies()).thenReturn(null);
        lenient().when(request.getRemoteAddr()).thenReturn("203.0.113.1");
        ReflectionTestUtils.setField(shortLinkService, "baseMapper", shortLinkMapper);
        ReflectionTestUtils.setField(shortLinkService, "meterRegistry", new SimpleMeterRegistry());
        try {
            java.lang.reflect.Method initMetrics = ShortLinkServiceImpl.class.getDeclaredMethod("initMetrics");
            initMetrics.setAccessible(true);
            initMetrics.invoke(shortLinkService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("加锁后 Redis 已被其他线程回填")
    void doubleCheckRedisIsBackfilledAfterLock() throws Exception {
        String fullShortUrl = "s.test/backfilled";
        when(redirectCache.getIfPresent(fullShortUrl)).thenReturn(null);
        // 第一次查询 Redis 为空, 双重检查时已有数据
        when(valueOperations.get(contains("goto:"))).thenReturn(null, "https://example.com/backfilled");
        when(bloomFilter.contains(fullShortUrl)).thenReturn(true);
        when(valueOperations.get(contains("is-null:goto_"))).thenReturn(null);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);

        shortLinkService.restoreUrl("backfilled", request, response);

        verify(cacheMonitoringService, atLeastOnce()).recordHitAsync();
        verify(redirectCache, atLeastOnce()).put(eq(fullShortUrl), eq("https://example.com/backfilled"));
        verify(response).sendRedirect("https://example.com/backfilled");
        // 不应查询数据库
        verify(shortLinkMapper, never()).selectOne(any());
    }
}
