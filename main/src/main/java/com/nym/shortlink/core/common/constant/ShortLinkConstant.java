package com.nym.shortlink.core.common.constant;

/**
 * 短链接常量类
 * 该类用于存储短链接服务中使用的常量值
 * 包括缓存有效时间和远程接口地址等配置信息
 */
public class ShortLinkConstant {

    /**
     * 永久短链接默认缓存有效时间，默认一个月
     * 单位：毫秒
     * 计算方式：30天 * 24小时 * 60分钟 * 60秒 * 1000毫秒 = 2626560000毫秒
     */
    public static final long DEFAULT_CACHE_VALID_TIME = 2626560000L;

    /**
     * 高德获取地区接口地址
     * 用于通过IP地址获取对应的地理位置信息
     * 接口文档参考：https://lbs.amap.com/api/webservice/guide/api/ipconfig
     */
    public static final String AMAP_REMOTE_URL = "https://restapi.amap.com/v3/ip";
}
