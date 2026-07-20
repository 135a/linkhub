package com.nym.shortlink.core.config;

import com.nym.shortlink.core.common.biz.user.UserTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final StringRedisTemplate stringRedisTemplate;

    public WebMvcConfiguration(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/api/short-link/admin/**")
                .excludePathPatterns("/api/short-link/admin/v1/user/login")
                .excludePathPatterns("/api/short-link/admin/v1/user/has-username")
                .excludePathPatterns("/api/short-link/admin/v1/user/check-login");
    }
}
