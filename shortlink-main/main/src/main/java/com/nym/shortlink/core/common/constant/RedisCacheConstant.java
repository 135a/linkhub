package com.nym.shortlink.core.common.constant;

/**
 * 短链接后管 Redis 缓存常量类
 * 该类用于定义系统中所有使用 Redis 缓存的键常量，
 * 包括分布式锁和用户登录相关的缓存键名。
 */
public class RedisCacheConstant {

    /**
     * 用户注册分布式锁
     * 用于在用户注册过程中加锁，防止并发注册问题
     * 键名格式: short-link:lock_user-register:[用户标识]
     */
    public static final String LOCK_USER_REGISTER_KEY = "short-link:lock_user-register:";

    /**
     * 分组创建分布式锁
     * 用于在创建分组过程中加锁，防止并发创建问题
     * 键名格式: short-link:lock_group-create:[分组ID]
     * %s 作为占位符，使用时需替换为实际的分组ID
     */
    public static final String LOCK_GROUP_CREATE_KEY = "short-link:lock_group-create:%s";

    /**
     * 用户登录缓存标识
     * 用于存储用户登录状态信息
     * 键名格式: short-link:login:[用户标识]
     */
    public static final String USER_LOGIN_KEY = "short-link:login:";
}
