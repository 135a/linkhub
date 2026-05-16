package com.nym.shortlink.core.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nym.shortlink.core.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 浏览器统计访问实体
 * 该类用于记录短链接被不同浏览器访问的统计数据
 */
@Data
@TableName("t_link_browser_stats")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkBrowserStatsDO extends BaseDO {

    /**
     * id
     * 数据库主键ID，唯一标识一条浏览器统计记录
     */
    private Long id;

    /**
     * 完整短链接
     * 完整的短链接URL，用于标识被访问的具体短链接
     */
    private String fullShortUrl;

    /**
     * 日期
     * 统计数据的日期，精确到天
     */
    private Date date;

    /**
     * 访问量
     * 该短链接在指定日期被特定浏览器访问的次数
     */
    private Integer cnt;

    /**
     * 浏览器
     * 访问短链接的浏览器类型，如Chrome、Firefox等
     */
    private String browser;
}
