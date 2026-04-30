package com.nym.shortlink.core.mq.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nym.shortlink.core.common.convention.exception.ServiceException;
import com.nym.shortlink.core.dao.entity.*;
import com.nym.shortlink.core.dao.mapper.*;
import com.nym.shortlink.core.dao.mapper.clickhouse.ClickHouseStatsMapper;
import com.nym.shortlink.core.dto.biz.ShortLinkStatsRecordDTO;
import com.nym.shortlink.core.mq.idempotent.MessageQueueIdempotentHandler;
import com.nym.shortlink.core.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.nym.shortlink.core.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;
import static com.nym.shortlink.core.common.constant.RedisKeyConstant.SHORT_LINK_STATS_UIP_KEY;
import static com.nym.shortlink.core.common.constant.RedisKeyConstant.SHORT_LINK_STATS_UV_KEY;
import static com.nym.shortlink.core.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;

/**
 * 短链接监控状态保存消息队列消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${rocketmq.producer.topic}",
        consumerGroup = "${rocketmq.consumer.group}"
)
public class ShortLinkStatsSaveConsumer implements RocketMQListener<Map<String, String>> {

    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;
    private final ClickHouseStatsMapper clickHouseStatsMapper;
    private final SseEmitterService sseEmitterService;

    @Value("${stats.storage.primary:clickhouse}")
    private String statsPrimary;

    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocaleAmapKey;

    @Override
    public void onMessage(Map<String, String> producerMap) {
        String keys = producerMap.get("keys");
        if (!messageQueueIdempotentHandler.isMessageBeingConsumed(keys)) {
            // 判断当前的这个消息流程是否执行完成
            if (messageQueueIdempotentHandler.isAccomplish(keys)) {
                return;
            }
            throw new ServiceException("消息未完成流程，需要消息队列重试");
        }
        try {
            ShortLinkStatsRecordDTO statsRecord = JSON.parseObject(producerMap.get("statsRecord"), ShortLinkStatsRecordDTO.class);
            if (statsRecord != null) {
                String fullShortUrl = Optional.ofNullable(producerMap.get("fullShortUrl")).orElse(statsRecord.getFullShortUrl());
                String gid = producerMap.get("gid");
                actualSaveShortLinkStats(fullShortUrl, gid, statsRecord);
            }
        } catch (Throwable ex) {
            // 删除幂等标识
            messageQueueIdempotentHandler.delMessageProcessed(keys);
            log.error("记录短链接监控消费异常", ex);
            throw ex;
        }
        messageQueueIdempotentHandler.setAccomplish(keys);
    }

    public void actualSaveShortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord) {
        fullShortUrl = Optional.ofNullable(fullShortUrl).orElse(statsRecord.getFullShortUrl());
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        try {
            // 此处进行真正的 UV/UIP 首次访问判断（已从主链路移出，避免阀塞 Tomcat 线程）
            boolean uvFirstFlag;
            if (statsRecord.getUvFirstFlag()) {
                // 主链路判断为新 Cookie（必定是新 UV），直接写入 Set
                stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, statsRecord.getUv());
                uvFirstFlag = true;
            } else {
                // 旧 Cookie，判断该 uv 值是否已在 Set
                Long uvAdded = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, statsRecord.getUv());
                uvFirstFlag = uvAdded != null && uvAdded > 0L;
            }
            // UIP 判断：由 Consumer 做，主链路不再内联
            Long uipAdded = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UIP_KEY + fullShortUrl, statsRecord.getRemoteAddr());
            boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
            // 用实际判断结果覆盖 DTO中的 placeholder
            statsRecord = statsRecord.toBuilder()
                    .uvFirstFlag(uvFirstFlag)
                    .uipFirstFlag(uipFirstFlag)
                    .build();
            if (StrUtil.isBlank(gid)) {
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                gid = shortLinkGotoDO.getGid();
            }
            // 解析高德 IP 地理位置（设置 2s 超时，避免外部 API 慢响应阻塞 Consumer 线程）
            Map<String, Object> localeParamMap = new HashMap<>();
            localeParamMap.put("key", statsLocaleAmapKey);
            localeParamMap.put("ip", statsRecord.getRemoteAddr());
            String actualProvince = "未知";
            String actualCity = "未知";
            String actualAdcode = "未知";
            try {
                String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL, localeParamMap, 2000);
                JSONObject localeResultObj = JSON.parseObject(localeResultStr);
                String infoCode = localeResultObj.getString("infocode");
                if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode, "10000")) {
                    String province = localeResultObj.getString("province");
                    boolean unknownFlag = StrUtil.equals(province, "[]");
                    actualProvince = unknownFlag ? actualProvince : province;
                    actualCity = unknownFlag ? actualCity : localeResultObj.getString("city");
                    actualAdcode = unknownFlag ? "未知" : localeResultObj.getString("adcode");
                }
            } catch (Exception ex) {
                log.warn("调用高德地图解析地理位置超时或异常，降级为未知地区: {}", ex.getMessage());
            }

            // 根据降级开关选择写入目标
            if ("clickhouse".equalsIgnoreCase(statsPrimary)) {
                saveToClickHouse(fullShortUrl, gid, statsRecord, actualProvince, actualCity, actualAdcode);
            }
            // 始终双写到 MySQL，保证未迁移到 ClickHouse 的统计维度（如分组统计、浏览器/OS分布）能正常查询
            saveToMySQL(fullShortUrl, gid, statsRecord, actualProvince, actualCity, actualAdcode, !"clickhouse".equalsIgnoreCase(statsPrimary));
            
            // 广播 SSE 事件，通知前端更新数据
            sseEmitterService.broadcastUpdate(gid);
        } catch (Throwable ex) {
            log.error("短链接访问量统计异常", ex);
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
    }

    /**
     * 写入 ClickHouse 统计数据
     */
    private void saveToClickHouse(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord,
                                  String province, String city, String adcode) {
        String dateStr = cn.hutool.core.date.DateUtil.formatDate(new Date());
        int hour = DateUtil.hour(new Date(), true);
        Week week = DateUtil.dayOfWeekEnum(new Date());
        int weekValue = week.getIso8601Value();

        clickHouseStatsMapper.insertAccessStats(fullShortUrl, dateStr,
                1, statsRecord.getUvFirstFlag() ? 1 : 0, statsRecord.getUipFirstFlag() ? 1 : 0,
                hour, weekValue);
        clickHouseStatsMapper.insertLocaleStats(fullShortUrl, dateStr,
                1, province, city, adcode, "中国");
        clickHouseStatsMapper.insertOsStats(fullShortUrl, dateStr, 1, statsRecord.getOs());
        clickHouseStatsMapper.insertBrowserStats(fullShortUrl, dateStr, 1, statsRecord.getBrowser());
        clickHouseStatsMapper.insertDeviceStats(fullShortUrl, dateStr, 1, statsRecord.getDevice());
        clickHouseStatsMapper.insertNetworkStats(fullShortUrl, dateStr, 1, statsRecord.getNetwork());
        clickHouseStatsMapper.insertAccessLog(fullShortUrl,
                statsRecord.getUv(), statsRecord.getRemoteAddr(),
                statsRecord.getBrowser(), statsRecord.getOs(),
                statsRecord.getNetwork(), statsRecord.getDevice(),
                StrUtil.join("-", "中国", province, city));
        clickHouseStatsMapper.insertStatsTodayStats(fullShortUrl, dateStr,
                1, statsRecord.getUvFirstFlag() ? 1 : 0, statsRecord.getUipFirstFlag() ? 1 : 0);
        // 同步更新主表 PV/UV/UIP 汇总字段（仍写 MySQL）
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        try {
            shortLinkMapper.incrementStats(gid, fullShortUrl,
                    1, statsRecord.getUvFirstFlag() ? 1 : 0, statsRecord.getUipFirstFlag() ? 1 : 0);
        } finally {
            if (rLock.isHeldByCurrentThread()) rLock.unlock();
        }
    }

    /**
     * 降级写入 MySQL 统计数据（原有逻辑），双写模式下仍需写入以便未迁移的接口查询
     */
    private void saveToMySQL(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord,
                             String province, String city, String adcode, boolean shouldIncrementStats) {
        int hour = DateUtil.hour(new Date(), true);
        Week week = DateUtil.dayOfWeekEnum(new Date());
        int weekValue = week.getIso8601Value();
        LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                .pv(1)
                .uv(statsRecord.getUvFirstFlag() ? 1 : 0)
                .uip(statsRecord.getUipFirstFlag() ? 1 : 0)
                .hour(hour)
                .weekday(weekValue)
                .fullShortUrl(fullShortUrl)
                .date(new Date())
                .build();
        linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
        LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                .province(province)
                .city(city)
                .adcode(adcode)
                .cnt(1)
                .fullShortUrl(fullShortUrl)
                .country("中国")
                .date(new Date())
                .build();
        linkLocaleStatsMapper.shortLinkLocaleState(linkLocaleStatsDO);
        LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                .os(statsRecord.getOs())
                .cnt(1)
                .fullShortUrl(fullShortUrl)
                .date(new Date())
                .build();
        linkOsStatsMapper.shortLinkOsState(linkOsStatsDO);
        LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                .browser(statsRecord.getBrowser())
                .cnt(1)
                .fullShortUrl(fullShortUrl)
                .date(new Date())
                .build();
        linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);
        LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                .device(statsRecord.getDevice())
                .cnt(1)
                .fullShortUrl(fullShortUrl)
                .date(new Date())
                .build();
        linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
        LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                .network(statsRecord.getNetwork())
                .cnt(1)
                .fullShortUrl(fullShortUrl)
                .date(new Date())
                .build();
        linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
        LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                .user(statsRecord.getUv())
                .ip(statsRecord.getRemoteAddr())
                .browser(statsRecord.getBrowser())
                .os(statsRecord.getOs())
                .network(statsRecord.getNetwork())
                .device(statsRecord.getDevice())
                .locale(StrUtil.join("-", "中国", province, city))
                .fullShortUrl(fullShortUrl)
                .build();
        linkAccessLogsMapper.insert(linkAccessLogsDO);
        if (shouldIncrementStats) {
            shortLinkMapper.incrementStats(gid, fullShortUrl, 1,
                    statsRecord.getUvFirstFlag() ? 1 : 0, statsRecord.getUipFirstFlag() ? 1 : 0);
        }
        LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                .todayPv(1)
                .todayUv(statsRecord.getUvFirstFlag() ? 1 : 0)
                .todayUip(statsRecord.getUipFirstFlag() ? 1 : 0)
                .fullShortUrl(fullShortUrl)
                .date(new Date())
                .build();
        linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
    }
}
