package com.nym.shortlink.core.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nym.shortlink.core.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 地区统计访问实体
 * 该类用于存储短链接的地区访问统计数据，包含地理位置和访问量等信息
 */
@Data                // 使用Lombok的@Data注解自动生成getter、setter等方法
@TableName("t_link_locale_stats")  // 指定对应的数据库表名为t_link_locale_stats
@Builder            // 使用Lombok的@Builder注解提供Builder模式的构建器
@NoArgsConstructor  // 使用Lombok的@NoArgsConstructor注解生成无参构造方法
@AllArgsConstructor // 使用Lombok的@AllArgsConstructor注解生成全参构造方法
public class LinkLocaleStatsDO extends BaseDO {  // 继承BaseDO，可能包含一些公共字段如创建时间、更新时间等

    /**
     * id
     */
    private Long id;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 日期
     */
    private Date date;

    /**
     * 访问量
     */
    private Integer cnt;

    /**
     * 省份名称
     */
    private String province;

    /**
     * 市名称
     */
    private String city;

    /**
     * 城市编码
     */
    private String adcode;

    /**
     * 国家标识
     */
    private String country;
}
