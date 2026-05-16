package com.nym.shortlink.core.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nym.shortlink.core.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 访问日志监控实体
 * 用于记录短链接的访问日志信息
 */
@Data
@TableName("t_link_access_logs")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkAccessLogsDO extends BaseDO {

    /**
     * id
     * 主键ID，唯一标识一条访问日志记录
     */
    private Long id;

    /**
     * 完整短链接
     * 被访问的短链接完整URL
     */
    private String fullShortUrl;

    /**
     * 用户信息
     * 访问用户的标识信息
     */
    private String user;

    /**
     * 浏览器
     * 访问时使用的浏览器类型和版本信息
     */
    private String browser;

    /**
     * 操作系统
     * 访问时使用的操作系统信息
     */
    private String os;

    /**
     * ip
     * 访问者的IP地址
     */
    private String ip;

    /**
     * 访问网络
     * 访问者使用的网络环境信息
     */
    private String network;

    /**
     * 访问设备
     * 访问者使用的设备类型信息
     */
    private String device;

    /**
     * 地区
     * 访问者的地理位置信息
     */
    private String locale;
}
