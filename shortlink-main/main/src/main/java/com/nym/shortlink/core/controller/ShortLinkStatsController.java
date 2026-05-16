package com.nym.shortlink.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nym.shortlink.core.common.biz.ratelimit.RateLimit;
import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.service.ShortLinkStatsService;
import com.nym.shortlink.core.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkGroupStatsReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkStatsReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkStatsRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * 短链接监控控制层
 * 提供短链接和分组短链接的访问统计功能
 */
@RestController(value = "shortLinkStatsControllerByAdmin")
@RequiredArgsConstructor // 使用Lombok的构造器注入，自动生成包含final字段的构造器
@Slf4j // 使用Lombok的日志注解，自动生成log对象
public class ShortLinkStatsController {

    // 注入短链接统计服务
    private final ShortLinkStatsService shortLinkStatsService;

    /**
     * 访问单个短链接指定时间内监控数据
     * @RateLimit 限流注解，限制该接口的访问频率为每秒1000次
     * @GetMapping HTTP GET请求映射，路径为"/api/short-link/admin/v1/stats"
     * @param requestParam 请求参数，包含短链接标识和查询时间范围等信息
     * @return 返回Result包装的ShortLinkStatsRespDTO，包含短链接的统计数据
     */
    @RateLimit(resource = "stats_single", qps = 1000)
    @GetMapping("/api/short-link/admin/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        // 调用服务层获取单个短链接的统计数据，并包装成Result返回
        Result<ShortLinkStatsRespDTO> result = Results.success(shortLinkStatsService.oneShortLinkStats(requestParam));
        return result;
    }

    /**
     * 访问分组短链接指定时间内监控数据
     * @RateLimit 限流注解，限制该接口的访问频率为每秒1000次
     * @GetMapping HTTP GET请求映射，路径为"/api/short-link/admin/v1/stats/group"
     * @param requestParam 请求参数，包含分组标识和查询时间范围等信息
     * @return 返回Result包装的ShortLinkStatsRespDTO，包含分组短链接的统计数据
     */
    @RateLimit(resource = "stats_group", qps = 1000)
    @GetMapping("/api/short-link/admin/v1/stats/group")
    public Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        Result<ShortLinkStatsRespDTO> result = Results.success(shortLinkStatsService.groupShortLinkStats(requestParam));
        return result;
    }

    /**
     * 访问单个短链接指定时间内访问记录监控数据
     */
    @RateLimit(resource = "stats_access_record", qps = 1000)
    @GetMapping("/api/short-link/admin/v1/stats/access-record")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        Result<IPage<ShortLinkStatsAccessRecordRespDTO>> result = Results.success(shortLinkStatsService.shortLinkStatsAccessRecord(requestParam));
        return result;
    }

    /**
        // 调用服务层获取单个短链接的访问记录，并包装成Result返回
     * 访问分组短链接指定时间内访问记录监控数据
     */
    @RateLimit(resource = "stats_group_access_record", qps = 1000)
    @GetMapping("/api/short-link/admin/v1/stats/access-record/group")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        return Results.success(shortLinkStatsService.groupShortLinkStatsAccessRecord(requestParam));
    }
}
