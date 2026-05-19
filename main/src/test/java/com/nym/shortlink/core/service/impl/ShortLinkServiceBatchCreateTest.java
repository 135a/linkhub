package com.nym.shortlink.core.service.impl;

import com.nym.shortlink.core.dao.entity.ShortLinkDO;
import com.nym.shortlink.core.dao.entity.ShortLinkGotoDO;
import com.nym.shortlink.core.dao.mapper.ShortLinkGotoMapper;
import com.nym.shortlink.core.dao.mapper.ShortLinkMapper;
import com.nym.shortlink.core.dto.req.ShortLinkBatchCreateReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkBatchCreateRespDTO;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkService - 批量创建短链接")
class ShortLinkServiceBatchCreateTest {

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
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(shortLinkService, "createShortLinkDefaultDomain", "s.test/");
        ReflectionTestUtils.setField(shortLinkService, "baseMapper", shortLinkMapper);
    }

    @Test
    @DisplayName("批量创建全部成功")
    void batchCreateAllSuccess() {
        ShortLinkBatchCreateReqDTO req = new ShortLinkBatchCreateReqDTO();
        req.setOriginUrls(Arrays.asList("https://a.com", "https://b.com", "https://c.com"));
        req.setDescribes(Arrays.asList("desc a", "desc b", "desc c"));
        req.setGid("batch-gid");
        req.setCreatedType(0);
        req.setValidDateType(0);

        when(shortLinkMapper.insert(any(ShortLinkDO.class))).thenReturn(1);
        when(shortLinkGotoMapper.insert(any(ShortLinkGotoDO.class))).thenReturn(1);

        ShortLinkBatchCreateRespDTO result = shortLinkService.batchCreateShortLink(req);

        assertEquals(3, result.getTotal());
        assertEquals(3, result.getBaseLinkInfos().size());
    }

    @Test
    @DisplayName("批量创建部分失败其余成功")
    void batchCreatePartialFailure() {
        ShortLinkBatchCreateReqDTO req = new ShortLinkBatchCreateReqDTO();
        req.setOriginUrls(Arrays.asList("https://a.com", "https://b.com", "https://c.com"));
        req.setDescribes(Arrays.asList("desc a", "desc b", "desc c"));
        req.setGid("batch-gid");
        req.setCreatedType(0);
        req.setValidDateType(0);

        // 第一条成功，第二条抛异常，第三条成功
        when(shortLinkMapper.insert(any(ShortLinkDO.class)))
                .thenReturn(1)
                .thenThrow(new DuplicateKeyException("duplicate"))
                .thenReturn(1);
        when(shortLinkGotoMapper.insert(any(ShortLinkGotoDO.class)))
                .thenReturn(1)
                .thenReturn(1);

        ShortLinkBatchCreateRespDTO result = shortLinkService.batchCreateShortLink(req);

        assertEquals(2, result.getTotal());
        assertEquals(2, result.getBaseLinkInfos().size());
    }
}
