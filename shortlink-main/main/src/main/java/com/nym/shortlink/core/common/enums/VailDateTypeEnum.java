package com.nym.shortlink.core.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 有效期类型枚举类
 * 使用@RequiredArgsConstructor注解自动生成全参构造函数
 */
@RequiredArgsConstructor
public enum VailDateTypeEnum {

    /**
     * 永久有效期
     * 值为0，表示永久的有效期类型
     */
    PERMANENT(0),

    /**
     * 自定义有效期
     * 值为1，表示用户可以自定义有效期的类型
     */
    CUSTOM(1);

    /**
     * 有效期类型的值
     * 使用@Getter注解自动生成getter方法
     */
    @Getter
    private final int type;
}
