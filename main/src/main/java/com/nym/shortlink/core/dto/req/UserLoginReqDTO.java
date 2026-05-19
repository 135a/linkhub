package com.nym.shortlink.core.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求参数
 */
@Data
public class UserLoginReqDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
