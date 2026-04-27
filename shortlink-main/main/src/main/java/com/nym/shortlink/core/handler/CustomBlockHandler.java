package com.nym.shortlink.core.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.dto.req.ShortLinkCreateReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkCreateRespDTO;

import com.nym.shortlink.core.service.PerformanceCounterService;
import com.nym.shortlink.core.toolkit.ApplicationContextHolder;

/**
 * 自定义流控策略
 */
public class CustomBlockHandler {

    public static Result<ShortLinkCreateRespDTO> createShortLinkBlockHandlerMethod(ShortLinkCreateReqDTO requestParam, BlockException exception) {
        ApplicationContextHolder.getBean(PerformanceCounterService.class).incrementSentinelBlock();
        return new Result<ShortLinkCreateRespDTO>().setCode("B100000").setMessage("当前访问网站人数过多，请稍后再试...");
    }
}
