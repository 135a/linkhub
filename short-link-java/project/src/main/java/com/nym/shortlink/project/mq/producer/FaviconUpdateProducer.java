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
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Favicon 更新消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FaviconUpdateProducer {

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.producer.favicon-topic:short-link_project-service_favicon-update_topic}")
    private String faviconTopic;

    /**
     * 发送 Favicon 更新消息
     *
     * @param fullShortUrl 完整短链接
     * @param originUrl    原始链接
     */
    public void send(String fullShortUrl, String originUrl) {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("fullShortUrl", fullShortUrl);
        producerMap.put("originUrl", originUrl);
        Message<Map<String, String>> message = MessageBuilder
                .withPayload(producerMap)
                .build();
        rocketMQTemplate.send(faviconTopic, message);
    }
}
