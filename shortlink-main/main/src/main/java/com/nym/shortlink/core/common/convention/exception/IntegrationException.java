package com.nym.shortlink.core.common.convention.exception;

import com.nym.shortlink.core.common.convention.errorcode.BaseErrorCode;
import com.nym.shortlink.core.common.convention.errorcode.IErrorCode;

/**
 * 远程服务调用异常
 */
public class IntegrationException extends AbstractException {

    public IntegrationException(String message) {
        this(message, null, BaseErrorCode.REMOTE_ERROR);
    }

    public IntegrationException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public IntegrationException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }

    @Override
    public String toString() {
        return "IntegrationException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
