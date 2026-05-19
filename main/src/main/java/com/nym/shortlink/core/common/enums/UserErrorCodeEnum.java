package com.nym.shortlink.core.common.enums;

import com.nym.shortlink.core.common.convention.errorcode.IErrorCode;

/**
 * 用户错误码枚举类
 * 实现了IErrorCode接口，用于定义用户相关的错误码及其对应描述信息
 */
public enum UserErrorCodeEnum implements IErrorCode {

    /**
     * 用户记录不存在的错误码
     * 错误码：B000200
     * 描述：用户记录不存在
     */
    USER_NULL("B000200", "用户记录不存在"),

    /**
     * 用户名已存在的错误码
     * 错误码：B000201
     * 描述：用户名已存在
     */
    USER_NAME_EXIST("B000201", "用户名已存在"),

    /**
     * 用户记录已存在的错误码
     * 错误码：B000202
     * 描述：用户记录已存在
     */
    USER_EXIST("B000202", "用户记录已存在"),

    /**
     * 用户记录新增失败错误码
     * 错误码：B000203
     * 描述：用户记录新增失败
     */
    USER_SAVE_ERROR("B000203", "用户记录新增失败");

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误信息描述
     */
    private final String message;

    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误信息描述
     */
    UserErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码
     * @return 错误码字符串
     */
    @Override
    public String code() {
        return code;
    }

    /**
     * 获取错误信息描述
     * @return 错误信息描述字符串
     */
    @Override
    public String message() {
        return message;
    }
}
