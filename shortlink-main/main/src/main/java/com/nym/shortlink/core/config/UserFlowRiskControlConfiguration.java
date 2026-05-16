package com.nym.shortlink.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 用户操作流量风控配置文件
 * 该类用于管理用户流量风控的相关配置，通过Spring Boot的@ConfigurationProperties注解
 * 将配置文件中以"short-link.flow-limit"为前缀的属性映射到该类的字段中
 */
@Data  // Lombok注解，自动生成getter、setter等方法
@Component  // Spring注解，将该类标记为Spring容器中的Bean
@ConfigurationProperties(prefix = "short-link.flow-limit")  // 绑定配置文件中指定前缀的属性
public class UserFlowRiskControlConfiguration {

    /**
     * 是否开启用户流量风控验证
     * 当设置为true时，系统将启用用户流量风控功能
     * 当设置为false时，系统将禁用用户流量风控功能
     */
    private Boolean enable;

    /**
     * 流量风控时间窗口，单位：秒
     * 用于指定流量统计的时间范围，例如设置为"60"表示统计最近60秒内的访问次数
     * 可以是具体的秒数，也可以是表达式如"1h"表示1小时
     */
    private String timeWindow;

    /**
     * 流量风控时间窗口内可访问次数
     * 指定在timeWindow设置的时间范围内，用户允许的最大访问次数
     * 超过此限制将被视为流量异常，可能触发相应的风控措施
     */
    private Long maxAccessCount;

    /**
     * 流量风控忽略路径
     * 指定哪些路径的访问不计入流量统计
     * 可以是单个路径，也可以是多个路径用逗号分隔
     * 例如"/api/health,/api/status"表示健康检查和状态接口的访问不计入流量统计
     */
    private String exclusions;
}
