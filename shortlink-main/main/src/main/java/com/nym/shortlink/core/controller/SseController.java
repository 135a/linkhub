package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/api/short-link/admin/v1/sse/connect", produces = "text/event-stream")
    public SseEmitter connect(@RequestParam("username") String username) {
        return sseEmitterService.createConnect(username);
    }
}
