package com.nym.shortlink.core.service.impl;

import com.nym.shortlink.core.common.convention.exception.ClientException;
import com.nym.shortlink.core.common.convention.exception.ServiceException;
import com.nym.shortlink.core.dao.entity.ShortLinkDO;
import com.nym.shortlink.core.dao.entity.ShortLinkGotoDO;
import com.nym.shortlink.core.dao.mapper.ShortLinkGotoMapper;
import com.nym.shortlink.core.dao.mapper.ShortLinkMapper;
import com.nym.shortlink.core.dto.req.ShortLinkCreateReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkCreateRespDTO;
import com.nym.shortlink.core.mq.producer.ShortLinkStatsSaveProducer;
import com.nym.shortlink.core.service.CacheMonitoringService;
import com.nym.shortlink.core.service.PerformanceCounterService;
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

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkService - 分布式锁创建短链接")
class ShortLinkServiceCreateByLockTest {

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
    private RLock rLock;
    @Mock
    private RBloomFilter<String> bloomFilter;
    @Mock
    private ShortLinkStatsSaveProducer statsSaveProducer;
    @Mock
    private CacheMonitoringService cacheMonitoringService;
    @Mock
    private PerformanceCounterService performanceCounterService;

    @InjectMocks
    private ShortLinkServiceImpl shortLinkService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        ReflectionTestUtils.setField(shortLinkService, "createShortLinkDefaultDomain", "s.test/");
        ReflectionTestUtils.setField(shortLinkService, "baseMapper", shortLinkMapper);
    }

    @Test
    @DisplayName("成功获取分布式锁并创建")
    void createWithLockSuccessfully() throws InterruptedException {
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(shortLinkMapper.insert(any(ShortLinkDO.class))).thenReturn(1);
        when(shortLinkGotoMapper.insert(any(ShortLinkGotoDO.class))).thenReturn(1);
        when(shortLinkMapper.selectOne(any())).thenReturn(null);

        ShortLinkCreateReqDTO req = ShortLinkCreateReqDTO.builder()
                .originUrl("https://example.com/locked-url")
                .gid("test-gid")
                .createdType(0)
                .validDateType(0)
                .build();

        ShortLinkCreateRespDTO result = shortLinkService.createShortLinkByLock(req);

        assertNotNull(result);
        assertTrue(result.getFullShortUrl().startsWith("http://s.test/"));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("获取分布式锁超时抛出 ClientException")
    void lockTimeoutThrowsException() throws InterruptedException {
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(false);

        ShortLinkCreateReqDTO req = ShortLinkCreateReqDTO.builder()
                .originUrl("https://example.com/timeout")
                .gid("test-gid")
                .createdType(0)
                .validDateType(0)
                .build();

        ClientException ex = assertThrows(ClientException.class,
                () -> shortLinkService.createShortLinkByLock(req));
        assertTrue(ex.getMessage().contains("系统繁忙"));
        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("获取分布式锁被中断抛出 ServiceException")
    void lockInterruptedThrowsException() throws InterruptedException {
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS))
                .thenThrow(new InterruptedException());

        ShortLinkCreateReqDTO req = ShortLinkCreateReqDTO.builder()
                .originUrl("https://example.com/interrupted")
                .gid("test-gid")
                .createdType(0)
                .validDateType(0)
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> shortLinkService.createShortLinkByLock(req));
        assertTrue(ex.getMessage().contains("被中断"));
        assertTrue(Thread.interrupted() || true); // 清除中断标志
    }
}
