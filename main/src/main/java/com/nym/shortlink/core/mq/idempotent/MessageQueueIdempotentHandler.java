package com.nym.shortlink.core.mq.idempotent;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 消息队列幂等处理器
 * 用于处理消息消费的幂等性问题，确保消息不会重复消费
 */
@Component
@RequiredArgsConstructor
public class MessageQueueIdempotentHandler {

    private final StringRedisTemplate stringRedisTemplate; // Redis操作模板，用于与Redis进行交互

    private static final String IDEMPOTENT_KEY_PREFIX = "short-link:idempotent:"; // 幂等性键的前缀，用于标识幂等性相关的键

    /**
     * 判断当前消息是否消费过
     * 通过Redis的setIfAbsent方法实现，如果键不存在则设置并返回true，表示消息未被消费过
     * 如果键已存在则返回false，表示消息已被消费过
     * 设置了2分钟的过期时间，防止内存泄漏
     *
     * @param messageId 消息唯一标识
     * @return 消息是否消费过，true表示已消费，false表示未消费
     */
    public boolean isMessageBeingConsumed(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId; // 构造完整的Redis键
        return Boolean.FALSE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "0", 2, TimeUnit.MINUTES));
    }

    /**
     * 判断消息消费流程是否执行完成
     * 通过检查Redis中存储的值是否为"1"来判断
     * "1"表示消息处理完成，"0"表示消息已被消费但处理未完成
     *
     * @param messageId 消息唯一标识
     * @return 消息是否执行完成，true表示已完成，false表示未完成
     */
    public boolean isAccomplish(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId; // 构造完整的Redis键
        return Objects.equals(stringRedisTemplate.opsForValue().get(key), "1");
    }

    /**
     * 设置消息流程执行完成
     * 将Redis中的值设置为"1"，表示消息处理完成
     * 同时设置了2分钟的过期时间，保持与初始设置的一致性
     *
     * @param messageId 消息唯一标识
     */
    public void setAccomplish(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId; // 构造完整的Redis键
        stringRedisTemplate.opsForValue().set(key, "1", 2, TimeUnit.MINUTES);
    }

    /**
     * 如果消息处理遇到异常情况，删除幂等标识
     * 删除Redis中的键，以便消息可以被重新消费
     * 这种情况通常发生在消息处理过程中出现异常，需要重试时
     *
     * @param messageId 消息唯一标识
     */
    public void delMessageProcessed(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId; // 构造完整的Redis键
        stringRedisTemplate.delete(key);
    }
}
