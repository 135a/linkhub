package com.nym.shortlink.core.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseEmitterService {

    // 存储用户的 SseEmitter，Key: username
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    public SseEmitter createConnect(String username) {
        // 设置超时时间为 0 表示不超时，或者设置一个较长的时间例如 1 小时
        SseEmitter emitter = new SseEmitter(0L);
        emitterMap.put(username, emitter);

        // 设置回调
        emitter.onCompletion(() -> emitterMap.remove(username));
        emitter.onTimeout(() -> emitterMap.remove(username));
        emitter.onError(e -> emitterMap.remove(username));

        return emitter;
    }

    /**
     * 向指定用户发送更新通知
     */
    public void sendUpdate(String username, String gid) {
        SseEmitter emitter = emitterMap.get(username);
        if (emitter != null) {
            try {
                // 发送格式：data: {"gid": "xxx"}
                emitter.send(SseEmitter.event().name("update").data("{\"gid\":\"" + gid + "\"}"));
            } catch (IOException e) {
                emitterMap.remove(username);
            }
        }
    }

    /**
     * 广播给所有在线用户 (如果不知道谁是谁)
     */
    public void broadcastUpdate(String gid) {
        emitterMap.forEach((username, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("update").data("{\"gid\":\"" + gid + "\"}"));
            } catch (IOException e) {
                emitterMap.remove(username);
            }
        });
    }
}
