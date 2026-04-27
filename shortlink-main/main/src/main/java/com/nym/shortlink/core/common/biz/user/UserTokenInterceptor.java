package com.nym.shortlink.core.common.biz.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

import static com.nym.shortlink.core.common.constant.RedisCacheConstant.USER_LOGIN_KEY;

public class UserTokenInterceptor implements HandlerInterceptor {

    // 前端 axios 发送的 HTTP Header key 名称
    private static final String HEADER_USERNAME = "username";
    private static final String HEADER_TOKEN = "Token";

    private final StringRedisTemplate stringRedisTemplate;

    public UserTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String username = request.getHeader(HEADER_USERNAME);
        String token = request.getHeader(HEADER_TOKEN);

        if (StringUtils.hasText(username) && StringUtils.hasText(token)) {
            Object userInfo = stringRedisTemplate.opsForHash().get(USER_LOGIN_KEY + username, token);
            if (userInfo != null) {
                // 续签 Token 有效期
                stringRedisTemplate.expire(USER_LOGIN_KEY + username, 30, TimeUnit.MINUTES);
                return true;
            }
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"A000200\",\"message\":\"用户Token不存在或用户未登录\",\"success\":false}");
        return false;
    }
}
