package com.nym.shortlink.core.common.convention.exception;

import com.nym.shortlink.core.common.convention.errorcode.BaseErrorCode;
import com.nym.shortlink.core.common.convention.errorcode.IErrorCode;

/**
 * 客户端异常
 * 继承自AbstractException，表示客户端侧发生的异常情况
 */
public class ClientException extends AbstractException {

    /**
     * 构造函数1：使用错误码初始化客户端异常
     * @param errorCode 错误码对象，用于表示具体的错误类型
     */
    public ClientException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    /**
     * 构造函数2：使用错误消息初始化客户端异常
     * @param message 异常的详细描述信息
     */
    public ClientException(String message) {
        this(message, null, BaseErrorCode.CLIENT_ERROR);
    }

    /**
     * 构造函数3：使用错误消息和错误码初始化客户端异常
     * @param message 异常的详细描述信息
     * @param errorCode 错误码对象，用于表示具体的错误类型
     */
    public ClientException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    /**
     * 构造函数4：使用错误消息、异常原因和错误码初始化客户端异常

 * 该构造函数允许创建一个包含详细错误信息、根本原因和特定错误码的客户端异常实例
     * @param message 异常的详细描述信息，用于提供具体的错误情况说明
     * @param throwable 导致此异常的异常对象，用于追踪异常的原始原因和调用栈
     * @param errorCode 错误码对象，用于表示具体的错误类型，便于错误分类和处理
     */
    public ClientException(String message, Throwable throwable, IErrorCode errorCode) {
    // 调用父类构造函数，传递错误消息、异常原因和错误码
        super(message, throwable, errorCode);
    }

    /**
     * 重写toString方法，返回格式化的异常信息字符串
     * @return 包含错误码和错误信息的字符串
     */
    @Override
    public String toString() {
        return "ClientException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
