package com.nym.shortlink.core.mq.consumer;

import com.nym.shortlink.core.common.convention.exception.ServiceException;
import com.nym.shortlink.core.dto.biz.ShortLinkStatsRecordDTO;
import com.nym.shortlink.core.mq.idempotent.MessageQueueIdempotentHandler;
import com.nym.shortlink.core.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import static com.nym.shortlink.core.common.constant.RedisKeyConstant.DELAY_QUEUE_STATS_KEY;

/**
 * 延迟记录短链接统计组件
 * 该组件已被标记为过时(@Deprecated)，用于消费延迟队列中的短链接统计记录
 */
@Deprecated
@Slf4j
@Component
@RequiredArgsConstructor
public class DelayShortLinkStatsConsumer implements InitializingBean {

    // Redisson客户端，用于操作Redis
    private final RedissonClient redissonClient;
    // 短链接服务，用于处理短链接相关操作
    private final ShortLinkService shortLinkService;
    // 消息队列幂等处理器，用于确保消息处理的唯一性和一致性
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;

    /**
     * 消费消息的方法
     * 创建一个单线程执行器，用于从延迟队列中获取并处理短链接统计记录
     */
    public void onMessage() {
        // 创建一个具有自定义线程工厂的单线程执行器
        // 线程名称设置为"delay_short-link_stats_consumer"，并设置为守护线程
        Executors.newSingleThreadExecutor(
                        runnable -> {
                            Thread thread = new Thread(runnable);
                            thread.setName("delay_short-link_stats_consumer");
                            thread.setDaemon(Boolean.TRUE);
                            return thread;
                        })
                .execute(() -> {
                    // 获取Redis中的阻塞双端队列
                    RBlockingDeque<ShortLinkStatsRecordDTO> blockingDeque = redissonClient.getBlockingDeque(DELAY_QUEUE_STATS_KEY);
                    // 将阻塞双端队列转换为延迟队列
                    RDelayedQueue<ShortLinkStatsRecordDTO> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
                    // 无限循环，持续消费队列中的消息
                    for (; ; ) {
                        try {
                            // 从延迟队列中获取一条统计记录
                            ShortLinkStatsRecordDTO statsRecord = delayedQueue.poll();
                            // 如果记录不为空，则进行处理
                            if (statsRecord != null) {
                                // 检查消息是否正在被消费
                                if (messageQueueIdempotentHandler.isMessageBeingConsumed(statsRecord.getKeys())) {
                                    // 判断当前的这个消息流程是否执行完成
                                    if (messageQueueIdempotentHandler.isAccomplish(statsRecord.getKeys())) {
                                        return;
                                    }
                                    throw new ServiceException("消息未完成流程，需要消息队列重试");
                                }
                                try {
                                    shortLinkService.shortLinkStats(statsRecord);
                                } catch (Throwable ex) {
                                    messageQueueIdempotentHandler.delMessageProcessed(statsRecord.getKeys());
                                    log.error("延迟记录短链接监控消费异常", ex);
                                }
                                messageQueueIdempotentHandler.setAccomplish(statsRecord.getKeys());
                                continue;
                            }
                            LockSupport.parkUntil(500);
                        } catch (Throwable ignored) {
                        }
                    }
                });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // onMessage();
    }
}
