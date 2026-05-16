package com.nym.shortlink.core.common.constant;

/**
 * 用户相关的常量类
 * 该类用于定义系统中与用户相关的常量字符串
 */
public class UserConstant {

    /**
     * 用户ID的键名常量
     * 用于在系统中唯一标识用户
     */
    public static final String USER_ID_KEY = "userId";

    /**
     * 用户名的键名常量
     * 用于存储用户登录时使用的用户名
     */
    public static final String USER_NAME_KEY = "username";

    /**
     * 真实姓名的键名常量
     * 用于存储用户的真实姓名信息
     */
    public static final String REAL_NAME_KEY = "realName";

    /**
     * 用户令牌的前缀常量
     * 用于生成Redis中存储用户令牌的键名
     */
    public static final String USER_TOKEN_KEY = "short-link:user:token:";

}
