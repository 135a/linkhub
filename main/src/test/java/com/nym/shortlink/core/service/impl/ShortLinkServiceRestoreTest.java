package com.nym.shortlink.core.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.nym.shortlink.core.dao.entity.ShortLinkDO;
import com.nym.shortlink.core.dao.entity.ShortLinkGotoDO;
import com.nym.shortlink.core.dao.mapper.ShortLinkGotoMapper;
import com.nym.shortlink.core.dao.mapper.ShortLinkMapper;
import com.nym.shortlink.core.mq.producer.ShortLinkStatsSaveProducer;
import com.nym.shortlink.core.service.CacheMonitoringService;
import com.nym.shortlink.core.service.PerformanceCounterService;
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
@DisplayName("ShortLinkService - 重定向多级缓存")
class ShortLinkServiceRestoreTest {

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
        lenient().when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
        lenient().when(request.getCookies()).thenReturn(null);
        lenient().when(request.getRemoteAddr()).thenReturn("203.0.113.1");
        ReflectionTestUtils.setField(shortLinkService, "baseMapper", shortLinkMapper);
        ReflectionTestUtils.setField(shortLinkService, "meterRegistry", new SimpleMeterRegistry());
        shortLinkService.initMetrics();
    }

    @Test
    @DisplayName("L1 Caffeine 缓存命中直接重定向")
    void l1CacheHitRedirectsImmediately() throws Exception {
        String fullShortUrl = "s.test/abc123";
        when(redirectCache.getIfPresent(fullShortUrl)).thenReturn("https://example.com/original");

        shortLinkService.restoreUrl("abc123", request, response);

        verify(cacheMonitoringService).recordL1HitAsync();
        verify(response).sendRedirect("https://example.com/original");
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("L2 Redis 缓存命中回填 L1 并重定向")
    void l2CacheHitsBackfillsL1() throws Exception {
        String fullShortUrl = "s.test/abc123";
        when(redirectCache.getIfPresent(fullShortUrl)).thenReturn(null);
        when(valueOperations.get(contains("goto:"))).thenReturn("https://example.com/l2-url");

        shortLinkService.restoreUrl("abc123", request, response);

        verify(cacheMonitoringService).recordHitAsync();
        verify(redirectCache).put(eq(fullShortUrl), eq("https://example.com/l2-url"));
        verify(response).sendRedirect("https://example.com/l2-url");
    }

    @Test
    @DisplayName("空值缓存命中直接返回 404")
    void nullValueCacheReturnsNotFound() throws Exception {
        String fullShortUrl = "s.test/abc123";
        when(redirectCache.getIfPresent(fullShortUrl)).thenReturn(null);
        when(valueOperations.get(contains("goto:"))).thenReturn(null);
        when(bloomFilter.contains(fullShortUrl)).thenReturn(true);
        when(valueOperations.get(contains("is-null:goto_"))).thenReturn("-");

        shortLinkService.restoreUrl("abc123", request, response);

        verify(cacheMonitoringService).recordMissAsync();
        verify(response).sendRedirect("/page/notfound");
        verify(shortLinkMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("布隆过滤器未命中，按 shortUri 查库找到记录")
    void bloomFilterMissFindsByShortUri() throws Exception {
        String fullShortUrl = "s.test/abc123";
        ShortLinkDO dbRecord = ShortLinkDO.builder()
                .fullShortUrl("other.domain/abc123")
                .shortUri("abc123")
                .originUrl("https://example.com/db-found")
                .gid("test-gid")
                .validDate(new Date(System.currentTimeMillis() + 86400000L))
                .build();

        when(redirectCache.getIfPresent(fullShortUrl)).thenReturn(null);
        when(valueOperations.get(contains("goto:"))).thenReturn(null);
        when(bloomFilter.contains(fullShortUrl)).thenReturn(false);
        when(shortLinkMapper.selectOne(any())).thenReturn(dbRecord);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(valueOperations.get(contains("is-null:goto_"))).thenReturn(null);
        when(shortLinkMapper.selectOne(any())).thenReturn(dbRecord);
        when(valueOperations.get(contains("goto:other.domain/abc123"))).thenReturn("https://example.com/db-found");

        shortLinkService.restoreUrl("abc123", request, response);

        verify(cacheMonitoringService, atLeastOnce()).recordHitAsync();
        verify(response).sendRedirect("https://example.com/db-found");
    }
}
