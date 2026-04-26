package com.nym.shortlink.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.GetMapping;
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
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam) {
        log.info("进入接口: createShortLink");
        Result<ShortLinkCreateRespDTO> result = Results.success(shortLinkService.createShortLink(requestParam));
        log.info("接口处理完毕: createShortLink");
        return result;
    }

    /**
     * 批量创建短链接
     */
    @SneakyThrows
    @PostMapping("/api/short-link/admin/v1/create/batch")
    public void batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam, HttpServletResponse response) {
        log.info("进入接口: batchCreateShortLink");
        ShortLinkBatchCreateRespDTO shortLinkBatchCreateRespDTO = shortLinkService.batchCreateShortLink(requestParam);
        if (shortLinkBatchCreateRespDTO != null) {
            List<ShortLinkBaseInfoRespDTO> baseLinkInfos = shortLinkBatchCreateRespDTO.getBaseLinkInfos();
            EasyExcelWebUtil.write(response, "批量创建短链接-短链接系统", ShortLinkBaseInfoRespDTO.class, baseLinkInfos);
        }
        log.info("接口处理完毕: batchCreateShortLink");
    }

    /**
     * 修改短链接
     */
    @PostMapping("/api/short-link/admin/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        log.info("进入接口: updateShortLink");
        shortLinkService.updateShortLink(requestParam);
        log.info("接口处理完毕: updateShortLink");
        return Results.success();
    }

    /**
     * 分页查询短链接
     */
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<Page<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
        log.info("进入接口: pageShortLink");
        Result<Page<ShortLinkPageRespDTO>> result = Results.success((Page<ShortLinkPageRespDTO>) shortLinkService.pageShortLink(requestParam));
        log.info("接口处理完毕: pageShortLink");
        return result;
    }
}
