package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE (Server-Sent Events) 控制器
 * 用于处理服务器发送事件的相关请求
 */
@RestController
@RequiredArgsConstructor
public class SseController {

    /**
     * SSE发射器服务
     * 用于管理SseEmitter实例和连接
     */
    private final SseEmitterService sseEmitterService;

    /**
     * 处理SSE连接请求
     * @param username 用户名，用于标识连接
     * @return SseEmitter实例，用于向客户端推送事件
     */
    @GetMapping(value = "/api/short-link/admin/v1/sse/connect", produces = "text/event-stream")
    public SseEmitter connect(@RequestParam("username") String username) {
        return sseEmitterService.createConnect(username);
    }
}
