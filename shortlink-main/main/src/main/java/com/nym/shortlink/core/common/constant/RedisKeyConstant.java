package com.nym.shortlink.core.common.constant;

/**
 * Redis Key 常量类
 * 该类用于定义系统中所有使用Redis作为存储的键值常量
 * 每个常量代表一个在Redis中使用的键，用于不同的业务场景
 */
public class RedisKeyConstant {

    /**
     * 短链接跳转前缀 Key
     * 用于存储短链接对应的原始长链接，实现短链接到长链接的映射
     * 格式：short-link:goto:{短链接码}
     */
    public static final String GOTO_SHORT_LINK_KEY = "short-link:goto:%s";

    /**
     * 短链接空值跳转前缀 Key
     * 用于标记短链接对应的原始链接为空的情况，防止无效跳转
     * 格式：short-link:is-null:goto_{短链接码}
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link:is-null:goto_%s";

    /**
     * 短链接跳转锁前缀 Key
     * 用于在短链接跳转过程中加锁，防止并发问题
     * 格式：short-link:lock:goto:{短链接码}
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link:lock:goto:%s";

    /**
     * 短链接修改分组 ID 锁前缀 Key
     * 用于在修改短链接所属分组时加锁，确保数据一致性
     * 格式：short-link:lock:update-gid:{短链接码}
     */
    public static final String LOCK_GID_UPDATE_KEY = "short-link:lock:update-gid:%s";

    /**
     * 短链接延迟队列消费统计 Key
     * 用于记录延迟队列的消费情况，便于监控和调试
     * 格式：short-link:delay-queue:stats
     */
    public static final String DELAY_QUEUE_STATS_KEY = "short-link:delay-queue:stats";

    /**
     * 短链接统计判断是否新用户缓存标识
     * 用于记录用户访问情况，区分新老用户
     * 格式：short-link:stats:uv:{用户标识}
     */
    public static final String SHORT_LINK_STATS_UV_KEY = "short-link:stats:uv:";

    /**
     * 短链接统计判断是否新 IP 缓存标识
     * 用于记录IP访问情况，区分不同IP的访问
     * 格式：short-link:stats:uip:{IP地址}
     */
    public static final String SHORT_LINK_STATS_UIP_KEY = "short-link:stats:uip:";

    /**
     * 短链接监控消息保存队列 Topic 缓存标识
     * 用于定义消息队列的Topic，实现监控消息的传递
     * 格式：short-link:stats-stream
     */
    public static final String SHORT_LINK_STATS_STREAM_TOPIC_KEY = "short-link:stats-stream";

    /**
     * 短链接监控消息保存队列 Group 缓存标识
     * 用于定义消息队列的消费者组，实现消息的分组消费
     * 格式：short-link:stats-stream:only-group
     */
    public static final String SHORT_LINK_STATS_STREAM_GROUP_KEY = "short-link:stats-stream:only-group";

    /**
     * 创建短链接锁标识
     * 用于在创建短链接时加锁，防止重复创建
     * 格式：short-link:lock:create
     */
    public static final String SHORT_LINK_CREATE_LOCK_KEY = "short-link:lock:create";
}
