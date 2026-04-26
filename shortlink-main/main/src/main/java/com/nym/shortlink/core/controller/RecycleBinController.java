package com.nym.shortlink.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.dto.req.RecycleBinRecoverReqDTO;
import com.nym.shortlink.core.dto.req.RecycleBinRemoveReqDTO;
import com.nym.shortlink.core.dto.req.RecycleBinSaveReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkPageRespDTO;
import com.nym.shortlink.core.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * 回收站管理控制层
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RecycleBinController {

    private final RecycleBinService recycleBinService;

    /**
     * 保存回收站
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam) {
        log.info("进入接口: saveRecycleBin");
        recycleBinService.saveRecycleBin(requestParam);
        log.info("接口处理完毕: saveRecycleBin");
        return Results.success();
    }

    /**
     * 分页查询回收站短链接
     */
    @GetMapping("/api/short-link/admin/v1/recycle-bin/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        log.info("进入接口: pageShortLink (recycle bin)");
        Result<IPage<ShortLinkPageRespDTO>> result = Results.success(recycleBinService.pageShortLink(requestParam));
        log.info("接口处理完毕: pageShortLink (recycle bin)");
        return result;
    }

    /**
     * 恢复短链接
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/recover")
    public Result<Void> recoverRecycleBin(@RequestBody RecycleBinRecoverReqDTO requestParam) {
        log.info("进入接口: recoverRecycleBin");
        recycleBinService.recoverRecycleBin(requestParam);
        log.info("接口处理完毕: recoverRecycleBin");
        return Results.success();
    }

    /**
     * 移除短链接
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/remove")
    public Result<Void> removeRecycleBin(@RequestBody RecycleBinRemoveReqDTO requestParam) {
        log.info("进入接口: removeRecycleBin");
        recycleBinService.removeRecycleBin(requestParam);
        log.info("接口处理完毕: removeRecycleBin");
        return Results.success();
    }
}
