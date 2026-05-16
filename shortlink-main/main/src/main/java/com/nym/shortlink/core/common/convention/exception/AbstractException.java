package com.nym.shortlink.core.common.convention.exception;

import com.nym.shortlink.core.common.convention.errorcode.IErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 抽象项目中两类异常体系，客户端异常和服务端异常
 * 这个类是一个抽象基类，用于定义项目中所有异常的基本结构
 *
 * @see ClientException 客户端异常类
 * @see ServiceException 服务端异常类
 */
@Getter
public abstract class AbstractException extends RuntimeException {

    // 错误代码，用于唯一标识异常类型
    public final String errorCode;

    // 错误消息，用于描述异常的具体信息
    public final String errorMessage;

    /**
     * 构造函数，用于创建异常实例
     * @param message 异常消息，如果为空则使用错误代码对应的消息
     * @param throwable 原始异常，可以为null
     * @param errorCode 错误代码接口，提供错误代码和默认消息
     */
    public AbstractException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable);
        this.errorCode = errorCode.code();
        // 如果提供的消息为空，则使用错误代码中定义的消息
        this.errorMessage = Optional.ofNullable(StringUtils.hasLength(message) ? message : null).orElse(errorCode.message());
    }
}
