package com.nym.shortlink.trace;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ComponentScan("com.nym.shortlink.trace")
@EnableScheduling
public class TraceAutoConfig {

    @Bean
    public RestTemplate traceRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "short-link.trace.log.enabled", havingValue = "true", matchIfMissing = true)
    public com.nym.shortlink.trace.aspect.LogAspect logAspect() {
        return new com.nym.shortlink.trace.aspect.LogAspect();
    }
}