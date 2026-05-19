package com.nym.shortlink.core.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Date;

/**
 * 短链接修改请求对象
 */
@Data
public class ShortLinkUpdateReqDTO {

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 完整短链接
     */
    @NotBlank(message = "完整短链接不能为空")
    private String fullShortUrl;

    /**
     * 原始分组标识
     */
    @NotBlank(message = "原始分组标识不能为空")
    private String originGid;

    /**
     * 分组标识
     */
    @NotBlank(message = "分组标识不能为空")
    private String gid;

    /**
     * 有效期类型 0：永久有效 1：自定义
     */
    private Integer validDateType;

    /**
     * 有效期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;

    /**
     * 描述
     */
    private String describe;
}
