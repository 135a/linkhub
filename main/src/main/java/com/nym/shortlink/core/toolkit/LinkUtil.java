package com.nym.shortlink.core.toolkit;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.util.Date;
import java.util.Optional;

import static com.nym.shortlink.core.common.constant.ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;

/**
 * 短链接工具类
 * 提供与短链接相关的实用方法，包括IP获取、设备信息识别、域名提取等功能
 */
public class LinkUtil {

    /**
     * 获取短链接缓存有效期时间
     * 计算从当前时间到指定有效期的毫秒数，如果有效期无效则返回默认缓存时间
     *
     * @param validDate 有效期时间
     * @return 有限期时间戳
     */
    public static long getLinkCacheValidTime(Date validDate) {
        return Optional.ofNullable(validDate)
                .map(each -> DateUtil.between(new Date(), each, DateUnit.MS))
                .orElse(DEFAULT_CACHE_VALID_TIME);
    }

    /**
     * 获取用户真实IP
     *
     * @param request 请求
     * @return 用户真实IP
     */
    public static String getActualIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // 多层 Docker 代理时，X-Forwarded-For 为 "真实IP, 网关IP1, 网关IP2" 逗号列表
        // 内层 Nginx 的 X-Real-IP: $remote_addr 会被覆写为内网网关 IP，不可信
        // 策略：遍历列表，跳过私有/保留网段，取第一个公网 IP；若全为内网则取最前一个兜底
        if (ipAddress != null && ipAddress.contains(",")) {
            String[] ips = ipAddress.split(",");
            String fallback = ips[0].trim();
            for (String ip : ips) {
                String candidate = ip.trim();
                if (!isPrivateIp(candidate)) {
                    return candidate;
                }
            }
            return fallback;
        }
        return ipAddress;
    }

    /**
     * 判断是否为私有/保留 IP 段（Docker 内网、局域网、回环等）
     */
    private static boolean isPrivateIp(String ip) {
        if (ip == null || ip.isEmpty())
            return true;
        // 回环
        if (ip.startsWith("127."))
            return true;
        // 10.x.x.x
        if (ip.startsWith("10."))
            return true;
        // 172.16.x.x ~ 172.31.x.x（Docker 默认网段 172.17-172.18 在此范围内）
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                if (second >= 16 && second <= 31)
                    return true;
            } catch (Exception ignored) {
            }
        }
        // 192.168.x.x
        if (ip.startsWith("192.168."))
            return true;
        // IPv6 回环/本地
        if ("::1".equals(ip) || ip.startsWith("fe80:"))
            return true;
        return false;
    }

    /**
     * 获取用户访问操作系统
     *
     * @param request 请求
     * @return 访问操作系统
     */
    public static String getOs(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("windows")) {
            return "Windows";
        } else if (userAgent.toLowerCase().contains("mac")) {
            return "Mac OS";
        } else if (userAgent.toLowerCase().contains("linux")) {
            return "Linux";
        } else if (userAgent.toLowerCase().contains("android")) {
            return "Android";
        } else if (userAgent.toLowerCase().contains("iphone") || userAgent.toLowerCase().contains("ipad")) {
            return "iOS";
        } else {
            return "Unknown";
        }
    }

    /**
     * 获取用户访问浏览器
     *
     * @param request 请求
     * @return 访问浏览器
     */
    public static String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("edg")) {
            return "Microsoft Edge";
        } else if (userAgent.toLowerCase().contains("chrome")) {
            return "Google Chrome";
        } else if (userAgent.toLowerCase().contains("firefox")) {
            return "Mozilla Firefox";
        } else if (userAgent.toLowerCase().contains("safari")) {
            return "Apple Safari";
        } else if (userAgent.toLowerCase().contains("opera")) {
            return "Opera";
        } else if (userAgent.toLowerCase().contains("msie") || userAgent.toLowerCase().contains("trident")) {
            return "Internet Explorer";
        } else {
            return "Unknown";
        }
    }

    /**
     * 获取用户访问设备
     *
     * @param request 请求
     * @return 访问设备
     */
    public static String getDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("mobile")) {
            return "Mobile";
        }
        return "PC";
    }

    /**
     * 获取用户访问网络
     *
     * @param request 请求
     * @return 访问设备
     */
    public static String getNetwork(HttpServletRequest request) {
        String actualIp = getActualIp(request);
        // 这里简单判断IP地址范围，您可能需要更复杂的逻辑
        // 例如，通过调用IP地址库或调用第三方服务来判断网络类型
        return actualIp.startsWith("192.168.") || actualIp.startsWith("10.") ? "WIFI" : "Mobile";
    }

    /**
     * 获取原始链接中的域名
     * 如果原始链接包含 www 开头的话需要去掉
     *
     * @param url 创建或者修改短链接的原始链接
     * @return 原始链接中的域名
     */
    public static String extractDomain(String url) {
        String domain = null;
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (StrUtil.isNotBlank(host)) {
                domain = host;
                if (domain.startsWith("www.")) {
                    domain = host.substring(4);
                }
            }
        } catch (Exception ignored) {
        }
        return domain;
    }
}
