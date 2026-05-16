package com.nym.shortlink.core.common.biz.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

import static com.nym.shortlink.core.common.constant.RedisCacheConstant.USER_LOGIN_KEY;

/**
 * 用户Token拦截器类
 * 用于验证用户请求中的Token是否有效，实现用户认证功能
 */
public class UserTokenInterceptor implements HandlerInterceptor {

    // 前端 axios 发送的 HTTP Header key 名称
    private static final String HEADER_USERNAME = "username";
    private static final String HEADER_TOKEN = "Token";

    // Redis操作模板，用于与Redis数据库交互
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造函数，注入StringRedisTemplate实例
     * @param stringRedisTemplate Redis操作模板
     */
    public UserTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 前置拦截方法
     * 在请求处理前进行用户Token验证
     * @param request 当前HTTP请求
     * @param response 当前HTTP响应
     * @param handler 请求处理方法
     * @return true:继续流程 false:终端流程
     * @throws Exception 可能发生的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头中获取用户名
        String username = request.getHeader(HEADER_USERNAME);
        // 如果请求头中没有用户名，则从请求参数中获取
        if (!StringUtils.hasText(username)) {
            username = request.getParameter(HEADER_USERNAME);
        }
        
        // 从请求头中获取Token
        String token = request.getHeader(HEADER_TOKEN);
        // 如果请求头中没有Token，则从请求参数中获取
        if (!StringUtils.hasText(token)) {
            token = request.getParameter(HEADER_TOKEN);
        }

        // 当用户名和Token都存在时进行验证
        if (StringUtils.hasText(username) && StringUtils.hasText(token)) {
            // 从Redis中获取用户信息
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
