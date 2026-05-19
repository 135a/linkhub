package com.nym.shortlink.core.common.web;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.nym.shortlink.core.common.convention.errorcode.BaseErrorCode;
import com.nym.shortlink.core.common.convention.exception.AbstractException;
import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理器
 * 用于统一处理应用中抛出的各种异常，提供统一的异常响应格式
 */
@Component
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 拦截参数验证异常
     * 当请求参数验证失败时，会触发此方法处理
     * @param request HTTP请求对象
     * @param ex 方法参数验证异常对象
     * @return 统一的响应结果对象
     */
    @SneakyThrows
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result validExceptionHandler(HttpServletRequest request, MethodArgumentNotValidException ex) {
        // 获取验证结果中的第一个字段错误
        BindingResult bindingResult = ex.getBindingResult();
        FieldError firstFieldError = CollectionUtil.getFirst(bindingResult.getFieldErrors());
        // 获取错误信息，如果没有则使用空字符串
        String exceptionStr = Optional.ofNullable(firstFieldError)
                .map(FieldError::getDefaultMessage)
                .orElse(StrUtil.EMPTY);
        // 记录错误日志
        log.error("[{}] {} [ex] {}", request.getMethod(), getUrl(request), exceptionStr);
        // 返回客户端错误响应
        return Results.failure(BaseErrorCode.CLIENT_ERROR.code(), exceptionStr);
    }

    /**
     * 拦截应用内抛出的异常
     */
    @ExceptionHandler(value = {AbstractException.class})
    public Result abstractException(HttpServletRequest request, AbstractException ex) {
        if (ex.getCause() != null) {
            log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex.toString(), ex.getCause());
            return Results.failure(ex);
        }
        log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex.toString());
        return Results.failure(ex);
    }

    /**
     * 处理 @Validated 单参数校验失败异常
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    public Result constraintViolationExceptionHandler(HttpServletRequest request, ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        log.error("[{}] {} [ex] {}", request.getMethod(), getUrl(request), message);
        return Results.failure(BaseErrorCode.CLIENT_ERROR.code(), message);
    }

    /**
     * 拦截未捕获异常
     */
    @ExceptionHandler(value = Throwable.class)
    public Result defaultErrorHandler(HttpServletRequest request, Throwable throwable) {
        log.error("[{}] {} ", request.getMethod(), getUrl(request), throwable);
        // 注意，此处是为了聚合模式添加的代码，正常不需要该判断
        if (throwable.getClass().getSuperclass() != null && Objects.equals(throwable.getClass().getSuperclass().getSimpleName(), AbstractException.class.getSimpleName())) {
            String errorCode = ReflectUtil.getFieldValue(throwable, "errorCode").toString();
            String errorMessage = ReflectUtil.getFieldValue(throwable, "errorMessage").toString();
            return Results.failure(errorCode, errorMessage);
        }
        return Results.failure();
    }

    private String getUrl(HttpServletRequest request) {
        if (StringUtils.isEmpty(request.getQueryString())) {
            return request.getRequestURL().toString();
        }
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }
}
