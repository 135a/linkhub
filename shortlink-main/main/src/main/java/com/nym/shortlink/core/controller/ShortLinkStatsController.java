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
 */
@RestController(value = "shortLinkStatsControllerByAdmin")
@RequiredArgsConstructor
@Slf4j
public class ShortLinkStatsController {

    private final ShortLinkStatsService shortLinkStatsService;

    /**
     * 访问单个短链接指定时间内监控数据
     */
    @RateLimit(resource = "stats_single", qps = 10)
    @GetMapping("/api/short-link/admin/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        Result<ShortLinkStatsRespDTO> result = Results.success(shortLinkStatsService.oneShortLinkStats(requestParam));
        return result;
    }

    /**
     * 访问分组短链接指定时间内监控数据
     */
    @RateLimit(resource = "stats_group", qps = 10)
    @GetMapping("/api/short-link/admin/v1/stats/group")
    public Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        Result<ShortLinkStatsRespDTO> result = Results.success(shortLinkStatsService.groupShortLinkStats(requestParam));
        return result;
    }

    /**
     * 访问单个短链接指定时间内访问记录监控数据
     */
    @RateLimit(resource = "stats_access_record", qps = 10)
    @GetMapping("/api/short-link/admin/v1/stats/access-record")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        Result<IPage<ShortLinkStatsAccessRecordRespDTO>> result = Results.success(shortLinkStatsService.shortLinkStatsAccessRecord(requestParam));
        return result;
    }

    /**
     * 访问分组短链接指定时间内访问记录监控数据
     */
    @RateLimit(resource = "stats_group_access_record", qps = 10)
    @GetMapping("/api/short-link/admin/v1/stats/access-record/group")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        Result<IPage<ShortLinkStatsAccessRecordRespDTO>> result = Results.success(shortLinkStatsService.groupShortLinkStatsAccessRecord(requestParam));
        return result;
    }
}
