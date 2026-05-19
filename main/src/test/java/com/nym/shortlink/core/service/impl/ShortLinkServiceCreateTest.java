package com.nym.shortlink.core.service.impl;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkService - 创建短链接")
class ShortLinkServiceCreateTest {

    @Mock
    private ShortLinkMapper shortLinkMapper;
    @Mock
    private ShortLinkGotoMapper shortLinkGotoMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
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
        ReflectionTestUtils.setField(shortLinkService, "createShortLinkDefaultDomain", "s.test/");
        ReflectionTestUtils.setField(shortLinkService, "baseMapper", shortLinkMapper);
    }

    @Test
    @DisplayName("正常创建短链接")
    void createShortLinkSuccessfully() {
        ShortLinkCreateReqDTO req = ShortLinkCreateReqDTO.builder()
                .originUrl("https://example.com/long-url")
                .gid("test-gid")
                .createdType(0)
                .validDateType(0)
                .describe("test link")
                .build();

        when(shortLinkMapper.insert(any(ShortLinkDO.class))).thenReturn(1);
        when(shortLinkGotoMapper.insert(any(ShortLinkGotoDO.class))).thenReturn(1);

        ShortLinkCreateRespDTO result = shortLinkService.createShortLink(req);

        assertNotNull(result);
        assertTrue(result.getFullShortUrl().startsWith("http://s.test/"));
        assertEquals("https://example.com/long-url" , result.getOriginUrl());
        assertEquals("test-gid", result.getGid());

        // 验证 Mapper 和 Redis 调用
        verify(shortLinkMapper, times(1)).insert(any(ShortLinkDO.class));
        verify(shortLinkGotoMapper, times(1)).insert(any(ShortLinkGotoDO.class));
        verify(valueOperations).set(contains("s.test/"), eq("https://example.com/long-url"),
                anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(bloomFilter).add(contains("s.test/"));
    }

    @Test
    @DisplayName("DuplicateKeyException 且布隆过滤器未包含时补加 BNF")
    void duplicateKeyExceptionWithBloomFilterAdd() {
        ShortLinkCreateReqDTO req = ShortLinkCreateReqDTO.builder()
                .originUrl("https://example.com/test")
                .gid("test-gid")
                .createdType(0)
                .validDateType(0)
                .build();

        when(shortLinkMapper.insert(any(ShortLinkDO.class)))
                .thenThrow(new DuplicateKeyException("duplicate key"));
        when(bloomFilter.contains(anyString())).thenReturn(false);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> shortLinkService.createShortLink(req));
        assertTrue(ex.getMessage().contains("生成重复"));
        verify(bloomFilter).add(anyString());
    }
}
