package com.nym.shortlink.core.common.convention.result;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全局返回对象
 * 用于统一API接口的返回格式，包含返回码、消息、数据和请求ID等信息
 */
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 5679018624309023727L; // 序列化版本ID，用于对象序列化和反序列化时的版本控制

    /**
     * 正确返回码
     * 当API调用成功时返回的固定值"0"
     */
    public static final String SUCCESS_CODE = "0";

    /**
     * 返回码
     * 用于标识API调用的结果状态，如"0"表示成功，其他值表示不同的错误情况
     */
    private String code;

    /**
     * 返回消息
     * 对返回码的详细说明，帮助调用方理解API调用的结果
     */
    private String message;

    /**
     * 响应数据
     * API调用成功后返回的具体数据，使用泛型T支持不同类型的数据
     */
    private T data;

    /**
     * 请求ID
     * 用于唯一标识一次API请求，便于追踪和调试
     */
    private String requestId;

    /**
     * 判断API调用是否成功
     * 通过比较返回码与成功码来判断API调用是否成功
     * @return 如果返回码等于成功码则返回true，否则返回false
     */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }
}
