package com.nym.shortlink.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.nym.shortlink.core.common.biz.ratelimit.RateLimit;
import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.service.ShortLinkService;
import com.nym.shortlink.core.dto.req.ShortLinkBatchCreateReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkCreateReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkPageReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkUpdateReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkBaseInfoRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkBatchCreateRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkCreateRespDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkPageRespDTO;
import com.nym.shortlink.core.toolkit.EasyExcelWebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 短链接后管控制层
 */
@RestController(value = "shortLinkControllerByAdmin")
@RequiredArgsConstructor
@Slf4j
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    /**
     * 创建短链接
     */
    @RateLimit(resource = "create_short-link", qps = 1,
            controlBehavior = RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER, maxQueueingTimeMs = 2000,
            message = "创建请求过于频繁，请稍后再试")
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam) {
        Result<ShortLinkCreateRespDTO> result = Results.success(shortLinkService.createShortLink(requestParam));
        return result;
    }

    /**
     * 批量创建短链接
     */
    @RateLimit(resource = "batch-create_short-link", qps = 1,
            controlBehavior = RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER, maxQueueingTimeMs = 5000,
            message = "批量创建请求过于频繁，请稍后再试")
    @SneakyThrows
    @PostMapping("/api/short-link/admin/v1/create/batch")
    public void batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam, HttpServletResponse response) {
        ShortLinkBatchCreateRespDTO shortLinkBatchCreateRespDTO = shortLinkService.batchCreateShortLink(requestParam);
        if (shortLinkBatchCreateRespDTO != null) {
            List<ShortLinkBaseInfoRespDTO> baseLinkInfos = shortLinkBatchCreateRespDTO.getBaseLinkInfos();
            EasyExcelWebUtil.write(response, "批量创建短链接-短链接系统", ShortLinkBaseInfoRespDTO.class, baseLinkInfos);
        }
    }

    /**
     * 修改短链接
     */
    @RateLimit(resource = "update_short-link", qps = 5)
    @PostMapping("/api/short-link/admin/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        shortLinkService.updateShortLink(requestParam);
        return Results.success();
    }

    /**
     * 分页查询短链接
     */
    @RateLimit(resource = "page_short-link", qps = 20)
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<Page<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
        Result<Page<ShortLinkPageRespDTO>> result = Results.success((Page<ShortLinkPageRespDTO>) shortLinkService.pageShortLink(requestParam));
        return result;
    }

    /**
     * 短链接跳转
     */
    @RateLimit(resource = "redirect_short-link", qps = 100)
    @SneakyThrows
    @GetMapping("/{shortUri}")
    public void restoreUrl(@PathVariable("shortUri") String shortUri, HttpServletRequest request, HttpServletResponse response) {
        log.info("收到短链接跳转请求, shortUri: {}", shortUri);
        shortLinkService.restoreUrl(shortUri, request, response);
    }
}
