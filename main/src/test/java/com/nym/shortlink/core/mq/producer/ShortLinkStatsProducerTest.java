package com.nym.shortlink.core.mq.producer;

import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkStatsProducer - 统计消息生产者")
class ShortLinkStatsProducerTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private ShortLinkStatsSaveProducer producer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "statsSaveTopic", "test-topic");
    }

    @Test
    @DisplayName("异步发送统计消息")
    void sendStatsMessageAsynchronously() {
        Map<String, String> messageMap = new HashMap<>();
        messageMap.put("fullShortUrl", "s.test/abc123");
        messageMap.put("statsRecord", "{\"fullShortUrl\":\"s.test/abc123\"}");

        producer.send(messageMap);

        // verify asyncSend was called with the topic and a Message
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rocketMQTemplate).asyncSend(eq("test-topic"), messageCaptor.capture(),
                any(SendCallback.class), eq(2000L));
        assertNotNull(messageCaptor.getValue());
    }

    @Test
    @DisplayName("发送消息自动生成 keys")
    void sendAutoGeneratesKeys() {
        Map<String, String> messageMap = new HashMap<>();
        messageMap.put("fullShortUrl", "s.test/abc123");

        producer.send(messageMap);

        assertNotNull(messageMap.get("keys"));
        assertFalse(messageMap.get("keys").isEmpty());
    }
}
