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
import jakarta.validation.Valid;
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
 * 提供短链接的创建、修改、查询和跳转等功能
 */
@RestController(value = "shortLinkControllerByAdmin") // 使用REST风格控制器，指定Bean名称为shortLinkControllerByAdmin
@RequiredArgsConstructor // 使用Lombok的@RequiredArgsConstructor注解，自动生成包含final字段的构造函数
@Slf4j // 使用Lombok的@Slf4j注解，自动生成日志记录器
public class ShortLinkController { // 短链接控制器类

    private final ShortLinkService shortLinkService; // 注入短链接服务接口

    /**
     * 创建短链接
     * 使用限流注解控制访问频率，QPS限制为200
     * @param requestParam 创建短链接的请求参数
     * @return 返回创建结果，包含生成的短链接信息
     */
    @RateLimit(resource = "create_short-link", qps = 200, controlBehavior = RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER, maxQueueingTimeMs = 2000, message = "创建请求过于频繁，请稍后再试")
    @PostMapping("/api/short-link/admin/v1/create") // POST请求映射，创建短链接的API路径
    public Result<ShortLinkCreateRespDTO> createShortLink(@Valid @RequestBody ShortLinkCreateReqDTO requestParam) { // 接收创建短链接的请求参数
        Result<ShortLinkCreateRespDTO> result = Results.success(shortLinkService.createShortLink(requestParam)); // 调用服务层方法创建短链接并返回结果
        return result;
    }

    /**
     * 创建短链接（分布式锁版本）
     * 使用限流注解控制访问频率，QPS限制为100
     * @param requestParam 创建短链接的请求参数
     * @return 返回创建结果，包含生成的短链接信息
     */
    @RateLimit(resource = "create-by-lock_short-link", qps = 100, controlBehavior = RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER, maxQueueingTimeMs = 2000, message = "创建请求过于频繁，请稍后再试")
    @PostMapping("/api/short-link/admin/v1/create/by-lock") // POST请求映射，使用分布式锁创建短链接的API路径
    public Result<ShortLinkCreateRespDTO> createShortLinkByLock(@Valid @RequestBody ShortLinkCreateReqDTO requestParam) { // 接收创建短链接的请求参数
        return Results.success(shortLinkService.createShortLinkByLock(requestParam)); // 调用服务层方法（使用分布式锁）创建短链接并返回结果
    }

    /**
     * 批量创建短链接
     * 使用限流注解控制访问频率，QPS限制为100
     * @param requestParam 批量创建短链接的请求参数
     * @param response HTTP响应对象，用于返回Excel文件
     */
    @RateLimit(resource = "batch-create_short-link", qps = 100, controlBehavior = RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER, maxQueueingTimeMs = 5000, message = "批量创建请求过于频繁，请稍后再试")
    @SneakyThrows // 使用Lombok的@SneakyThrows注解，自动抛出检查型异常
    @PostMapping("/api/short-link/admin/v1/create/batch") // POST请求映射，批量创建短链接的API路径
    public void batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam,
            HttpServletResponse response) { // 接收批量创建短链接的请求参数和HTTP响应对象
        ShortLinkBatchCreateRespDTO shortLinkBatchCreateRespDTO = shortLinkService.batchCreateShortLink(requestParam); // 调用服务层方法批量创建短链接
        if (shortLinkBatchCreateRespDTO != null) { // 如果批量创建结果不为空
            List<ShortLinkBaseInfoRespDTO> baseLinkInfos = shortLinkBatchCreateRespDTO.getBaseLinkInfos(); // 获取短链接基本信息列表
            EasyExcelWebUtil.write(response, "批量创建短链接-短链接系统", ShortLinkBaseInfoRespDTO.class, baseLinkInfos); // 使用EasyExcel工具将结果写入Excel响应
        }
    }

    /**
     * 修改短链接
     * 使用限流注解控制访问频率，QPS限制为5
     * @param requestParam 修改短链接的请求参数
     * @return 返回操作结果
     */
    @RateLimit(resource = "update_short-link", qps = 5)
    @PostMapping("/api/short-link/admin/v1/update") // POST请求映射，修改短链接的API路径
    public Result<Void> updateShortLink(@Valid @RequestBody ShortLinkUpdateReqDTO requestParam) { // 接收修改短链接的请求参数
        shortLinkService.updateShortLink(requestParam); // 调用服务层方法修改短链接
        return Results.success(); // 返回操作成功结果
    }

    /**
     * 分页查询短链接
     * 使用限流注解控制访问频率，QPS限制为20
     * @param requestParam 分页查询短链接的请求参数
     * @return 返回分页查询结果，包含短链接列表和分页信息
     */
    @RateLimit(resource = "page_short-link", qps = 20)
    @GetMapping("/api/short-link/admin/v1/page") // GET请求映射，分页查询短链接的API路径
    public Result<Page<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) { // 接收分页查询短链接的请求参数
        Result<Page<ShortLinkPageRespDTO>> result = Results
                .success((Page<ShortLinkPageRespDTO>) shortLinkService.pageShortLink(requestParam)); // 调用服务层方法分页查询短链接并返回结果
        return result;
    }

    /**
     * 短链接跳转
     * 使用限流注解控制访问频率，QPS限制为5000
     * @param shortUri 短链接的后缀
     * @param request HTTP请求对象，用于获取请求信息
     * @param response HTTP响应对象，用于重定向到原始URL
     */
    @RateLimit(resource = "redirect_short-link", qps = 5000)
    @SneakyThrows // 使用Lombok的@SneakyThrows注解，自动抛出检查型异常
    @GetMapping("/{shortUri}") // GET请求映射，短链接跳转的API路径，shortUri为路径变量
    public void restoreUrl(@PathVariable("shortUri") String shortUri, HttpServletRequest request,
            HttpServletResponse response) {
        shortLinkService.restoreUrl(shortUri, request, response);
    }
}
