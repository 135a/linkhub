package com.nym.shortlink.trace;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
@ConditionalOnProperty(name = "trace.enabled", havingValue = "true", matchIfMissing = true)
public @interface EnableTrace {
}