package com.nym.shortlink.core.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nym.shortlink.core.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 操作系统统计访问实体
 * 该类用于存储短链接按操作系统维度的统计数据
 */
@Data
@TableName("t_link_os_stats")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkOsStatsDO extends BaseDO {

    /**
     * id
     * 数据库主键ID
     */
    private Long id;

    /**
     * 完整短链接
     * 完整的短链接URL，用于关联具体的短链接记录
     */
    private String fullShortUrl;

    /**
     * 日期
     * 统计数据对应的日期，格式为YYYY-MM-DD
     */
    private Date date;

    /**
     * 访问量
     * 该操作系统在特定日期下的访问次数统计
     */
    private Integer cnt;

    /**
     * 操作系统
     * 访问来源的操作系统名称，如Windows、macOS、Linux等
     */
    private String os;
}
