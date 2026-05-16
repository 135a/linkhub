package com.nym.shortlink.core.config;

import com.nym.shortlink.core.common.biz.user.UserTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类，用于配置 Spring MVC 的各种组件和行为
 * 实现了 WebMvcConfigurer 接口，以自定义 MVC 的配置
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    /**
     * StringRedisTemplate 的实例，用于操作 Redis
     * 用于在拦截器中验证用户登录状态
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造函数，通过依赖注入方式获取 StringRedisTemplate 实例
     * @param stringRedisTemplate Redis 操作模板
     */
    public WebMvcConfiguration(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 添加拦截器到拦截器注册器中
     * @param registry 拦截器注册器，用于注册和配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加用户令牌拦截器，用于验证用户登录状态
        registry.addInterceptor(new UserTokenInterceptor(stringRedisTemplate))
                // 拦截所有管理员 API 路径
                .addPathPatterns("/api/short-link/admin/**")
                // 排除登录相关路径，这些路径不需要验证登录状态
                .excludePathPatterns("/api/short-link/admin/v1/user/login")
                .excludePathPatterns("/api/short-link/admin/v1/user/has-username")
                .excludePathPatterns("/api/short-link/admin/v1/user/check-login");
    }
}
