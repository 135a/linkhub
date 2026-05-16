package com.nym.shortlink.core.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nym.shortlink.core.common.biz.user.UserContext;
import com.nym.shortlink.core.common.convention.exception.ServiceException;
import com.nym.shortlink.core.dao.entity.GroupDO;
import com.nym.shortlink.core.dao.entity.LinkAccessLogsDO;
import com.nym.shortlink.core.dao.entity.LinkAccessStatsDO;
import com.nym.shortlink.core.dao.entity.LinkDeviceStatsDO;
import com.nym.shortlink.core.dao.entity.LinkLocaleStatsDO;
import com.nym.shortlink.core.dao.entity.LinkNetworkStatsDO;
import com.nym.shortlink.core.dao.mapper.LinkGroupMapper;
import com.nym.shortlink.core.dao.mapper.LinkAccessLogsMapper;
import com.nym.shortlink.core.dao.mapper.LinkAccessStatsMapper;
import com.nym.shortlink.core.dao.mapper.LinkBrowserStatsMapper;
import com.nym.shortlink.core.dao.mapper.LinkDeviceStatsMapper;
import com.nym.shortlink.core.dao.mapper.LinkLocaleStatsMapper;
import com.nym.shortlink.core.dao.mapper.LinkNetworkStatsMapper;
import com.nym.shortlink.core.dao.mapper.LinkOsStatsMapper;
import com.nym.shortlink.core.dao.mapper.clickhouse.ClickHouseStatsMapper;
import com.nym.shortlink.core.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkGroupStatsReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkStatsReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsAccessDailyRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsBrowserRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsDeviceRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsLocaleCNRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsNetworkRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsOsRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsTopIpRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsUvRespDTO;
import com.nym.shortlink.core.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 短链接监控接口实现层
 * 提供短链接访问数据的统计功能，包括单个短链接和分组的访问统计
 * 支持多种维度的数据分析：按时间、地区、设备、浏览器等
 * 使用缓存机制提高查询性能，支持ClickHouse和MySQL双数据源
 */
@Service
@RequiredArgsConstructor
public class ShortLinkStatsServiceImpl implements ShortLinkStatsService {

    // 注入各种数据访问对象和缓存客户端
    private final LinkGroupMapper linkGroupMapper;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final ClickHouseStatsMapper clickHouseStatsMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    private final org.redisson.api.RedissonClient redissonClient;

    /** 统计查询主数据源开关： clickhouse 帐号时对时序聚合查询用 ClickHouse，其余复杂查询仍用 MySQL */
    @Value("${stats.storage.primary:clickhouse}")
    private String statsPrimary;

    /** 统计查询专用线程池：并行执行多维度 DB 查询，核心数 * 4 适配 IO 密集型场景 */
    private static final ExecutorService STATS_QUERY_EXECUTOR = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 4,
            Runtime.getRuntime().availableProcessors() * 8,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            new ThreadPoolExecutor.CallerRunsPolicy());

/**
 * 获取单个短链接的统计数据
 * @param requestParam 包含短链接、开始日期和结束日期的请求参数
 * @return 包含各种统计数据的响应DTO
 */
    @Override
    public ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {
    // 验证用户是否有权限访问该分组
        checkGroupBelongToUser(requestParam.getGid());

        // 性能优化：Redis 结果缓存，60s TTL，相同参数的查询直接返回缓存
        String resultCacheKey = "stats:single:" + requestParam.getFullShortUrl() + ":" + requestParam.getStartDate() + ":" + requestParam.getEndDate();
        String cachedResult = stringRedisTemplate.opsForValue().get(resultCacheKey);
        if (StrUtil.isNotBlank(cachedResult)) {
            return com.alibaba.fastjson2.JSON.parseObject(cachedResult, ShortLinkStatsRespDTO.class);
        }
        // 防缓存击穿：使用 tryLock 设置等待超时，避免无限等待导致 Tomcat 线程池耗尽
        org.redisson.api.RLock lock = redissonClient.getLock("lock:" + resultCacheKey);
        boolean locked = false;
        try {
        // 尝试获取锁，最多等待200ms，锁持有时间最长5000ms
            locked = lock.tryLock(200, 5000, TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new ServiceException("系统繁忙，统计数据拉取中，请稍后再试");
            }
            // 二次判断：拿到锁后再读一次，若其他线程已写入直接返回
            cachedResult = stringRedisTemplate.opsForValue().get(resultCacheKey);
            if (StrUtil.isNotBlank(cachedResult)) {
                return com.alibaba.fastjson2.JSON.parseObject(cachedResult, ShortLinkStatsRespDTO.class);
            }
        // 处理日期格式，去除多余的时间部分
        String startDate = requestParam.getStartDate();
        String endDate = requestParam.getEndDate();
        if (StrUtil.isNotBlank(startDate) && startDate.split(" ").length > 2) {
            startDate = startDate.split(" ")[0] + " " + startDate.split(" ")[1];
        }
        if (StrUtil.isNotBlank(endDate) && endDate.split(" ").length > 2) {
            endDate = endDate.split(" ")[0] + " " + endDate.split(" ")[1];
        }
        requestParam.setStartDate(startDate);
        requestParam.setEndDate(endDate);
        // 生成日期范围列表
        List<String> rangeDates = DateUtil.rangeToList(DateUtil.parse(startDate), DateUtil.parse(endDate), DateField.DAY_OF_MONTH)
                .stream().map(DateUtil::formatDate).toList();

        // 如果不是 ClickHouse 作为主要存储，则查询统计数据
        if (!"clickhouse".equalsIgnoreCase(statsPrimary)) {
            List<LinkAccessStatsDO> listStatsByShortLink = linkAccessStatsMapper.listStatsByShortLink(requestParam);
            if (CollUtil.isEmpty(listStatsByShortLink)) {
                return null;
            }
        }

        // 基础访问数据（PV/UV/UIP 汇总）- 串行先执行（后续并行查询依赖其结果判空）
        LinkAccessStatsDO pvUvUidStatsByShortLink = new LinkAccessStatsDO();
        if ("clickhouse".equalsIgnoreCase(statsPrimary)) {
            // 使用 ClickHouse 查询 PV/UV/UIP 数据
            Map<String, Object> sumMap = clickHouseStatsMapper.sumPvUvUipByShortLink(requestParam.getFullShortUrl(), startDate, endDate);
            if (sumMap == null || sumMap.get("pv") == null) {
                return null;
            }
            pvUvUidStatsByShortLink.setPv(Integer.parseInt(sumMap.get("pv").toString()));
            pvUvUidStatsByShortLink.setUv(Integer.parseInt(sumMap.get("uv").toString()));
            pvUvUidStatsByShortLink.setUip(Integer.parseInt(sumMap.get("uip").toString()));
        } else {
            // 使用 MySQL 查询 PV/UV/UIP 数据
            pvUvUidStatsByShortLink = linkAccessLogsMapper.findPvUvUidStatsByShortLink(requestParam);
            if (pvUvUidStatsByShortLink == null) {
                return null;
            }
        }

        // 按天 daily 数据统计
        List<ShortLinkStatsAccessDailyRespDTO> daily = new ArrayList<>();
        if ("clickhouse".equalsIgnoreCase(statsPrimary)) {
            // 使用 ClickHouse 按天统计数据
            List<Map<String, Object>> chDaily = clickHouseStatsMapper.listDailyStatsByShortLink(requestParam.getFullShortUrl(), startDate, endDate);
            Map<String, Map<String, Object>> chDailyMap = new HashMap<>();
            chDaily.forEach(row -> chDailyMap.put(row.get("date").toString(), row));
            rangeDates.forEach(each -> {
                Map<String, Object> row = chDailyMap.get(each);
                if (row != null) {
                    daily.add(ShortLinkStatsAccessDailyRespDTO.builder().date(each)
                            .pv(Integer.parseInt(row.getOrDefault("pv", 0).toString()))
                            .uv(Integer.parseInt(row.getOrDefault("uv", 0).toString()))
                            .uip(Integer.parseInt(row.getOrDefault("uip", 0).toString())).build());
                } else {
                    daily.add(ShortLinkStatsAccessDailyRespDTO.builder().date(each).pv(0).uv(0).uip(0).build());
                }
            });
        } else {
            // 使用 MySQL 按天统计数据
            List<LinkAccessStatsDO> listStatsByShortLink = linkAccessStatsMapper.listStatsByShortLink(requestParam);
            rangeDates.forEach(each -> listStatsByShortLink.stream()
                    .filter(item -> Objects.equals(each, DateUtil.formatDate(item.getDate()))).findFirst()
                    .ifPresentOrElse(
                            item -> daily.add(ShortLinkStatsAccessDailyRespDTO.builder().date(each).pv(item.getPv()).uv(item.getUv()).uip(item.getUip()).build()),
                            () -> daily.add(ShortLinkStatsAccessDailyRespDTO.builder().date(each).pv(0).uv(0).uip(0).build())));
        }

        // ===== 并行执行所有独立的多维度统计查询 =====
        // 创建最终请求参数的副本，用于并行查询
        final ShortLinkStatsReqDTO fp = requestParam;
        final String fs = startDate, fe = endDate;

        // 地区统计查询
        CompletableFuture<List<LinkLocaleStatsDO>> localeFuture = CompletableFuture.supplyAsync(() -> {
            if ("clickhouse".equalsIgnoreCase(statsPrimary)) {
                return clickHouseStatsMapper.listLocaleStatsByShortLink(fp.getFullShortUrl(), fs, fe).stream().map(row -> {
                    LinkLocaleStatsDO d = new LinkLocaleStatsDO();
                    d.setProvince(row.getOrDefault("province", "未知").toString());
                    d.setCnt(Integer.parseInt(row.getOrDefault("cnt", 0).toString()));
                    return d;
                }).toList();
            }
            return linkLocaleStatsMapper.listLocaleByShortLink(fp);
        }, STATS_QUERY_EXECUTOR);

        // 小时统计查询
        CompletableFuture<List<LinkAccessStatsDO>> hourFuture = CompletableFuture.supplyAsync(
                () -> linkAccessStatsMapper.listHourStatsByShortLink(fp), STATS_QUERY_EXECUTOR);
        // Top IP统计查询
        CompletableFuture<List<HashMap<String, Object>>> topIpFuture = CompletableFuture.supplyAsync(
                () -> linkAccessLogsMapper.listTopIpByShortLink(fp), STATS_QUERY_EXECUTOR);
        // 星期统计查询
        CompletableFuture<List<LinkAccessStatsDO>> weekdayFuture = CompletableFuture.supplyAsync(
                () -> linkAccessStatsMapper.listWeekdayStatsByShortLink(fp), STATS_QUERY_EXECUTOR);
        // 浏览器统计查询
        CompletableFuture<List<HashMap<String, Object>>> browserFuture = CompletableFuture.supplyAsync(
                () -> linkBrowserStatsMapper.listBrowserStatsByShortLink(fp), STATS_QUERY_EXECUTOR);
        // 操作系统统计查询
        CompletableFuture<List<HashMap<String, Object>>> osFuture = CompletableFuture.supplyAsync(
                () -> linkOsStatsMapper.listOsStatsByShortLink(fp), STATS_QUERY_EXECUTOR);
        // UV类型统计查询
        CompletableFuture<HashMap<String, Object>> uvTypeFuture = CompletableFuture.supplyAsync(
                () -> linkAccessLogsMapper.findUvTypeCntByShortLink(fp), STATS_QUERY_EXECUTOR);
        // 设备统计查询
        CompletableFuture<List<LinkDeviceStatsDO>> deviceFuture = CompletableFuture.supplyAsync(
                () -> linkDeviceStatsMapper.listDeviceStatsByShortLink(fp), STATS_QUERY_EXECUTOR);
        // 网络统计查询
        CompletableFuture<List<LinkNetworkStatsDO>> networkFuture = CompletableFuture.supplyAsync(
                () -> linkNetworkStatsMapper.listNetworkStatsByShortLink(fp), STATS_QUERY_EXECUTOR);

        // 等待所有并行查询完成
        CompletableFuture.allOf(localeFuture, hourFuture, topIpFuture, weekdayFuture,
                browserFuture, osFuture, uvTypeFuture, deviceFuture, networkFuture).join();

        // 地区
        List<ShortLinkStatsLocaleCNRespDTO> localeCnStats = new ArrayList<>();
        List<LinkLocaleStatsDO> localeList = localeFuture.join();
        int localeCnSum = localeList.stream().mapToInt(LinkLocaleStatsDO::getCnt).sum();
        localeList.forEach(each -> localeCnStats.add(ShortLinkStatsLocaleCNRespDTO.builder()
                .cnt(each.getCnt()).locale(each.getProvince())
                .ratio(localeCnSum == 0 ? 0 : Math.round((double) each.getCnt() / localeCnSum * 100.0) / 100.0).build()));

        // 小时
        List<Integer> hourStats = new ArrayList<>();
        List<LinkAccessStatsDO> hourList = hourFuture.join();
        for (int i = 0; i < 24; i++) {
            final int h = i;
            hourStats.add(hourList.stream().filter(e -> Objects.equals(e.getHour(), h)).findFirst().map(LinkAccessStatsDO::getPv).orElse(0));
        }

        // Top IP
        List<ShortLinkStatsTopIpRespDTO> topIpStats = new ArrayList<>();
        topIpFuture.join().forEach(each -> topIpStats.add(ShortLinkStatsTopIpRespDTO.builder()
                .ip(each.get("ip").toString()).cnt(Integer.parseInt(each.get("count").toString())).build()));

        // 星期
        List<Integer> weekdayStats = new ArrayList<>();
        List<LinkAccessStatsDO> weekdayList = weekdayFuture.join();
        for (int i = 1; i < 8; i++) {
            final int w = i;
            weekdayStats.add(weekdayList.stream().filter(e -> Objects.equals(e.getWeekday(), w)).findFirst().map(LinkAccessStatsDO::getPv).orElse(0));
        }

        // 浏览器
        List<ShortLinkStatsBrowserRespDTO> browserStats = new ArrayList<>();
        List<HashMap<String, Object>> browserList = browserFuture.join();
        int browserSum = browserList.stream().mapToInt(e -> Integer.parseInt(e.get("count").toString())).sum();
        browserList.forEach(each -> browserStats.add(ShortLinkStatsBrowserRespDTO.builder()
                .cnt(Integer.parseInt(each.get("count").toString())).browser(each.get("browser").toString())
                .ratio(browserSum == 0 ? 0 : Math.round((double) Integer.parseInt(each.get("count").toString()) / browserSum * 100.0) / 100.0).build()));

        // OS
        List<ShortLinkStatsOsRespDTO> osStats = new ArrayList<>();
        List<HashMap<String, Object>> osList = osFuture.join();
        int osSum = osList.stream().mapToInt(e -> Integer.parseInt(e.get("count").toString())).sum();
        osList.forEach(each -> osStats.add(ShortLinkStatsOsRespDTO.builder()
                .cnt(Integer.parseInt(each.get("count").toString())).os(each.get("os").toString())
                .ratio(osSum == 0 ? 0 : Math.round((double) Integer.parseInt(each.get("count").toString()) / osSum * 100.0) / 100.0).build()));

        // UV 类型
        List<ShortLinkStatsUvRespDTO> uvTypeStats = new ArrayList<>();
        HashMap<String, Object> uvMap = uvTypeFuture.join();
        int oldUserCnt = Integer.parseInt(Optional.ofNullable(uvMap).map(e -> e.get("oldUserCnt")).map(Object::toString).orElse("0"));
        int newUserCnt = Integer.parseInt(Optional.ofNullable(uvMap).map(e -> e.get("newUserCnt")).map(Object::toString).orElse("0"));
        int uvSum = oldUserCnt + newUserCnt;
        uvTypeStats.add(ShortLinkStatsUvRespDTO.builder().uvType("newUser").cnt(newUserCnt)
                .ratio(uvSum == 0 ? 0 : Math.round((double) newUserCnt / uvSum * 100.0) / 100.0).build());
        uvTypeStats.add(ShortLinkStatsUvRespDTO.builder().uvType("oldUser").cnt(oldUserCnt)
                .ratio(uvSum == 0 ? 0 : Math.round((double) oldUserCnt / uvSum * 100.0) / 100.0).build());

        // 设备
        List<ShortLinkStatsDeviceRespDTO> deviceStats = new ArrayList<>();
        List<LinkDeviceStatsDO> deviceList = deviceFuture.join();
        int deviceSum = deviceList.stream().mapToInt(LinkDeviceStatsDO::getCnt).sum();
        deviceList.forEach(each -> deviceStats.add(ShortLinkStatsDeviceRespDTO.builder()
                .cnt(each.getCnt()).device(each.getDevice())
                .ratio(deviceSum == 0 ? 0 : Math.round((double) each.getCnt() / deviceSum * 100.0) / 100.0).build()));

        // 网络
        List<ShortLinkStatsNetworkRespDTO> networkStats = new ArrayList<>();
        List<LinkNetworkStatsDO> networkList = networkFuture.join();
        int networkSum = networkList.stream().mapToInt(LinkNetworkStatsDO::getCnt).sum();
        networkList.forEach(each -> networkStats.add(ShortLinkStatsNetworkRespDTO.builder()
                .cnt(each.getCnt()).network(each.getNetwork())
                .ratio(networkSum == 0 ? 0 : Math.round((double) each.getCnt() / networkSum * 100.0) / 100.0).build()));

        ShortLinkStatsRespDTO result = ShortLinkStatsRespDTO.builder()
                .pv(pvUvUidStatsByShortLink.getPv()).uv(pvUvUidStatsByShortLink.getUv()).uip(pvUvUidStatsByShortLink.getUip())
                .daily(daily).localeCnStats(localeCnStats).hourStats(hourStats).topIpStats(topIpStats)
                .weekdayStats(weekdayStats).browserStats(browserStats).osStats(osStats)
                .uvTypeStats(uvTypeStats).deviceStats(deviceStats).networkStats(networkStats).build();
            stringRedisTemplate.opsForValue().set(resultCacheKey, com.alibaba.fastjson2.JSON.toJSONString(result), 60, TimeUnit.SECONDS);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("系统繁忙，请稍后再试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public ShortLinkStatsRespDTO groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        checkGroupBelongToUser(requestParam.getGid());

        // 性能优化：Redis 结果缓存，60s TTL
        String groupResultCacheKey = "stats:group:" + requestParam.getGid() + ":" + requestParam.getStartDate() + ":" + requestParam.getEndDate();
        String cachedGroupResult = stringRedisTemplate.opsForValue().get(groupResultCacheKey);
        if (StrUtil.isNotBlank(cachedGroupResult)) {
            return com.alibaba.fastjson2.JSON.parseObject(cachedGroupResult, ShortLinkStatsRespDTO.class);
        }

        // 防缓存击穿：使用 tryLock 设置等待超时，避免无限等待导致 Tomcat 线程池耗尽
        org.redisson.api.RLock lock = redissonClient.getLock("lock:" + groupResultCacheKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(200, 5000, TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new ServiceException("系统繁忙，统计数据拉取中，请稍后再试");
            }
            // 二次判断：拿到锁后再读一次，若其他线程已写入直接返回
            cachedGroupResult = stringRedisTemplate.opsForValue().get(groupResultCacheKey);
            if (StrUtil.isNotBlank(cachedGroupResult)) {
                return com.alibaba.fastjson2.JSON.parseObject(cachedGroupResult, ShortLinkStatsRespDTO.class);
            }

            List<LinkAccessStatsDO> listStatsByGroup = linkAccessStatsMapper.listStatsByGroup(requestParam);
            if (CollUtil.isEmpty(listStatsByGroup)) {
                return null;
            }
            // 基础访问数据
            LinkAccessStatsDO pvUvUidStatsByGroup = linkAccessLogsMapper.findPvUvUidStatsByGroup(requestParam);
            // 基础访问详情
            List<ShortLinkStatsAccessDailyRespDTO> daily = new ArrayList<>();
            String startDate = requestParam.getStartDate();
            String endDate = requestParam.getEndDate();
            // 鲁棒性处理：防止前端传参出现重复时间后缀（如 "2026-04-21 00:00:00 00:00:00"）
            if (StrUtil.isNotBlank(startDate) && startDate.split(" ").length > 2) {
                startDate = startDate.split(" ")[0] + " " + startDate.split(" ")[1];
            }
            if (StrUtil.isNotBlank(endDate) && endDate.split(" ").length > 2) {
                endDate = endDate.split(" ")[0] + " " + endDate.split(" ")[1];
            }
            requestParam.setStartDate(startDate);
            requestParam.setEndDate(endDate);
            List<String> rangeDates = DateUtil.rangeToList(DateUtil.parse(startDate), DateUtil.parse(endDate), DateField.DAY_OF_MONTH).stream()
                    .map(DateUtil::formatDate)
                    .toList();
            rangeDates.forEach(each -> listStatsByGroup.stream()
                    .filter(item -> Objects.equals(each, DateUtil.formatDate(item.getDate())))
                    .findFirst()
                    .ifPresentOrElse(item -> {
                        ShortLinkStatsAccessDailyRespDTO accessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                                .date(each)
                                .pv(item.getPv())
                                .uv(item.getUv())
                                .uip(item.getUip())
                                .build();
                        daily.add(accessDailyRespDTO);
                    }, () -> {
                        ShortLinkStatsAccessDailyRespDTO accessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                                .date(each)
                                .pv(0)
                                .uv(0)
                                .uip(0)
                                .build();
                        daily.add(accessDailyRespDTO);
                    }));

            // ===== 并行执行所有独立的多维度统计查询 =====
            final ShortLinkGroupStatsReqDTO fp = requestParam;
            CompletableFuture<List<LinkLocaleStatsDO>> localeFuture = CompletableFuture.supplyAsync(
                    () -> linkLocaleStatsMapper.listLocaleByGroup(fp), STATS_QUERY_EXECUTOR);
            CompletableFuture<List<LinkAccessStatsDO>> hourFuture = CompletableFuture.supplyAsync(
                    () -> linkAccessStatsMapper.listHourStatsByGroup(fp), STATS_QUERY_EXECUTOR);
            CompletableFuture<List<HashMap<String, Object>>> topIpFuture = CompletableFuture.supplyAsync(
                    () -> linkAccessLogsMapper.listTopIpByGroup(fp), STATS_QUERY_EXECUTOR);
            CompletableFuture<List<LinkAccessStatsDO>> weekdayFuture = CompletableFuture.supplyAsync(
                    () -> linkAccessStatsMapper.listWeekdayStatsByGroup(fp), STATS_QUERY_EXECUTOR);
            CompletableFuture<List<HashMap<String, Object>>> browserFuture = CompletableFuture.supplyAsync(
                    () -> linkBrowserStatsMapper.listBrowserStatsByGroup(fp), STATS_QUERY_EXECUTOR);
            CompletableFuture<List<HashMap<String, Object>>> osFuture = CompletableFuture.supplyAsync(
                    () -> linkOsStatsMapper.listOsStatsByGroup(fp), STATS_QUERY_EXECUTOR);
            CompletableFuture<List<LinkDeviceStatsDO>> deviceFuture = CompletableFuture.supplyAsync(
                    () -> linkDeviceStatsMapper.listDeviceStatsByGroup(fp), STATS_QUERY_EXECUTOR);
            CompletableFuture<List<LinkNetworkStatsDO>> networkFuture = CompletableFuture.supplyAsync(
                    () -> linkNetworkStatsMapper.listNetworkStatsByGroup(fp), STATS_QUERY_EXECUTOR);

            CompletableFuture.allOf(localeFuture, hourFuture, topIpFuture, weekdayFuture,
                    browserFuture, osFuture, deviceFuture, networkFuture).join();

            // 地区访问详情（仅国内）
            List<ShortLinkStatsLocaleCNRespDTO> localeCnStats = new ArrayList<>();
            List<LinkLocaleStatsDO> listedLocaleByGroup = localeFuture.join();
            int localeCnSum = listedLocaleByGroup.stream()
                    .mapToInt(LinkLocaleStatsDO::getCnt)
                    .sum();
            listedLocaleByGroup.forEach(each -> {
                double ratio = (double) each.getCnt() / localeCnSum;
                double actualRatio = Math.round(ratio * 100.0) / 100.0;
                ShortLinkStatsLocaleCNRespDTO localeCNRespDTO = ShortLinkStatsLocaleCNRespDTO.builder()
                        .cnt(each.getCnt())
                        .locale(each.getProvince())
                        .ratio(actualRatio)
                        .build();
                localeCnStats.add(localeCNRespDTO);
            });
            // 小时访问详情
            List<Integer> hourStats = new ArrayList<>();
            List<LinkAccessStatsDO> listHourStatsByGroup = hourFuture.join();
            for (int i = 0; i < 24; i++) {
                java.util.concurrent.atomic.AtomicInteger hour = new java.util.concurrent.atomic.AtomicInteger(i);
                int hourCnt = listHourStatsByGroup.stream()
                        .filter(each -> Objects.equals(each.getHour(), hour.get()))
                        .findFirst()
                        .map(LinkAccessStatsDO::getPv)
                        .orElse(0);
                hourStats.add(hourCnt);
            }
            // 高频访问IP详情
            List<ShortLinkStatsTopIpRespDTO> topIpStats = new ArrayList<>();
            List<HashMap<String, Object>> listTopIpByGroup = topIpFuture.join();
            listTopIpByGroup.forEach(each -> {
                ShortLinkStatsTopIpRespDTO statsTopIpRespDTO = ShortLinkStatsTopIpRespDTO.builder()
                        .ip(each.get("ip").toString())
                        .cnt(Integer.parseInt(each.get("count").toString()))
                        .build();
                topIpStats.add(statsTopIpRespDTO);
            });
            // 一周访问详情
            List<Integer> weekdayStats = new ArrayList<>();
            List<LinkAccessStatsDO> listWeekdayStatsByGroup = weekdayFuture.join();
            for (int i = 1; i < 8; i++) {
                java.util.concurrent.atomic.AtomicInteger weekday = new java.util.concurrent.atomic.AtomicInteger(i);
                int weekdayCnt = listWeekdayStatsByGroup.stream()
                        .filter(each -> Objects.equals(each.getWeekday(), weekday.get()))
                        .findFirst()
                        .map(LinkAccessStatsDO::getPv)
                        .orElse(0);
                weekdayStats.add(weekdayCnt);
            }
            // 浏览器访问详情
            List<ShortLinkStatsBrowserRespDTO> browserStats = new ArrayList<>();
            List<HashMap<String, Object>> listBrowserStatsByGroup = browserFuture.join();
            int browserSum = listBrowserStatsByGroup.stream()
                    .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                    .sum();
            listBrowserStatsByGroup.forEach(each -> {
                double ratio = (double) Integer.parseInt(each.get("count").toString()) / browserSum;
                double actualRatio = Math.round(ratio * 100.0) / 100.0;
                ShortLinkStatsBrowserRespDTO browserRespDTO = ShortLinkStatsBrowserRespDTO.builder()
                        .cnt(Integer.parseInt(each.get("count").toString()))
                        .browser(each.get("browser").toString())
                        .ratio(actualRatio)
                        .build();
                browserStats.add(browserRespDTO);
            });
            // 操作系统访问详情
            List<ShortLinkStatsOsRespDTO> osStats = new ArrayList<>();
            List<HashMap<String, Object>> listOsStatsByGroup = osFuture.join();
            int osSum = listOsStatsByGroup.stream()
                    .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                    .sum();
            listOsStatsByGroup.forEach(each -> {
                double ratio = (double) Integer.parseInt(each.get("count").toString()) / osSum;
                double actualRatio = Math.round(ratio * 100.0) / 100.0;
                ShortLinkStatsOsRespDTO osRespDTO = ShortLinkStatsOsRespDTO.builder()
                        .cnt(Integer.parseInt(each.get("count").toString()))
                        .os(each.get("os").toString())
                        .ratio(actualRatio)
                        .build();
                osStats.add(osRespDTO);
            });
            // 访问设备类型详情
            List<ShortLinkStatsDeviceRespDTO> deviceStats = new ArrayList<>();
            List<LinkDeviceStatsDO> listDeviceStatsByGroup = deviceFuture.join();
            int deviceSum = listDeviceStatsByGroup.stream()
                    .mapToInt(LinkDeviceStatsDO::getCnt)
                    .sum();
            listDeviceStatsByGroup.forEach(each -> {
                double ratio = (double) each.getCnt() / deviceSum;
                double actualRatio = Math.round(ratio * 100.0) / 100.0;
                ShortLinkStatsDeviceRespDTO deviceRespDTO = ShortLinkStatsDeviceRespDTO.builder()
                        .cnt(each.getCnt())
                        .device(each.getDevice())
                        .ratio(actualRatio)
                        .build();
                deviceStats.add(deviceRespDTO);
            });
            // 访问网络类型详情
            List<ShortLinkStatsNetworkRespDTO> networkStats = new ArrayList<>();
            List<LinkNetworkStatsDO> listNetworkStatsByGroup = networkFuture.join();
            int networkSum = listNetworkStatsByGroup.stream()
                    .mapToInt(LinkNetworkStatsDO::getCnt)
                    .sum();
            listNetworkStatsByGroup.forEach(each -> {
                double ratio = (double) each.getCnt() / networkSum;
                double actualRatio = Math.round(ratio * 100.0) / 100.0;
                ShortLinkStatsNetworkRespDTO networkRespDTO = ShortLinkStatsNetworkRespDTO.builder()
                        .cnt(each.getCnt())
                        .network(each.getNetwork())
                        .ratio(actualRatio)
                        .build();
                networkStats.add(networkRespDTO);
            });
            ShortLinkStatsRespDTO groupResult = ShortLinkStatsRespDTO.builder()
                    .pv(pvUvUidStatsByGroup.getPv())
                    .uv(pvUvUidStatsByGroup.getUv())
                    .uip(pvUvUidStatsByGroup.getUip())
                    .daily(daily)
                    .localeCnStats(localeCnStats)
                    .hourStats(hourStats)
                    .topIpStats(topIpStats)
                    .weekdayStats(weekdayStats)
                    .browserStats(browserStats)
                    .osStats(osStats)
                    .deviceStats(deviceStats)
                    .networkStats(networkStats)
                    .build();
            stringRedisTemplate.opsForValue().set(groupResultCacheKey, com.alibaba.fastjson2.JSON.toJSONString(groupResult), 60, TimeUnit.SECONDS);
            return groupResult;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("系统繁忙，请稍后再试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        checkGroupBelongToUser(requestParam.getGid());
        String startDate = requestParam.getStartDate();
        String endDate = requestParam.getEndDate();
        if (StrUtil.isNotBlank(startDate) && startDate.split(" ").length > 2) {
            startDate = startDate.split(" ")[0] + " " + startDate.split(" ")[1];
        }
        if (StrUtil.isNotBlank(endDate) && endDate.split(" ").length > 2) {
            endDate = endDate.split(" ")[0] + " " + endDate.split(" ")[1];
        }
        LambdaQueryWrapper<LinkAccessLogsDO> queryWrapper = Wrappers.lambdaQuery(LinkAccessLogsDO.class)
                .eq(LinkAccessLogsDO::getFullShortUrl, requestParam.getFullShortUrl())
                .between(LinkAccessLogsDO::getCreateTime, startDate, endDate)
                .eq(LinkAccessLogsDO::getDelFlag, 0)
                .orderByDesc(LinkAccessLogsDO::getCreateTime);
        IPage<LinkAccessLogsDO> linkAccessLogsDOIPage = linkAccessLogsMapper.selectPage(requestParam, queryWrapper);
        if (CollUtil.isEmpty(linkAccessLogsDOIPage.getRecords())) {
            return new Page<>();
        }
        IPage<ShortLinkStatsAccessRecordRespDTO> actualResult = linkAccessLogsDOIPage.convert(each -> BeanUtil.toBean(each, ShortLinkStatsAccessRecordRespDTO.class));
        List<String> userAccessLogsList = actualResult.getRecords().stream()
                .map(ShortLinkStatsAccessRecordRespDTO::getUser)
                .toList();
        List<Map<String, Object>> uvTypeList = linkAccessLogsMapper.selectUvTypeByUsers(
                requestParam.getGid(),
                requestParam.getFullShortUrl(),
                requestParam.getEnableStatus(),
                requestParam.getStartDate(),
                requestParam.getEndDate(),
                userAccessLogsList
        );
        actualResult.getRecords().forEach(each -> {
            String uvType = uvTypeList.stream()
                    .filter(item -> Objects.equals(each.getUser(), item.get("user")))
                    .findFirst()
                    .map(item -> item.get("uvType"))
                    .map(Object::toString)
                    .orElse("旧访客");
            each.setUvType(uvType);
        });
        return actualResult;
    }

    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        checkGroupBelongToUser(requestParam.getGid());
        String startDate = requestParam.getStartDate();
        String endDate = requestParam.getEndDate();
        if (StrUtil.isNotBlank(startDate) && startDate.split(" ").length > 2) {
            startDate = startDate.split(" ")[0] + " " + startDate.split(" ")[1];
        }
        if (StrUtil.isNotBlank(endDate) && endDate.split(" ").length > 2) {
            endDate = endDate.split(" ")[0] + " " + endDate.split(" ")[1];
        }
        requestParam.setStartDate(startDate);
        requestParam.setEndDate(endDate);
        IPage<LinkAccessLogsDO> linkAccessLogsDOIPage = linkAccessLogsMapper.selectGroupPage(requestParam);
        if (CollUtil.isEmpty(linkAccessLogsDOIPage.getRecords())) {
            return new Page<>();
        }
        IPage<ShortLinkStatsAccessRecordRespDTO> actualResult = linkAccessLogsDOIPage
                .convert(each -> BeanUtil.toBean(each, ShortLinkStatsAccessRecordRespDTO.class));
        List<String> userAccessLogsList = actualResult.getRecords().stream()
                .map(ShortLinkStatsAccessRecordRespDTO::getUser)
                .toList();
        List<Map<String, Object>> uvTypeList = linkAccessLogsMapper.selectGroupUvTypeByUsers(
                requestParam.getGid(),
                requestParam.getStartDate(),
                requestParam.getEndDate(),
                userAccessLogsList
        );
        actualResult.getRecords().forEach(each -> {
            String uvType = uvTypeList.stream()
                    .filter(item -> Objects.equals(each.getUser(), item.get("user")))
                    .findFirst()
                    .map(item -> item.get("uvType"))
                    .map(Object::toString)
                    .orElse("旧访客");
            each.setUvType(uvType);
        });
        return actualResult;
    }

    public void checkGroupBelongToUser(String gid) throws ServiceException {
        String username = Optional.ofNullable(UserContext.getUsername())
                .orElseThrow(() -> new ServiceException("用户未登录"));
        // 性能优化：Redis 缓存 gid-username 归属关系，10 分钟内跳过 MySQL 查询
        String cacheKey = "stats:gid:" + username + ":" + gid;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if ("1".equals(cached)) {
            return;
        }
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, username);
        List<GroupDO> groupDOList = linkGroupMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(groupDOList)) {
            throw new ServiceException("用户信息与分组标识不匹配");
        }
        stringRedisTemplate.opsForValue().set(cacheKey, "1", 10, TimeUnit.MINUTES);
    }
}
