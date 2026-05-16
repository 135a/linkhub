package com.nym.shortlink.core.dto.biz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 短链接统计实体
 * 使用@Data注解自动生成getter、setter等方法
 * 使用@Builder支持Builder模式构建对象
 * 使用@NoArgsConstructor和@AllArgsConstructor提供无参和全参构造方法
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkStatsRecordDTO {

    /**
     * 完整短链接
     * 用于存储完整的短链接地址
     */
    private String fullShortUrl;

    /**
     * 访问用户IP
     * 记录访问用户的IP地址
     */
    private String remoteAddr;

    /**
     * 操作系统
     * 记录访问用户使用的操作系统信息
     */
    private String os;

    /**
     * 浏览器
     * 记录访问用户使用的浏览器信息
     */
    private String browser;

    /**
     * 操作设备
     */
    private String device;

    /**
     * 网络
     */
    private String network;

    /**
     * UV
     */
    private String uv;

    /**
     * UV访问标识
     */
    private Boolean uvFirstFlag;

    /**
     * UIP访问标识
     */
    private Boolean uipFirstFlag;

    /**
     * 消息队列唯一标识
     */
    private String keys;

    /**
     * 当前时间
     */
    private Date currentDate;
}
