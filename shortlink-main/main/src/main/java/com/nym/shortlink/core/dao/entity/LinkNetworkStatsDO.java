package com.nym.shortlink.core.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nym.shortlink.core.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 访问网络统计访问实体
 * 该类用于存储短链接访问的网络统计数据
 */
@Data // 使用Lombok的@Data注解，自动生成getter、setter等方法
@TableName("t_link_network_stats") // MyBatis-Plus的表名注解，指定对应的数据库表
@Builder // 使用Lombok的@Builder注解，支持Builder模式创建对象
@NoArgsConstructor // 使用Lombok的无参构造注解，自动生成无参构造方法
@AllArgsConstructor // 使用Lombok的全参构造注解，自动生成包含所有参数的构造方法
public class LinkNetworkStatsDO extends BaseDO {

    /**
     * id
     * 主键ID，用于唯一标识一条记录
     */
    private Long id;

    /**
     * 完整短链接
     * 完整的短链接URL，用于关联到具体的短链接
     */
    private String fullShortUrl;

    /**
     * 日期
     * 统计数据的日期，用于按天统计访问情况
     */
    private Date date;

    /**
     * 访问量
     * 该短链接在特定日期通过特定网络访问的次数
     */
    private Integer cnt;

    /**
     * 访问网络
     * 访问短链接的网络类型，如WiFi、4G、5G等
     */
    private String network;
}
