package com.nym.shortlink.core.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nym.shortlink.core.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 短链接今日统计实体
 * 该类用于存储短链接每日的统计数据，包括PV、UV和IP数等信息
 */
@TableName("t_link_stats_today")  // MyBatis-Plus注解，指定对应的数据库表名
@Data                          // Lombok注解，自动生成getter、setter等方法
@Builder                       // Lombok注解，提供Builder模式构建对象
@NoArgsConstructor              // Lombok注解，生成无参构造方法
@AllArgsConstructor             // Lombok注解，生成包含所有参数的构造方法
public class LinkStatsTodayDO extends BaseDO {  // 继承BaseDO，可能包含创建时间、更新时间等通用字段

    /**
     * id
     */
    private Long id;

    /**
     * 短链接
     */
    private String fullShortUrl;

    /**
     * 日期
     */
    private Date date;

    /**
     * 今日pv
     */
    private Integer todayPv;

    /**
     * 今日uv
     */
    private Integer todayUv;

    /**
     * 今日ip数
     */
    private Integer todayUip;
}
