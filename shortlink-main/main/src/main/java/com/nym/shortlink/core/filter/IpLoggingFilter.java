package com.nym.shortlink.core.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 全局 IP 诊断过滤器
 * 每次请求都打印完整 IP 链路，方便排查多层 Docker 代理下真实 IP 丢失问题
 */
@Slf4j
@Component
@Order(1)
public class IpLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 检查请求是否为 HttpServletRequest 类型
        if (request instanceof HttpServletRequest httpReq) {
            // 获取请求 URI
            String uri = httpReq.getRequestURI();
            // 判断是否为静态资源，如果不是则记录 IP 信息
            if (!isStaticResource(uri)) {

                // 获取各种可能的 IP 头信息
                String xff        = httpReq.getHeader("X-Forwarded-For");    // X-Forwarded-For 头信息
                String xRealIp    = httpReq.getHeader("X-Real-IP");          // X-Real-IP 头信息
                String remoteAddr = request.getRemoteAddr();                 // 客户端远程地址
                String resolved   = resolveRealIp(xff, xRealIp, remoteAddr); // 解析出的真实 IP

                // 记录 IP 相关信息到日志
                log.info("[IP-LOG] {} {} | resolved={} | X-Forwarded-For={} | X-Real-IP={} | remoteAddr={}",
                        httpReq.getMethod(), uri,    // HTTP 方法和请求 URI
                        resolved,                    // 解析出的真实 IP
                        xff       != null ? xff       : "(null)",  // X-Forwarded-For 值
                        xRealIp   != null ? xRealIp   : "(null)",  // X-Real-IP 值
                        remoteAddr);                             // 远程地址
            }
        }
        // 继续过滤器链
        chain.doFilter(request, response);
    }

    /**
     * 解析真实 IP 地址
     * @param xff X-Forwarded-For 头信息
     * @param xRealIp X-Real-IP 头信息
     * @param remoteAddr 远程地址
     * @return 解析出的真实 IP 地址
     */
    private String resolveRealIp(String xff, String xRealIp, String remoteAddr) {
        // 优先使用 X-Forwarded-For，并过滤掉私有 IP
        if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
            for (String part : xff.split(",")) {
                String ip = part.trim();
                // 返回第一个非私有 IP
                if (!isPrivateIp(ip)) {
                    return ip;
                }
            }
            // 如果都是私有 IP，返回第一个 IP
            return xff.split(",")[0].trim();
        }
        // 其次使用 X-Real-IP
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        // 最后使用远程地址
        return remoteAddr;
    }

    /**
     * 判断是否为私有 IP 地址
     * @param ip IP 地址
     * @return 如果是私有 IP 返回 true，否则返回 false
     */
    private boolean isPrivateIp(String ip) {
        if (ip == null || ip.isEmpty())           return true;
        if (ip.startsWith("127."))                return true;  // 本地回环地址
        if (ip.startsWith("10."))                 return true;  // A 类私有地址
        if (ip.startsWith("192.168."))            return true;  // C 类私有地址
        if ("::1".equals(ip) || ip.startsWith("fe80:")) return true;  // IPv6 本地地址
        if (ip.startsWith("172.")) {
            try {
                // 检查是否为 B 类私有地址 (172.16.0.0 - 172.31.255.255)
                int second = Integer.parseInt(ip.split("\\.")[1]);
                return second >= 16 && second <= 31;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * 判断是否为静态资源请求
     * @param uri 请求 URI
     * @return 如果是静态资源返回 true，否则返回 false
     */
    private boolean isStaticResource(String uri) {
        if (uri == null) return false;

        // 检查常见的静态资源扩展名和路径
        return uri.endsWith(".js")    || uri.endsWith(".css")    // JavaScript 和 CSS
            || uri.endsWith(".png")   || uri.endsWith(".jpg")    // 图片文件
            || uri.endsWith(".ico")   || uri.endsWith(".svg")    // 图标和 SVG
            || uri.endsWith(".woff")  || uri.endsWith(".woff2")  // 字体文件
            || uri.equals("/healthz")                              // 健康检查接口
            || uri.startsWith("/actuator");                         // Actuator 接口
    }
}
