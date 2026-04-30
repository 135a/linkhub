package com.nym.shortlink.core.mq.producer;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 短链接监控状态保存消息队列生产者
 * <p>
 * 使用 asyncSend 异步发送，主线程不阻塞等待 Broker ACK，
 * 跳转接口延迟不受 MQ 网络 RTT 影响，显著提升吞吐量。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsSaveProducer {

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.producer.topic}")
    private String statsSaveTopic;

    /**
     * 异步发送短链接统计消息，主线程立即返回，不阻塞跳转响应
     */
    public void send(Map<String, String> producerMap) {
        String keys = UUID.randomUUID().toString();
        producerMap.put("keys", keys);
        Message<Map<String, String>> build = MessageBuilder
                .withPayload(producerMap)
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .build();
        rocketMQTemplate.asyncSend(statsSaveTopic, build, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.debug("[消息访问统计监控] 消息发送成功，消息ID：{}，Keys：{}", sendResult.getMsgId(), keys);
            }

            @Override
            public void onException(Throwable ex) {
                log.error("[消息访问统计监控] 消息发送失败，消息体：{}", JSON.toJSONString(producerMap), ex);
            }
        }, 2000L);
    }
}
