/*
 * Copyright © 2026 NageOffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nym.shortlink.admin.config;

import com.nym.shortlink.admin.common.biz.user.UserContext;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * openFeign 微服务调用传递用户信息配置
 */
@Configuration
public class OpenFeignConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            template.header("username", UserContext.getUsername());
            template.header("userId", UserContext.getUserId());
            template.header("realName", UserContext.getRealName());
        };
    }

    /**
     * Bound every Feign call:
     *   - connectTimeout 500ms covers TCP handshake + TLS on a well-behaved
     *     intra-compose network.
     *   - readTimeout 1000ms stops a single slow downstream from stalling the
     *     admin request far beyond its latency budget (see
     *     {@code critical-path-latency-budget} spec).
     * Callers that need graceful degradation (e.g. {@code listGroup}) catch
     * the resulting exception and return partial data.
     */
    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(
                500, TimeUnit.MILLISECONDS,
                1000, TimeUnit.MILLISECONDS,
                true
        );
    }
}
