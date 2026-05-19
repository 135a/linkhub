package com.nym.shortlink.core.config;

import com.nym.shortlink.core.common.biz.user.UserFlowRiskControlFilter;
import com.nym.shortlink.core.common.biz.user.UserTransmitFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 用户配置自动装配
 * 该配置类用于注册和配置与用户相关的过滤器
 */
@Configuration
public class UserConfiguration {

    /**
     * 用户信息传递过滤器
     * 该过滤器用于在整个请求处理过程中传递用户信息
     * @return FilterRegistrationBean<UserTransmitFilter> 过滤器注册Bean
     */
    @Bean
    public FilterRegistrationBean<UserTransmitFilter> globalUserTransmitFilter() {
        // 创建过滤器注册Bean
        FilterRegistrationBean<UserTransmitFilter> registration = new FilterRegistrationBean<>();
        // 设置过滤器实例
        registration.setFilter(new UserTransmitFilter());
        // 设置过滤器拦截的URL模式，拦截所有请求
        registration.addUrlPatterns("/*");
        // 设置过滤器执行顺序，数值越小优先级越高
        registration.setOrder(0);
        // 返回配置好的过滤器注册Bean
        return registration;
    }

    /**
     * 用户操作流量风控过滤器
     * 该过滤器用于限制用户请求频率，防止恶意请求
     * 只有当配置项 short-link.flow-limit.enable 为 true 时才生效
     * @param stringRedisTemplate Redis操作模板，用于存储和获取限流数据
     * @param userFlowRiskControlConfiguration 用户流量风控配置
     * @return FilterRegistrationBean<UserFlowRiskControlFilter> 过滤器注册Bean
     */
    @Bean
    @ConditionalOnProperty(name = "short-link.flow-limit.enable", havingValue = "true")
    public FilterRegistrationBean<UserFlowRiskControlFilter> globalUserFlowRiskControlFilter(
            StringRedisTemplate stringRedisTemplate,
            UserFlowRiskControlConfiguration userFlowRiskControlConfiguration) {
        // 创建过滤器注册Bean
        FilterRegistrationBean<UserFlowRiskControlFilter> registration = new FilterRegistrationBean<>();
        // 设置过滤器实例，并注入Redis模板和配置信息
        registration.setFilter(new UserFlowRiskControlFilter(stringRedisTemplate, userFlowRiskControlConfiguration));
        // 设置过滤器拦截的URL模式，拦截所有请求
        registration.addUrlPatterns("/*");
        // 设置过滤器执行顺序，数值越小优先级越高
        registration.setOrder(10);
        // 返回配置好的过滤器注册Bean
        return registration;
    }
}
