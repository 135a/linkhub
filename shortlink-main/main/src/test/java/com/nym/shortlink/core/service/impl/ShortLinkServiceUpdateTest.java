package com.nym.shortlink.core.service.impl;

import com.nym.shortlink.core.common.convention.exception.ClientException;
import com.nym.shortlink.core.dao.entity.ShortLinkDO;
import com.nym.shortlink.core.dao.entity.ShortLinkGotoDO;
import com.nym.shortlink.core.dao.mapper.ShortLinkGotoMapper;
import com.nym.shortlink.core.dao.mapper.ShortLinkMapper;
import com.nym.shortlink.core.dto.req.ShortLinkUpdateReqDTO;
import com.nym.shortlink.core.mq.producer.ShortLinkStatsSaveProducer;
import com.nym.shortlink.core.service.CacheMonitoringService;
import com.nym.shortlink.core.service.PerformanceCounterService;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkService - 更新短链接")
class ShortLinkServiceUpdateTest {

    @Mock
    private ShortLinkMapper shortLinkMapper;
    @Mock
    private ShortLinkGotoMapper shortLinkGotoMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RReadWriteLock readWriteLock;
    @Mock
    private RLock writeLock;
    @Mock
    private RBloomFilter<String> bloomFilter;
    @Mock
    private ShortLinkStatsSaveProducer statsSaveProducer;
    @Mock
    private CacheMonitoringService cacheMonitoringService;
    @Mock
    private PerformanceCounterService performanceCounterService;
    @Mock
    private Cache<String, String> redirectCache;

    @InjectMocks
    private ShortLinkServiceImpl shortLinkService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(shortLinkService, "baseMapper", shortLinkMapper);
    }

    @Test
    @DisplayName("跨组移动使用读写锁")
    void crossGroupUpdateUsesReadWriteLock() {
        ShortLinkDO existing = ShortLinkDO.builder()
                .domain("s.test/")
                .shortUri("abc123")
                .originUrl("https://example.com/url")
                .gid("old-gid")
                .fullShortUrl("s.test/abc123")
                .createdType(0)
                .validDateType(0)
                .enableStatus(0)
                .totalPv(0)
                .totalUv(0)
                .totalUip(0)
                .build();

        when(shortLinkMapper.selectOne(any())).thenReturn(existing);
        when(redissonClient.getReadWriteLock(anyString())).thenReturn(readWriteLock);
        when(readWriteLock.writeLock()).thenReturn(writeLock);
        when(writeLock.isHeldByCurrentThread()).thenReturn(true);
        when(shortLinkMapper.update(any(), any())).thenReturn(1);
        when(shortLinkMapper.insert(any(ShortLinkDO.class))).thenReturn(1);
        when(shortLinkGotoMapper.selectOne(any())).thenReturn(
                ShortLinkGotoDO.builder().gid("old-gid").fullShortUrl("s.test/abc123").build());
        when(shortLinkGotoMapper.delete(any())).thenReturn(1);
        when(shortLinkGotoMapper.insert(any(ShortLinkGotoDO.class))).thenReturn(1);

        ShortLinkUpdateReqDTO req = new ShortLinkUpdateReqDTO();
        req.setFullShortUrl("s.test/abc123");
        req.setOriginGid("old-gid");
        req.setGid("new-gid");
        req.setOriginUrl("https://example.com/url");
        req.setValidDateType(0);

        assertDoesNotThrow(() -> shortLinkService.updateShortLink(req));
        verify(writeLock).lock();
        verify(writeLock).unlock();
    }
}
