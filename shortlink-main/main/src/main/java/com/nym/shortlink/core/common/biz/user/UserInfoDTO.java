package com.nym.shortlink.core.common.biz.user;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息实体
 * 使用@Data注解自动生成getter、setter等方法
 * 使用@NoArgsConstructor注解生成无参构造方法
 * 使用@AllArgsConstructor注解生成全参构造方法
 * 使用@Builder注解支持Builder模式创建对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {

    /**
     * 用户 ID
     * 使用@JSONField注解指定JSON序列化时的字段名
     */
    @JSONField(name = "id")
    private String userId;

    /**
     * 用户名
     * 用于登录的唯一标识
     */
    private String username;

    /**
     * 真实姓名
     * 用户的真实姓名信息
     */
    private String realName;

    /**
     * 用户Token
     * 用于验证用户身份的令牌
     */
    private String token;
}
