package com.nym.shortlink.core.common.convention.errorcode;

/**
 * 平台错误码接口
 * 该接口定义了错误码的基本结构，包含错误码和错误信息两个方法
 */
public interface IErrorCode {

    /**
     * 错误码
     */
    String code();

    /**
     * 错误信息
     */
    String message();
}
