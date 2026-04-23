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
}