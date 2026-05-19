package com.nym.shortlink.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nym.shortlink.core.common.convention.exception.ClientException;
import com.nym.shortlink.core.dao.entity.ShortLinkDO;
import com.nym.shortlink.core.dao.mapper.ShortLinkGotoMapper;
import com.nym.shortlink.core.dao.mapper.ShortLinkMapper;
import com.nym.shortlink.core.dto.req.ShortLinkPageReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkPageRespDTO;
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
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkService - 分页查询")
class ShortLinkServicePageTest {

    @Mock
    private ShortLinkMapper shortLinkMapper;
    @Mock
    private ShortLinkGotoMapper shortLinkGotoMapper;
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

    @InjectMocks
    private ShortLinkServiceImpl shortLinkService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shortLinkService, "baseMapper", shortLinkMapper);
    }

    @Test
    @DisplayName("gid 为空时抛出 ClientException")
    void emptyGidThrowsException() {
        ShortLinkPageReqDTO req = new ShortLinkPageReqDTO();
        req.setGid("");

        ClientException ex = assertThrows(ClientException.class,
                () -> shortLinkService.pageShortLink(req));
        assertTrue(ex.getMessage().contains("分组标识不能为空"));
    }

    @Test
    @DisplayName("正常分页查询返回 domain 拼接 http:// 前缀")
    void normalPageQueryPrependsHttpPrefix() {
        ShortLinkPageReqDTO req = new ShortLinkPageReqDTO();
        req.setGid("test-gid");
        req.setCurrent(1);
        req.setSize(10);

        ShortLinkDO mockEntity = ShortLinkDO.builder()
                .domain("s.test/")
                .shortUri("abc123")
                .fullShortUrl("s.test/abc123")
                .originUrl("https://example.com")
                .gid("test-gid")
                .build();

        IPage<ShortLinkDO> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(mockEntity));

        when(shortLinkMapper.pageLink(req)).thenReturn(mockPage);
        when(shortLinkMapper.pageLinkCount(req.getGid())).thenReturn(1L);

        IPage<ShortLinkPageRespDTO> result = shortLinkService.pageShortLink(req);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertTrue(result.getRecords().get(0).getDomain().startsWith("http://"));
    }
}
