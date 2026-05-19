package com.nym.shortlink.core.toolkit;

import java.security.SecureRandom;

/**
 * 分组ID随机生成器
 * 该类用于生成指定长度的随机字符串，通常用作分组标识符
 */
public final class RandomGenerator {

    // 定义包含所有可能字符的字符串，包括数字和大写、小写字母
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    // 创建一个安全的随机数生成器实例
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成随机分组ID
     * 默认生成6位长度的随机字符串

     *
     * @return 分组ID 返回6位随机字符串
     */
    public static String generateRandom() {
        // 调用带参数的方法，指定长度为6
        return generateRandom(6);
    }

    /**
     * 生成随机分组ID
     * 根据指定长度生成随机字符串
     * @param length 生成多少位
     * @return 分组ID
     */
    public static String generateRandom(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
}
