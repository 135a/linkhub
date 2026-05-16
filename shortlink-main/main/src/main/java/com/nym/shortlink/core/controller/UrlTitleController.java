package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.service.UrlTitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * URL 标题控制层
 * 该控制层提供获取网站标题的接口
 */
@RestController(value = "urlTitleControllerByAdmin") // 使用@RestController注解标记为RESTful控制器，value指定bean名称
@RequiredArgsConstructor // 使用Lombok的@RequiredArgsConstructor生成构造器，自动注入final字段
@Slf4j // 使用Lombok的@Slf4j注解生成日志记录器
public class UrlTitleController { // 定义URL标题控制器类

    private final UrlTitleService urlTitleService; // 注入URL标题服务接口

    /**
     * 根据URL获取对应网站的标题
     * @param url 网页链接地址
     * @return 返回包含网站标题的Result对象
     */
    @GetMapping("/api/short-link/admin/v1/title") // 定义GET请求映射，指定请求路径
    public Result<String> getTitleByUrl(@RequestParam("url") String url) { // 定义获取标题方法，接收url参数
        Result<String> result = Results.success(urlTitleService.getTitleByUrl(url)); // 调用服务层获取标题并封装成功结果
        return result; // 返回结果
    }
}
