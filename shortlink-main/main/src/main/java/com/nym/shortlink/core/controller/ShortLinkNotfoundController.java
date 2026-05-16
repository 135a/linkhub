package com.nym.shortlink.core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 短链接不存在跳转控制器
 * 该控制器用于处理短链接不存在时的跳转逻辑
 */
@Controller
public class ShortLinkNotfoundController {

    /**
     * 短链接不存在跳转页面
     * 当访问的短链接不存在时，系统将调用此方法进行页面跳转
     * @return 返回"notfound"视图名称，由视图解析器解析为具体的页面
     */
    @RequestMapping("/page/notfound")
    public String notfound() {
        return "notfound";
    }
}
