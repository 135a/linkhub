package com.nym.shortlink.trace.aspect;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.nym.shortlink.trace.annotation.NoLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志切面
 */
@Aspect
@Slf4j
public class LogAspect {

    private static final List<String> SENSITIVE_FIELDS = Arrays.asList("password", "token", "secret", "oldPassword", "newPassword");

    /**
     * 敏感字段脱敏过滤器
     */
    private static final ValueFilter SENSITIVE_FILTER = (object, name, value) -> {
        if (value == null) {
            return null;
        }
        if (SENSITIVE_FIELDS.stream().anyMatch(field -> StrUtil.equalsIgnoreCase(field, name))) {
            return "******";
        }
        return value;
    };

    @Pointcut("execution(* com.nym.shortlink..controller..*.*(..)) || execution(* com.nym.shortlink..service..*.*(..))")
    public void logPointcut() {
    }

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 检查是否有 @NoLog 注解
        if (method.isAnnotationPresent(NoLog.class) || method.getDeclaringClass().isAnnotationPresent(NoLog.class)) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = method.getName();

        // 打印入参日志 (DEBUG)
        if (log.isDebugEnabled()) {
            Object[] args = joinPoint.getArgs();
            String params = buildParams(args);
            log.debug("[START] {}.{} | Params: {}", className, methodName, params);
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            log.error("[ERROR] {}.{} | Exception: {}", className, methodName, e.getMessage(), e);
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            // 打印出参和耗时日志 (DEBUG)
            if (log.isDebugEnabled()) {
                String resultStr = serializeWithMasking(result);
                log.debug("[END] {}.{} | Duration: {}ms | Result: {}", className, methodName, duration, resultStr);
            }
        }
    }

    private String buildParams(Object[] args) {
        if (ArrayUtil.isEmpty(args)) {
            return "[]";
        }
        return Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest) && !(arg instanceof HttpServletResponse) && !(arg instanceof MultipartFile))
                .map(this::serializeWithMasking)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String serializeWithMasking(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return JSON.toJSONString(obj, SENSITIVE_FILTER, JSONWriter.Feature.WriteNulls);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
