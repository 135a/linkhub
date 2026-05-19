package com.nym.shortlink.core.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.nym.shortlink.core.common.convention.exception.ServiceException;
import com.nym.shortlink.core.dao.mapper.*;
import com.nym.shortlink.core.dao.mapper.clickhouse.ClickHouseStatsMapper;
import com.nym.shortlink.core.dto.biz.ShortLinkStatsRecordDTO;
import com.nym.shortlink.core.mq.idempotent.MessageQueueIdempotentHandler;
import com.nym.shortlink.core.service.SseEmitterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkStatsConsumer - 统计消息消费者")
class ShortLinkStatsConsumerTest {

    @Mock
    private ShortLinkMapper shortLinkMapper;
    @Mock
    private ShortLinkGotoMapper shortLinkGotoMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RReadWriteLock readWriteLock;
    @Mock
    private RLock readLock;
    @Mock
    private MessageQueueIdempotentHandler idempotentHandler;
    @Mock
    private ClickHouseStatsMapper clickHouseStatsMapper;
    @Mock
    private SseEmitterService sseEmitterService;
    @Mock
    private LinkAccessStatsMapper linkAccessStatsMapper;
    @Mock
    private LinkLocaleStatsMapper linkLocaleStatsMapper;
    @Mock
    private LinkOsStatsMapper linkOsStatsMapper;
    @Mock
    private LinkBrowserStatsMapper linkBrowserStatsMapper;
    @Mock
    private LinkAccessLogsMapper linkAccessLogsMapper;
    @Mock
    private LinkDeviceStatsMapper linkDeviceStatsMapper;
    @Mock
    private LinkNetworkStatsMapper linkNetworkStatsMapper;
    @Mock
    private LinkStatsTodayMapper linkStatsTodayMapper;

    @InjectMocks
    private ShortLinkStatsSaveConsumer consumer;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        ReflectionTestUtils.setField(consumer, "statsPrimary", "mysql");
        ReflectionTestUtils.setField(consumer, "statsLocaleAmapKey", "test-api-key");
    }

    @Test
    @DisplayName("消息幂等：isMessageBeingConsumed=true 跳过处理")
    void duplicateMessageIsSkipped() {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("keys", "msg-001");

        // isMessageBeingConsumed 返回 true 表示消息已存在于 Redis（重复或已处理中）
        when(idempotentHandler.isMessageBeingConsumed("msg-001")).thenReturn(true);

        // 不应抛出异常
        assertDoesNotThrow(() -> consumer.onMessage(producerMap));
    }

    @Test
    @DisplayName("首次消费：isMessageBeingConsumed=false 触发重试异常")
    void firstTimeMessageThrowsForRetry() {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("keys", "msg-002");

        // 首次 SETNX 成功，返回 false
        when(idempotentHandler.isMessageBeingConsumed("msg-002")).thenReturn(false);
        // isAccomplish 检查是否已完成
        when(idempotentHandler.isAccomplish("msg-002")).thenReturn(false);

        // 首次消费会抛出 ServiceException 触发 RocketMQ 重试
        assertThrows(ServiceException.class, () -> consumer.onMessage(producerMap));
    }
}
