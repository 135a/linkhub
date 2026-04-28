package com.nym.shortlink.core.common.biz.ratelimit;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流 AOP 切面，负责：
 * <ol>
 *   <li>首次调用时向 Sentinel 动态注册 {@link FlowRule}（追加而非覆盖）</li>
 *   <li>通过 {@link SphU#entry} 执行资源埋点</li>
 *   <li>捕获 {@link BlockException}，向客户端返回 HTTP 429</li>
 * </ol>
 *
 * <p>只拦截标注了 {@link RateLimit} 注解的方法。
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    /**
     * 已注册到 Sentinel 的资源名缓存，防止重复注册导致规则被覆盖。
     * key: 资源名, value: 固定 true
     */
    private final Map<String, Boolean> registeredResources = new ConcurrentHashMap<>();

    /**
     * 限流触发时的统一错误码
     */
    private static final String RATE_LIMIT_CODE = "RATE_LIMIT_429";

    /**
     * 拦截所有带 {@link RateLimit} 注解的方法，执行限流逻辑。
     *
     * @param joinPoint 方法切入点
     * @param rateLimit 方法上的限流注解
     * @return 原方法返回值；若限流触发则写入 HTTP 429 响应后返回 null
     * @throws Throwable 原方法抛出的异常
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String resource = rateLimit.resource();
        double qps = rateLimit.qps();
        int controlBehavior = rateLimit.controlBehavior();
        int maxQueueingTimeMs = rateLimit.maxQueueingTimeMs();
        String message = rateLimit.message();

        // 首次调用时向 Sentinel 追加注册 FlowRule
        registerRuleIfAbsent(resource, qps, controlBehavior, maxQueueingTimeMs);

        Entry entry = null;
        try {
            // Sentinel 资源埋点：申请令牌
            entry = SphU.entry(resource);
            // 未被限流，正常执行业务方法
            return joinPoint.proceed();
        } catch (BlockException e) {
            // 触发限流：写入 HTTP 429 响应（携带接口自定义提示）
            log.warn("[RateLimitAspect] 接口限流触发，resource={}, qps={}, message={}", resource, qps, message);
            handleBlockException(message);
            return null;
        } finally {
            // 无论是否限流，都必须退出资源（否则 Sentinel 统计数据异常）
            if (entry != null) {
                entry.exit();
            }
        }
    }

    /**
     * 若资源尚未注册，则向 Sentinel 追加 QPS 限流规则。
     * 使用 {@link ConcurrentHashMap#putIfAbsent} 保证并发安全，避免重复注册。
     *
     * @param resource           资源名
     * @param qps                QPS 阈值
     * @param controlBehavior    限流控制行为（快速失败/漏桶/预热）
     * @param maxQueueingTimeMs  漏桶模式下最大排队等待时间（毫秒）
     */
    private void registerRuleIfAbsent(String resource, double qps, int controlBehavior, int maxQueueingTimeMs) {
        if (registeredResources.putIfAbsent(resource, Boolean.TRUE) == null) {
            // 获取现有规则列表（避免覆盖其他资源的规则）
            List<FlowRule> existingRules = new ArrayList<>(FlowRuleManager.getRules());

            // 构建新规则
            FlowRule rule = new FlowRule();
            rule.setResource(resource);
            rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            rule.setCount(qps);
            // 设置限流控制策略（快速失败 / 漏桶匀速排队 / 预热）
            rule.setControlBehavior(controlBehavior);
            // 漏桶模式下设置最大排队等待时间，其他模式忽略此参数
            if (controlBehavior == RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER) {
                rule.setMaxQueueingTimeMs(maxQueueingTimeMs);
            }

            existingRules.add(rule);
            FlowRuleManager.loadRules(existingRules);

            log.info("[RateLimitAspect] 注册限流规则：resource={}, qps={}, controlBehavior={}, maxQueueingTimeMs={}",
                    resource, qps, controlBehavior, maxQueueingTimeMs);
        }
    }

    /**
     * 限流触发时，通过 {@link HttpServletResponse} 写入 HTTP 429 响应。
     * 兼容普通接口和 void 接口（批量创建等）。
     *
     * @param message 接口自定义的限流提示信息（来自 {@link RateLimit#message()}）
     */
    private void handleBlockException(String message) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.error("[RateLimitAspect] 无法获取 HttpServletResponse，RequestContextHolder 为空");
            return;
        }

        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            log.error("[RateLimitAspect] HttpServletResponse 为 null");
            return;
        }

        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");

        // 构造统一响应体，使用接口自定义提示信息
        RateLimitResponse body = new RateLimitResponse(RATE_LIMIT_CODE, message);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(JSON.toJSONString(body));
            writer.flush();
        } catch (Exception ex) {
            log.error("[RateLimitAspect] 写入限流响应失败", ex);
        }
    }

    /**
     * 限流响应体结构，与项目 Result 格式保持一致。
     */
    record RateLimitResponse(String code, String message) {
        @SuppressWarnings("unused")
        public Object getData() {
            return null;
        }
    }
}
