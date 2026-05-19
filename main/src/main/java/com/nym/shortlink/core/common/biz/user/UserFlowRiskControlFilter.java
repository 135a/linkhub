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
 * 用于限制用户在一定时间窗口内的请求次数，防止恶意请求或系统过载
 */
@Slf4j
@RequiredArgsConstructor
public class UserFlowRiskControlFilter implements Filter {

    // Redis模板，用于与Redis交互
    private final StringRedisTemplate stringRedisTemplate;
    // 用户流量风控配置，包含时间窗口、最大访问次数等参数
    private final UserFlowRiskControlConfiguration userFlowRiskControlConfiguration;

    // Lua脚本文件路径，用于实现Redis中的流量限制逻辑
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

    /**
     * 过滤器核心处理方法
     * @param request 请求对象
     * @param response 响应对象
     * @param filterChain 过滤器链
     * @throws IOException 可能抛出的IO异常
     * @throws ServletException 可能抛出的Servlet异常
     */
    @SneakyThrows
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        // 将请求转换为HttpServletRequest对象
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        // 获取请求URI
        String requestURI = httpServletRequest.getRequestURI();
        // 获取排除配置的URI列表
        String exclusions = userFlowRiskControlConfiguration.getExclusions();
        // 检查请求是否在排除列表中
        if (StringUtils.hasText(exclusions)) {
            String[] exclusionArray = exclusions.split(",");
            for (String exclusion : exclusionArray) {
                // 使用AntPathMatcher进行路径匹配
                if (ANT_PATH_MATCHER.match(exclusion, requestURI)) {
                    // 如果匹配到排除路径，直接放行
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }
        // 获取用户名，优先从上下文中获取，其次从请求头获取
        String username = Optional.ofNullable(UserContext.getUsername())
                .orElse(((jakarta.servlet.http.HttpServletRequest) request).getHeader("username"));
        // 如果用户名为空，使用"other"作为默认值
        if (!StringUtils.hasText(username)) {
            username = "other";
        }
        Long result;
        try {
            // 执行Lua脚本进行流量限制检查
            result = stringRedisTemplate.execute(FLOW_LIMIT_SCRIPT, Lists.newArrayList(username), userFlowRiskControlConfiguration.getTimeWindow());
        } catch (Throwable ex) {
            // 记录执行Lua脚本时的错误
            log.error("执行用户请求流量限制LUA脚本出错", ex);
            // 返回错误响应
            returnJson((HttpServletResponse) response, JSON.toJSONString(Results.failure(new ClientException(FLOW_LIMIT_ERROR))));
            return;
        }
        // 检查是否超过最大访问次数
        if (result == null || result > userFlowRiskControlConfiguration.getMaxAccessCount()) {
            // 记录流量限制日志
            log.info("用户请求流量限制，用户名：{},请求次数： {} ", username,result);
            // 返回错误响应
            returnJson((HttpServletResponse) response, JSON.toJSONString(Results.failure(new ClientException(FLOW_LIMIT_ERROR))));
            return;
        }
        // 如果未超过限制，继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 返回JSON格式的响应
     * @param response HTTP响应对象
     * @param json 要返回的JSON字符串
     * @throws Exception 可能抛出的异常
     */
    private void returnJson(HttpServletResponse response, String json) throws Exception {
        // 设置HTTP状态码为429 (Too Many Requests)
        response.setStatus(429);
        // 设置字符编码为UTF-8
        response.setCharacterEncoding("UTF-8");
        // 设置内容类型为JSON
        response.setContentType("application/json; charset=utf-8");
        // 使用try-with-resources确保Writer正确关闭
        try (PrintWriter writer = response.getWriter()) {
            writer.print(json);
        }
    }
}
