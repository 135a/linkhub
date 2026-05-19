package com.nym.shortlink.core.service;

import com.nym.shortlink.core.dao.entity.ShortLinkDO;
import com.nym.shortlink.core.dao.mapper.ShortLinkGotoMapper;
import com.nym.shortlink.core.dao.mapper.ShortLinkMapper;
import com.nym.shortlink.core.mq.producer.ShortLinkStatsSaveProducer;
import com.github.benmanes.caffeine.cache.Cache;
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
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.nym.shortlink.core.service.impl.ShortLinkServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BloomFilter - 布隆过滤器穿透保护")
class BloomFilterPenetrationTest {

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
    @DisplayName("布隆过滤器判否且 DB 也不存在时返回 404")
    void bloomFilterFalseAndDbMissReturnsNotFound() throws Exception {
        String fullShortUrl = "s.test/nonexist";
        when(redirectCache.getIfPresent(fullShortUrl)).thenReturn(null);
        when(valueOperations.get(anyString())).thenReturn(null); // Redis miss
        when(bloomFilter.contains(fullShortUrl)).thenReturn(false);
        when(shortLinkMapper.selectOne(any())).thenReturn(null);

        shortLinkService.restoreUrl("nonexist", request, response);

        verify(performanceCounterService).incrementBloomFilterIntercept();
        verify(response).sendRedirect("/page/notfound");
    }

    @Test
    @DisplayName("布隆过滤器误报时降级走完完整链路")
    void bloomFilterFalsePositiveDegradesGracefully() throws Exception {
        String fullShortUrl = "s.test/falsepos";
        when(redirectCache.getIfPresent(fullShortUrl)).thenReturn(null);
        when(valueOperations.get(anyString())).thenReturn(null); // Redis miss
        when(bloomFilter.contains(fullShortUrl)).thenReturn(true);
        // 加锁后查库也找不到——误报的结果
        when(redissonClient.getLock(anyString())).thenReturn(mock(org.redisson.api.RLock.class));
        when(shortLinkGotoMapper.selectOne(any())).thenReturn(null);

        shortLinkService.restoreUrl("falsepos", request, response);

        // 应设置空值缓存防穿透
        verify(valueOperations).set(contains("is-null:goto_"), eq("-"), eq(30L), any());
        verify(response).sendRedirect("/page/notfound");
    }
}
