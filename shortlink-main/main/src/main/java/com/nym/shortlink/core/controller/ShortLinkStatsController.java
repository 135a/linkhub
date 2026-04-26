package com.nym.shortlink.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
    @GetMapping("/api/short-link/admin/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        log.info("进入接口: shortLinkStats");
        Result<ShortLinkStatsRespDTO> result = Results.success(shortLinkStatsService.oneShortLinkStats(requestParam));
        log.info("接口处理完毕: shortLinkStats");
        return result;
    }

    /**
     * 访问分组短链接指定时间内监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/group")
    public Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        log.info("进入接口: groupShortLinkStats");
        Result<ShortLinkStatsRespDTO> result = Results.success(shortLinkStatsService.groupShortLinkStats(requestParam));
        log.info("接口处理完毕: groupShortLinkStats");
        return result;
    }

    /**
     * 访问单个短链接指定时间内访问记录监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/access-record")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        log.info("进入接口: shortLinkStatsAccessRecord");
        Result<IPage<ShortLinkStatsAccessRecordRespDTO>> result = Results.success(shortLinkStatsService.shortLinkStatsAccessRecord(requestParam));
        log.info("接口处理完毕: shortLinkStatsAccessRecord");
        return result;
    }

    /**
     * 访问分组短链接指定时间内访问记录监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/access-record/group")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        log.info("进入接口: groupShortLinkStatsAccessRecord");
        Result<IPage<ShortLinkStatsAccessRecordRespDTO>> result = Results.success(shortLinkStatsService.groupShortLinkStatsAccessRecord(requestParam));
        log.info("接口处理完毕: groupShortLinkStatsAccessRecord");
        return result;
    }
}
