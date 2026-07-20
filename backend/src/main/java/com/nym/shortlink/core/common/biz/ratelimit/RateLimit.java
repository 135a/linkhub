package com.nym.shortlink.core.common.biz.ratelimit;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义限流注解，基于 Alibaba Sentinel 实现 QPS 流量控制。
 *
 * <h3>支持的限流算法（{@code controlBehavior}）</h3>
 * <ul>
 *   <li>{@link RuleConstant#CONTROL_BEHAVIOR_DEFAULT}（默认）— 快速失败：超限立即返回 HTTP 429，
 *       适用于读接口、安全敏感接口（登录/注册），响应时间可预期。</li>
 *   <li>{@link RuleConstant#CONTROL_BEHAVIOR_RATE_LIMITER} — 漏桶/匀速排队：超限请求进入等待队列，
 *       按固定速率处理，适用于写接口（创建、批量创建），可平滑流量避免数据库突刺。</li>
 *   <li>{@link RuleConstant#CONTROL_BEHAVIOR_WARM_UP} — 预热/冷启动：系统启动初期从低 QPS 逐渐
 *       升到目标阈值，适合冷数据场景。</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 快速失败（默认）
 * @RateLimit(resource = "redirect_short-link", qps = 100)
 *
 * // 漏桶匀速排队，最长等待 2 秒
 * @RateLimit(resource = "create_short-link", qps = 1,
 *            controlBehavior = RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER,
 *            maxQueueingTimeMs = 2000)
 * }</pre>
 *
 * <p>注意：超过 QPS 阈值且无法排队时，返回 HTTP 429，业务逻辑不会执行。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Sentinel 资源名，全局唯一，建议使用 "模块_操作" 命名规范，如 "create_short-link"。
     */
    String resource();

    /**
     * QPS 阈值（每秒最大请求数），默认 10。
     * 写操作建议设置较低值（1~5），读操作可适当放宽（10~100）。
     */
    double qps() default 10;

    /**
     * 限流控制行为，决定超限后的处理策略，默认快速失败。
     *
     * <ul>
     *   <li>{@link RuleConstant#CONTROL_BEHAVIOR_DEFAULT}（0）— 快速失败（推荐用于读接口、安全接口）</li>
     *   <li>{@link RuleConstant#CONTROL_BEHAVIOR_RATE_LIMITER}（2）— 漏桶匀速排队（推荐用于写接口）</li>
     *   <li>{@link RuleConstant#CONTROL_BEHAVIOR_WARM_UP}（1）— 预热冷启动</li>
     * </ul>
     */
    int controlBehavior() default RuleConstant.CONTROL_BEHAVIOR_DEFAULT;

    /**
     * 漏桶模式下请求在队列中的最大等待时间（毫秒），默认 500ms。
     * 仅当 {@code controlBehavior = CONTROL_BEHAVIOR_RATE_LIMITER} 时生效。
     * 超过此等待时间仍未获得令牌则返回 HTTP 429。
     */
    int maxQueueingTimeMs() default 500;

    /**
     * 限流触发时返回给用户的提示信息，默认为通用提示。
     *
     * <p>可针对不同接口设置语义更清晰的提示，例如：
     * <ul>
     *   <li>创建接口：{@code "创建请求过于频繁，请稍后再试"}</li>
     *   <li>登录接口：{@code "操作过于频繁，请稍后再试"}</li>
     *   <li>跳转接口：{@code "当前网站访问人数过多,请耐心等待"}（默认）</li>
     * </ul>
     */
    String message() default "当前网站访问人数过多,请耐心等待";
}
