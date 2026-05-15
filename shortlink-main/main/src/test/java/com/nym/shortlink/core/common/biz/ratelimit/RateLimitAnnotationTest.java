package com.nym.shortlink.core.common.biz.ratelimit;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimit - 自定义限流注解")
class RateLimitAnnotationTest {

    @RateLimit(resource = "test_fast_fail", qps = 100)
    void fastFailEndpoint() {}

    @RateLimit(resource = "test_rate_limiter", qps = 5,
            controlBehavior = RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER,
            maxQueueingTimeMs = 2000)
    void rateLimiterEndpoint() {}

    @RateLimit(resource = "test_warm_up", qps = 50,
            controlBehavior = RuleConstant.CONTROL_BEHAVIOR_WARM_UP,
            message = "预热中请稍后")
    void warmUpEndpoint() {}

    @Test
    @DisplayName("默认值：快速失败模式，QPS=10，等待=500ms")
    void defaultValuesAreCorrect() throws Exception {
        Method method = getClass().getDeclaredMethod("fastFailEndpoint");
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        assertEquals("test_fast_fail", annotation.resource());
        assertEquals(100.0, annotation.qps(), 0.001);
        assertEquals(RuleConstant.CONTROL_BEHAVIOR_DEFAULT, annotation.controlBehavior());
        assertEquals(500, annotation.maxQueueingTimeMs());
        assertEquals("当前网站访问人数过多,请耐心等待", annotation.message());
    }

    @Test
    @DisplayName("漏桶模式配置正确")
    void rateLimiterConfigIsCorrect() throws Exception {
        Method method = getClass().getDeclaredMethod("rateLimiterEndpoint");
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        assertEquals(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER, annotation.controlBehavior());
        assertEquals(5.0, annotation.qps(), 0.001);
        assertEquals(2000, annotation.maxQueueingTimeMs());
    }

    @Test
    @DisplayName("预热模式配置正确")
    void warmUpConfigIsCorrect() throws Exception {
        Method method = getClass().getDeclaredMethod("warmUpEndpoint");
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        assertEquals(RuleConstant.CONTROL_BEHAVIOR_WARM_UP, annotation.controlBehavior());
        assertEquals("预热中请稍后" , annotation.message());
    }

    @Test
    @DisplayName("不同接口可设置不同 QPS")
    void differentApisHaveDifferentQps() throws Exception {
        Method fastFail = getClass().getDeclaredMethod("fastFailEndpoint");
        Method rateLimiter = getClass().getDeclaredMethod("rateLimiterEndpoint");

        double qps1 = fastFail.getAnnotation(RateLimit.class).qps();
        double qps2 = rateLimiter.getAnnotation(RateLimit.class).qps();

        assertNotEquals(qps1, qps2, "不同接口的 QPS 阈值应不同");
    }
}
