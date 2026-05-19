package com.nym.shortlink.core.common.biz.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * ApiExecutionTimeAspect类是一个切面组件，用于记录和控制器的执行时间
 * 使用@Aspect注解标记这是一个切面类
 * 使用@Component注解标记这是一个Spring组件
 * 使用@Slf4j注解提供日志记录功能
 */
@Aspect
@Component
@Slf4j
public class ApiExecutionTimeAspect {

    /**
     * 定义一个切入点，匹配com.nym.shortlink.core.controller包下的所有类的所有方法
     * @Pointcut注解用于定义切入点表达式
     */
    @Pointcut("execution(* com.nym.shortlink.core.controller.*.*(..))")
    public void controllerMethods() {}

    /**
     * 环绕通知，用于在控制器方法执行前后记录日志和计算执行时间
     * @Around注解表示这是一个环绕通知
     * @param joinPoint 连接点，可以获取被拦截方法的信息
     * @return 返回被拦截方法的执行结果
     * @throws Throwable 方法执行过程中可能抛出的异常
     */
    @Around("controllerMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录方法开始执行的时间
        long startTime = System.currentTimeMillis();
        // 获取被拦截方法的类名和方法名
        String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();
        
        // 获取当前请求的属性，用于获取HTTP请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            // 获取HTTP请求对象
            HttpServletRequest request = attributes.getRequest();
            // 记录请求开始的信息，包括请求方法和请求URI
            log.info("Started request: {} {}", request.getMethod(), request.getRequestURI());
        }
        // 记录进入方法的信息
        log.info("Entering method: {}", methodName);
        
        try {
            // 执行被拦截的方法
            Object result = joinPoint.proceed();
            // 计算方法执行耗时
            long elapsedTime = System.currentTimeMillis() - startTime;
            // 记录方法正常结束的信息和执行时间
            log.info("Exiting method: {} - execution time: {} ms", methodName, elapsedTime);
            return result;
        } catch (Throwable e) {
            // 计算方法执行耗时（异常情况下）
            long elapsedTime = System.currentTimeMillis() - startTime;
            // 记录方法异常结束的信息和执行时间
            log.info("Exception in method: {} - execution time: {} ms", methodName, elapsedTime);
            // 抛出异常
            throw e;
        }
    }
}
