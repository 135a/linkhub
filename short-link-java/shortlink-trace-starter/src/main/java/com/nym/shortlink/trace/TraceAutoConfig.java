package com.nym.shortlink.trace;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TraceAutoConfig {

    @Bean
    public RestTemplate traceRestTemplate() {
        return new RestTemplate();
    }
}