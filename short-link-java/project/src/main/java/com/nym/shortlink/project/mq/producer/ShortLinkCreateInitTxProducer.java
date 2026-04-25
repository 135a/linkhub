/*
 * Copyright © 2026 NageOffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nym.shortlink.project.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 短链接创建初始化事务消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkCreateInitTxProducer {

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.producer.tx-init-topic:short-link_project-service_tx-init_topic}")
    private String txTopic;

    /**
     * 发送短链接创建初始化事务消息
     *
     * @param fullShortUrl 完整短链接
     * @param gid          分组标识
     * @return 发送结果
     */
    public SendResult sendCreateInitTransaction(String fullShortUrl, String gid) {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("fullShortUrl", fullShortUrl);
        producerMap.put("gid", gid);
        Message<Map<String, String>> message = MessageBuilder
                .withPayload(producerMap)
                .build();
        return rocketMQTemplate.sendMessageInTransaction(txTopic, message, null);
    }
}
