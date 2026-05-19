package com.nym.shortlink.core.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE(Server-Sent Events)服务类，用于处理服务器向客户端推送消息的场景
 * 使用Spring框架的SseEmitter实现服务端推送功能
 */
@Service
public class SseEmitterService {

    // 存储用户的 SseEmitter，Key: username，使用ConcurrentHashMap保证线程安全
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 创建与客户端的SSE连接
     * @param username 用户名，作为连接的唯一标识
     * @return SseEmitter对象，用于向客户端推送消息
     */
    public SseEmitter createConnect(String username) {
        // 设置超时时间为 0 表示不超时，或者设置一个较长的时间例如 1 小时
        SseEmitter emitter = new SseEmitter(0L);
        emitterMap.put(username, emitter);

        // 设置回调：当连接完成、超时或发生错误时，从map中移除对应的emitter
        emitter.onCompletion(() -> emitterMap.remove(username));
        emitter.onTimeout(() -> emitterMap.remove(username));
        emitter.onError(e -> emitterMap.remove(username));

        return emitter;
    }

    /**
     * 向指定用户发送更新通知
     * @param username 接收通知的用户名
     * @param gid 要推送的数据内容
     */
    public void sendUpdate(String username, String gid) {
        // 获取指定用户的emitter
        SseEmitter emitter = emitterMap.get(username);
        if (emitter != null) {
            try {
                // 发送格式：data: {"gid": "xxx"}
                emitter.send(SseEmitter.event().name("update").data("{\"gid\":\"" + gid + "\"}"));
            } catch (IOException e) {
                // 发送失败时，移除该用户的emitter
                emitterMap.remove(username);
            }
        }
    }

    private final Map<String, Long> lastPushTimeMap = new ConcurrentHashMap<>();

    /**
     * 广播给所有在线用户 (如果不知道谁是谁)
     */
    public void broadcastUpdate(String gid) {
        // 限流防抖：同一个分组的数据更新，最多每 2 秒向前端推送一次
        // 防止在压测或高并发场景下，产生 SSE 更新风暴导致前端浏览器卡死
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastPushTimeMap.getOrDefault(gid, 0L);
        if (currentTime - lastTime < 2000) {
            return; 
        }
        lastPushTimeMap.put(gid, currentTime);

        emitterMap.forEach((username, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("update").data("{\"gid\":\"" + gid + "\"}"));
            } catch (IOException e) {
                emitterMap.remove(username);
            }
        });
    }
}
