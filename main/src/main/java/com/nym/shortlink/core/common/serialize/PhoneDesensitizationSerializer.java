package com.nym.shortlink.core.common.serialize;

import cn.hutool.core.util.DesensitizedUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 手机号脱敏反序列化
 * 该类用于在JSON序列化过程中对手机号进行脱敏处理
 * 继承自JsonSerializer<String>，专门处理字符串类型的手机号数据
 */
public class PhoneDesensitizationSerializer extends JsonSerializer<String> {

    /**
     * 重写serialize方法，实现手机号脱敏逻辑
     * @param phone 原始手机号字符串
     * @param jsonGenerator JSON生成器，用于生成JSON输出
     * @param serializerProvider 序列化提供者，用于获取序列化配置
     * @throws IOException 可能抛出的IO异常
     */
    @Override
    public void serialize(String phone, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        // 调用DesensitizedUtil的mobilePhone方法对手机号进行脱敏处理
        String phoneDesensitization = DesensitizedUtil.mobilePhone(phone);
        // 将脱敏后的手机号写入JSON输出
        jsonGenerator.writeString(phoneDesensitization);
    }
}
