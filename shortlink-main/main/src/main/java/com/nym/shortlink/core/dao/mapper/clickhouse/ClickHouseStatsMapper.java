package com.nym.shortlink.core.dao.mapper.clickhouse;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * ClickHouse 短链接统计 Mapper
 * 包含写入（8 张统计表 INSERT）和聚合查询方法
 */
@Mapper
public interface ClickHouseStatsMapper {

    // ===================== 写入方法 =====================

    /** 写入访问统计（PV/UV/UIP，按小时+星期聚合） */
    void insertAccessStats(@Param("fullShortUrl") String fullShortUrl,
                           @Param("date") String date,
                           @Param("pv") int pv,
                           @Param("uv") int uv,
                           @Param("uip") int uip,
                           @Param("hour") int hour,
                           @Param("weekday") int weekday);

    /** 写入地理位置统计 */
    void insertLocaleStats(@Param("fullShortUrl") String fullShortUrl,
                           @Param("date") String date,
                           @Param("cnt") int cnt,
                           @Param("province") String province,
                           @Param("city") String city,
                           @Param("adcode") String adcode,
                           @Param("country") String country);

    /** 写入操作系统统计 */
    void insertOsStats(@Param("fullShortUrl") String fullShortUrl,
                       @Param("date") String date,
                       @Param("cnt") int cnt,
                       @Param("os") String os);

    /** 写入浏览器统计 */
    void insertBrowserStats(@Param("fullShortUrl") String fullShortUrl,
                            @Param("date") String date,
                            @Param("cnt") int cnt,
                            @Param("browser") String browser);

    /** 写入设备统计 */
    void insertDeviceStats(@Param("fullShortUrl") String fullShortUrl,
                           @Param("date") String date,
                           @Param("cnt") int cnt,
                           @Param("device") String device);

    /** 写入网络类型统计 */
    void insertNetworkStats(@Param("fullShortUrl") String fullShortUrl,
                            @Param("date") String date,
                            @Param("cnt") int cnt,
                            @Param("network") String network);

    /** 写入访问日志明细 */
    void insertAccessLog(@Param("fullShortUrl") String fullShortUrl,
                         @Param("user") String user,
                         @Param("ip") String ip,
                         @Param("browser") String browser,
                         @Param("os") String os,
                         @Param("network") String network,
                         @Param("device") String device,
                         @Param("locale") String locale);

    /** 写入今日实时统计 */
    void insertStatsTodayStats(@Param("fullShortUrl") String fullShortUrl,
                               @Param("date") String date,
                               @Param("todayPv") int todayPv,
                               @Param("todayUv") int todayUv,
                               @Param("todayUip") int todayUip);

    // ===================== 查询方法 =====================

    /**
     * 查询单链接时间范围内的 PV/UV/UIP 汇总（按天分组）
     * 对应 oneShortLinkStats 的 daily 维度
     */
    List<Map<String, Object>> listDailyStatsByShortLink(
            @Param("fullShortUrl") String fullShortUrl,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 查询单链接时间范围内的总 PV/UV/UIP
     * 对应 oneShortLinkStats 的顶部汇总数据
     */
    Map<String, Object> sumPvUvUipByShortLink(
            @Param("fullShortUrl") String fullShortUrl,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 查询单链接地区分布（省级）
     */
    List<Map<String, Object>> listLocaleStatsByShortLink(
            @Param("fullShortUrl") String fullShortUrl,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 查询单链接访问日志明细（分页）
     * 返回总数，结合 offset/limit 实现分页
     */
    List<Map<String, Object>> listAccessLogsByShortLink(
            @Param("fullShortUrl") String fullShortUrl,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("offset") long offset,
            @Param("limit") long limit);

    /**
     * 查询单链接访问日志总数（用于分页计数）
     */
    long countAccessLogsByShortLink(
            @Param("fullShortUrl") String fullShortUrl,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);
}
