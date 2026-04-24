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
package com.nym.shortlink.admin.common.biz.user;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.nym.shortlink.admin.common.convention.exception.ClientException;
import com.nym.shortlink.admin.common.convention.exception.ServiceException;
import com.nym.shortlink.admin.common.convention.result.Results;
import com.nym.shortlink.admin.config.UserFlowRiskControlConfiguration;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import static com.nym.shortlink.admin.common.convention.errorcode.BaseErrorCode.FLOW_LIMIT_ERROR;

/**
 * 用户操作流量风控过滤器
 */
@Slf4j
@RequiredArgsConstructor
public class UserFlowRiskControlFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserFlowRiskControlConfiguration userFlowRiskControlConfiguration;

    private static final String USER_FLOW_RISK_CONTROL_LUA_SCRIPT_PATH = "lua/user_flow_risk_control.lua";

    @SneakyThrows
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(USER_FLOW_RISK_CONTROL_LUA_SCRIPT_PATH)));
        redisScript.setResultType(Long.class);
        String username = UserContext.getUsername();
        AnonymousIdentity anonymousIdentity = null;
        String rateLimitIdentity;
        if (username != null && !username.isBlank()) {
            rateLimitIdentity = username;
        } else {
            anonymousIdentity = resolveAnonymousIdentity(httpServletRequest);
            rateLimitIdentity = anonymousIdentity.identity();
        }
        Long result;
        try {
            result = stringRedisTemplate.execute(redisScript, Lists.newArrayList(rateLimitIdentity), userFlowRiskControlConfiguration.getTimeWindow());
        } catch (Throwable ex) {
            if (anonymousIdentity != null) {
                log.error("执行用户请求流量限制LUA脚本出错，identityType={}, identity={}",
                        anonymousIdentity.source(), anonymousIdentity.identity(), ex);
            } else {
                log.error("执行用户请求流量限制LUA脚本出错，username={}", rateLimitIdentity, ex);
            }
            returnJson((HttpServletResponse) response, JSON.toJSONString(Results.failure(new ServiceException("流量风控执行失败"))));
            return;
        }
        if (result == null || result > userFlowRiskControlConfiguration.getMaxAccessCount()) {
            if (anonymousIdentity != null) {
                log.warn("用户流量风控触发，identityType={}, identity={}, currentCount={}, maxAccessCount={}",
                        anonymousIdentity.source(), anonymousIdentity.identity(), result, userFlowRiskControlConfiguration.getMaxAccessCount());
            } else {
                log.warn("用户流量风控触发，username={}, currentCount={}, maxAccessCount={}",
                        rateLimitIdentity, result, userFlowRiskControlConfiguration.getMaxAccessCount());
            }
            returnJson((HttpServletResponse) response, JSON.toJSONString(Results.failure(new ClientException(FLOW_LIMIT_ERROR))));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private AnonymousIdentity resolveAnonymousIdentity(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return new AnonymousIdentity("anonymous:" + forwardedFor.split(",")[0].trim(), "X-Forwarded-For");
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return new AnonymousIdentity("anonymous:" + realIp.trim(), "X-Real-IP");
        }
        return new AnonymousIdentity("anonymous:" + Optional.ofNullable(request.getRemoteAddr()).orElse("unknown"), "remoteAddr");
    }

    private void returnJson(HttpServletResponse response, String json) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.print(json);
        }
    }

    private record AnonymousIdentity(String identity, String source) {
    }
}
