package com.nym.shortlink.core.common.convention.result;

import com.nym.shortlink.core.common.convention.errorcode.BaseErrorCode;
import com.nym.shortlink.core.common.convention.exception.AbstractException;

import java.util.Optional;

/**
 * 全局返回对象构造器
 * 该类用于构建各种类型的响应结果，包括成功响应和失败响应
 */
public final class Results {

    /**
     * 构造成功响应
     * @return 返回一个表示操作成功的Result对象，不包含数据
     */
    public static Result<Void> success() {
        return new Result<Void>()
                .setCode(Result.SUCCESS_CODE);
    }

    /**
     * 构造带返回数据的成功响应
     * @param data 要返回的数据
     * @param <T> 数据类型
     * @return 返回一个包含数据的成功Result对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(Result.SUCCESS_CODE)
                .setData(data);
    }

    /**
     * 构建服务端失败响应
     * @return 返回一个表示服务端错误的Result对象
     */
    public static Result<Void> failure() {
        return new Result<Void>()
                .setCode(BaseErrorCode.SERVICE_ERROR.code())
                .setMessage(BaseErrorCode.SERVICE_ERROR.message());
    }

    /**
     * 通过 {@link AbstractException} 构建失败响应
     * @param abstractException 异常对象，包含错误信息和错误码
     * @return 返回一个包含错误信息的Result对象
     */
    public static Result<Void> failure(AbstractException abstractException) {
        String errorCode = Optional.ofNullable(abstractException.getErrorCode())
                .orElse(BaseErrorCode.SERVICE_ERROR.code());
        String errorMessage = Optional.ofNullable(abstractException.getErrorMessage())
                .orElse(BaseErrorCode.SERVICE_ERROR.message());
        return new Result<Void>()
                .setCode(errorCode)
                .setMessage(errorMessage);
    }

    /**
     * 通过 errorCode、errorMessage 构建失败响应
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @return 返回一个包含错误信息的Result对象
     */
    public static Result<Void> failure(String errorCode, String errorMessage) {
        return new Result<Void>()
                .setCode(errorCode)
                .setMessage(errorMessage);
    }
}
