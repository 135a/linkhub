package com.nym.shortlink.core.common.convention.exception;

import com.nym.shortlink.core.common.convention.errorcode.BaseErrorCode;
import com.nym.shortlink.core.common.convention.errorcode.IErrorCode;

import java.util.Optional;

/**
 * 服务端异常
 * 继承自AbstractException，用于处理服务层抛出的异常
 */
public class ServiceException extends AbstractException {

    /**
     * 构造一个带有默认错误码的服务端异常
     * @param message 异常信息
     */
    public ServiceException(String message) {
        this(message, null, BaseErrorCode.SERVICE_ERROR);
    }

    /**
     * 构造一个带有指定错误码的服务端异常
     * @param errorCode 错误码接口，用于获取错误码和错误信息
     */
    public ServiceException(IErrorCode errorCode) {
        this(null, errorCode);
    }

    /**
     * 构造一个带有指定错误码和异常信息的服务端异常
     * @param message 异常信息
     * @param errorCode 错误码接口，用于获取错误码和错误信息
     */
    public ServiceException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    /**
     * 构造一个完整的服务端异常，包含异常信息、异常原因和错误码
     * @param message 异常信息
     * @param throwable 异常原因
     * @param errorCode 错误码接口，用于获取错误码和错误信息
     */
    public ServiceException(String message, Throwable throwable, IErrorCode errorCode) {
        super(Optional.ofNullable(message).orElse(errorCode.message()), throwable, errorCode);
    }

    /**
     * 重写toString方法，返回异常的字符串表示
     * @return 包含错误码和错误信息的字符串
     */
    @Override
    public String toString() {
        return "ServiceException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}

