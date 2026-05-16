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

    private final RocketMQTemplate rocketMQTemplate; // RocketMQ模板对象，用于发送消息

    @Value("${rocketmq.producer.topic}")
    private String statsSaveTopic; // 消息队列主题名称，从配置文件中注入

    /**
     * 异步发送短链接统计消息，主线程立即返回，不阻塞跳转响应
     * @param producerMap 包含统计信息的Map，将被发送到消息队列
     */
    public void send(Map<String, String> producerMap) {
        String keys = UUID.randomUUID().toString(); // 生成唯一的消息Key，用于消息追踪
        producerMap.put("keys", keys); // 将生成的Key添加到消息体中
        Message<Map<String, String>> build = MessageBuilder
                .withPayload(producerMap) // 设置消息体
                .setHeader(MessageConst.PROPERTY_KEYS, keys) // 设置消息头中的Key属性
                .build(); // 构建消息对象
        rocketMQTemplate.asyncSend(statsSaveTopic, build, new SendCallback() { // 异步发送消息，不阻塞主线程
            @Override
            public void onSuccess(SendResult sendResult) {
                // 消息发送成功的回调处理
                log.debug("[消息访问统计监控] 消息发送成功，消息ID：{}，Keys：{}", sendResult.getMsgId(), keys);
            }

            @Override
            public void onException(Throwable ex) {
                // 消息发送失败的回调处理
                log.error("[消息访问统计监控] 消息发送失败，消息体：{}", JSON.toJSONString(producerMap), ex);
            }
        }, 2000L); // 设置发送超时时间为2000毫秒
    }
}
