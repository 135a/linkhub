package com.nym.shortlink.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nym.shortlink.core.common.biz.ratelimit.RateLimit;
import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.dto.req.RecycleBinRecoverReqDTO;
import com.nym.shortlink.core.dto.req.RecycleBinRemoveReqDTO;
import com.nym.shortlink.core.dto.req.RecycleBinSaveReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkPageRespDTO;
import com.nym.shortlink.core.service.RecycleBinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * 回收站管理控制层
 * 该类提供了回收站相关的RESTful API接口，包括保存回收站、分页查询、恢复和移除短链接等功能
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RecycleBinController {

    // 注入回收站服务接口
    private final RecycleBinService recycleBinService;

    /**
     * 保存回收站
     * @param requestParam 保存回收站的请求参数
     * @return 返回操作结果
     * @RateLimit 限制该接口的访问频率为每秒5次
     */
    @RateLimit(resource = "recycle_save", qps = 5)
    @PostMapping("/api/short-link/admin/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@Valid @RequestBody RecycleBinSaveReqDTO requestParam) {
        recycleBinService.saveRecycleBin(requestParam);
        return Results.success();
    }

    /**
     * 分页查询回收站短链接
     * @param requestParam 分页查询请求参数
     * @return 返回分页查询结果
     * @RateLimit 限制该接口的访问频率为每秒20次
     */
    @RateLimit(resource = "recycle_page", qps = 20)
    @GetMapping("/api/short-link/admin/v1/recycle-bin/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        Result<IPage<ShortLinkPageRespDTO>> result = Results.success(recycleBinService.pageShortLink(requestParam));
        return result;
    }

    /**
     * 恢复短链接
     * @param requestParam 恢复短链接的请求参数
     * @return 返回操作结果
     * @RateLimit 限制该接口的访问频率为每秒5次
     */
    @RateLimit(resource = "recycle_recover", qps = 5)
    @PostMapping("/api/short-link/admin/v1/recycle-bin/recover")
    public Result<Void> recoverRecycleBin(@Valid @RequestBody RecycleBinRecoverReqDTO requestParam) {
        recycleBinService.recoverRecycleBin(requestParam);
        return Results.success();
    }

    /**
     * 移除短链接
     * @param requestParam 移除短链接的请求参数
     * @return 返回操作结果
     * @RateLimit 限制该接口的访问频率为每秒5次
     */
    @RateLimit(resource = "recycle_remove", qps = 5)
    @PostMapping("/api/short-link/admin/v1/recycle-bin/remove")
    public Result<Void> removeRecycleBin(@Valid @RequestBody RecycleBinRemoveReqDTO requestParam) {
        recycleBinService.removeRecycleBin(requestParam);
        return Results.success();
    }
}
