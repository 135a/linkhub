package com.nym.shortlink.core.toolkit;

import cn.hutool.core.lang.hash.MurmurHash;

/**
 * HASH 工具类
 * 提供将字符串转换为Base62编码的功能
 */
public class HashUtil {

    // 定义Base62编码的字符集，包括数字(0-9)、大写字母(A-Z)和小写字母(a-z)
    private static final char[] CHARS = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  // 数字部分
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',  // 大写字母部分
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'  // 小写字母部分
    };
    // 定义字符集的大小，即62
    private static final int SIZE = CHARS.length;

    /**
     * 将十进制数转换为Base62编码
     * @param num 要转换的十进制数
     * @return Base62编码字符串
     */
    private static String convertDecToBase62(long num) {
        StringBuilder sb = new StringBuilder();
        // 使用辗转相除法将十进制数转换为62进制
        while (num > 0) {
            int i = (int) (num % SIZE);  // 计算余数
            sb.append(CHARS[i]);  // 将余数对应的字符添加到结果中
            num /= SIZE;  // 除以62继续计算
        }
        return sb.reverse().toString();  // 反转字符串得到正确顺序
    }

    /**
     * 将字符串转换为Base62编码
     * @param str 要转换的字符串
     * @return Base62编码字符串
     */
    public static String hashToBase62(String str) {
        // 使用MurmurHash算法计算字符串的32位哈希值
        int i = MurmurHash.hash32(str);
        // 处理负数情况，将负数转换为正数
        long num = i < 0 ? Integer.MAX_VALUE - (long) i : i;
        // 将转换后的正数转换为Base62编码
        return convertDecToBase62(num);
    }
}
