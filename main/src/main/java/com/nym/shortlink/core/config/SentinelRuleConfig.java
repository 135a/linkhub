package com.nym.shortlink.core.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 初始化限流配置（已废弃）
 *
 * @deprecated 限流规则已迁移至 {@code RateLimitAspect}，通过 {@code @RateLimit} 注解动态注册。
 *             本类保留仅供历史参考，@Component 已移除，不再注入 Spring 容器。
 */
@Deprecated
// @Component  -- 已废弃，限流规则由 RateLimitAspect 通过 @RateLimit 注解动态加载
public class SentinelRuleConfig implements InitializingBean {

    /**
     * Spring Bean 初始化完成后执行，配置 Sentinel 限流规则
     * 在应用启动时自动加载限流规则到 Sentinel 规则管理器
     *
     * @throws Exception 初始化异常
     * @deprecated 已由 RateLimitAspect 替代
     */
    @Deprecated
    @Override
    public void afterPropertiesSet() throws Exception {
        // 创建短链接接口的 QPS 限流规则（每秒最多 1 次请求）
        List<FlowRule> rules = new ArrayList<>();
        FlowRule createOrderRule = new FlowRule();
        createOrderRule.setResource("create_short-link");
        createOrderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        createOrderRule.setCount(1);
        rules.add(createOrderRule);
        FlowRuleManager.loadRules(rules);
    }
}
