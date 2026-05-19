package com.nym.shortlink.core.common.convention.errorcode;

/**
 * 基础错误码定义
 * 该枚举类实现了IErrorCode接口，用于定义系统中的各种错误码
 * 错误码采用分级结构，一级宏观错误码和二级宏观错误码相结合的方式
 */
public enum BaseErrorCode implements IErrorCode {

    // ========== 一级宏观错误码 客户端错误 ==========
    // 宏观错误码，表示用户端发生的错误
    CLIENT_ERROR("A000001", "用户端错误"),

    // ========== 二级宏观错误码 用户注册错误 ==========
    // 用户注册过程中的错误
    USER_REGISTER_ERROR("A000100", "用户注册错误"),
    // 用户名校验失败
    USER_NAME_VERIFY_ERROR("A000110", "用户名校验失败"),
    // 用户名已存在
    USER_NAME_EXIST_ERROR("A000111", "用户名已存在"),
    // 用户名包含敏感词
    USER_NAME_SENSITIVE_ERROR("A000112", "用户名包含敏感词"),
    // 用户名包含特殊字符
    USER_NAME_SPECIAL_CHARACTER_ERROR("A000113", "用户名包含特殊字符"),
    // 密码校验失败
    PASSWORD_VERIFY_ERROR("A000120", "密码校验失败"),
    // 密码长度不够
    PASSWORD_SHORT_ERROR("A000121", "密码长度不够"),
    // 手机格式校验失败
    PHONE_VERIFY_ERROR("A000151", "手机格式校验失败"),

    // ========== 二级宏观错误码 系统请求缺少幂等Token ==========
    // 幂等Token为空
    IDEMPOTENT_TOKEN_NULL_ERROR("A000200", "幂等Token为空"),
    // 幂等Token已被使用或失效
    IDEMPOTENT_TOKEN_DELETE_ERROR("A000201", "幂等Token已被使用或失效"),

    // ========== 一级宏观错误码 系统执行出错 ==========
    // 系统执行过程中发生的错误
    SERVICE_ERROR("B000001", "系统执行出错"),
    // ========== 二级宏观错误码 系统执行超时 ==========
    SERVICE_TIMEOUT_ERROR("B000100", "系统执行超时"),

    // ========== 流量限制错误 ==========
    FLOW_LIMIT_ERROR("A000300", "当前访问网站人数过多，请稍后再试");

    private final String code;

    private final String message;

    BaseErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
