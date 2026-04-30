package com.nym.shortlink.core.common.biz.user;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.nym.shortlink.core.common.convention.exception.ClientException;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.config.UserFlowRiskControlConfiguration;
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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import static com.nym.shortlink.core.common.convention.errorcode.BaseErrorCode.FLOW_LIMIT_ERROR;

/**
 * 用户操作流量风控过滤器
 */
@Slf4j
@RequiredArgsConstructor
public class UserFlowRiskControlFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserFlowRiskControlConfiguration userFlowRiskControlConfiguration;

    private static final String USER_FLOW_RISK_CONTROL_LUA_SCRIPT_PATH = "lua/user_flow_risk_control.lua";

    /**
     * 性能优化：将 Lua 脚本对象提升为静态常量，类加载时初始化一次。
     * 原实现每次请求都 new DefaultRedisScript + ResourceScriptSource，
     * 导致每次请求重新读取 classpath 文件并解析脚本，是隐藏的 I/O 瓶颈。
     */
    private static final DefaultRedisScript<Long> FLOW_LIMIT_SCRIPT;
    static {
        FLOW_LIMIT_SCRIPT = new DefaultRedisScript<>();
        FLOW_LIMIT_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource(USER_FLOW_RISK_CONTROL_LUA_SCRIPT_PATH)));
        FLOW_LIMIT_SCRIPT.setResultType(Long.class);
    }

    // AntPathMatcher 是线程安全的无状态对象，静态复用避免每次请求 new
    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    @SneakyThrows
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String requestURI = httpServletRequest.getRequestURI();
        String exclusions = userFlowRiskControlConfiguration.getExclusions();
        if (StringUtils.hasText(exclusions)) {
            String[] exclusionArray = exclusions.split(",");
            for (String exclusion : exclusionArray) {
                if (ANT_PATH_MATCHER.match(exclusion, requestURI)) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }
        String username = Optional.ofNullable(UserContext.getUsername())
                .orElse(((jakarta.servlet.http.HttpServletRequest) request).getHeader("username"));
        if (!StringUtils.hasText(username)) {
            username = "other";
        }
        Long result;
        try {
            result = stringRedisTemplate.execute(FLOW_LIMIT_SCRIPT, Lists.newArrayList(username), userFlowRiskControlConfiguration.getTimeWindow());
        } catch (Throwable ex) {
            log.error("执行用户请求流量限制LUA脚本出错", ex);
            returnJson((HttpServletResponse) response, JSON.toJSONString(Results.failure(new ClientException(FLOW_LIMIT_ERROR))));
            return;
        }
        if (result == null || result > userFlowRiskControlConfiguration.getMaxAccessCount()) {
            log.info("用户请求流量限制，用户名：{},请求次数： {} ", username,result);
            returnJson((HttpServletResponse) response, JSON.toJSONString(Results.failure(new ClientException(FLOW_LIMIT_ERROR))));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void returnJson(HttpServletResponse response, String json) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.print(json);
        }
    }
}
