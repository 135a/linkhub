package com.nym.shortlink.core.mq.idempotent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageQueueIdempotent - 消息幂等处理")
class MessageQueueIdempotentTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MessageQueueIdempotentHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("消息首次消费：isMessageBeingConsumed 返回 false")
    void firstTimeConsumerReturnsFalse() {
        when(valueOperations.setIfAbsent(anyString(), eq("0"), eq(2L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);

        assertFalse(handler.isMessageBeingConsumed("msg-001"));
    }

    @Test
    @DisplayName("消息重复消费：isMessageBeingConsumed 返回 true")
    void duplicateMessageReturnsTrue() {
        when(valueOperations.setIfAbsent(anyString(), eq("0"), eq(2L), eq(TimeUnit.MINUTES)))
                .thenReturn(false);

        assertTrue(handler.isMessageBeingConsumed("msg-001"));
    }

    @Test
    @DisplayName("消息处理完成：isAccomplish 返回 true")
    void accomplishedReturnsTrue() {
        when(valueOperations.get(anyString())).thenReturn("1");

        assertTrue(handler.isAccomplish("msg-001"));
    }

    @Test
    @DisplayName("消息未处理完成：isAccomplish 返回 false")
    void notAccomplishedReturnsFalse() {
        when(valueOperations.get(anyString())).thenReturn("0");

        assertFalse(handler.isAccomplish("msg-001"));
    }

    @Test
    @DisplayName("设置消息处理完成")
    void setAccomplishUpdatesStatus() {
        handler.setAccomplish("msg-001");

        verify(valueOperations).set(contains("msg-001"), eq("1"), eq(2L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("异常时删除幂等标记使消息可重新消费")
    void delMessageProcessedOnException() {
        handler.delMessageProcessed("msg-001");

        verify(stringRedisTemplate).delete(contains("msg-001"));
    }
}
