package com.nym.shortlink.core.common.convention.exception;

import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 用于统一处理应用中抛出的各种异常，返回规范的错误响应
 */
@Slf4j // 使用Lombok的日志注解，提供日志功能
@RestControllerAdvice // Spring MVC的控制器增强注解，用于全局异常处理
public class GlobalExceptionHandler {

    /**
     * 处理 @Valid @RequestBody 校验失败异常
     * 当请求体参数使用@Valid注解校验失败时，此方法会被调用
     * @param ex 方法参数校验异常对象，包含详细的校验错误信息
     * @return 返回包含错误信息的Result对象，其中包含错误字段和错误消息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class) // 指定处理的异常类型
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置HTTP响应状态码为400
    public Result<Map<String, String>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        // 将校验错误信息转换为Map，字段名作为key，错误消息作为value
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(), // 获取字段名
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage() // 获取错误消息
                                : "参数校验失败", // 如果没有默认消息则使用默认值
                        (existing, replacement) -> existing)); // 合并重复键时的处理策略
        log.debug("请求参数校验失败: {}", errors); // 记录调试日志
        // 返回格式化的错误响应
        return new Result<Map<String, String>>()
                .setCode("400") // 设置错误码
                .setMessage("请求参数校验失败") // 设置错误消息
                .setData(errors); // 设置错误数据
    }

    /**
     * 处理 @Validated 单参数校验失败异常
     * 当请求参数使用@Validated注解校验失败时，此方法会被调用
     * @param ex 约束违反异常对象，包含详细的校验错误信息
     * @return 返回包含错误信息的Result对象，其中包含错误消息
     */
    @ExceptionHandler(ConstraintViolationException.class) // 指定处理的异常类型
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置HTTP响应状态码为400
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        // 将校验错误信息合并为一个字符串，格式为"字段名: 错误消息"
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage()) // 格式化每个错误信息
                .collect(Collectors.joining(", ")); // 使用逗号连接所有错误信息
        log.debug("请求参数校验失败: {}", message); // 记录调试日志

        // 返回格式化的错误响应
        return Results.failure("400", message); // 使用Results工具类创建错误响应
    }
}
