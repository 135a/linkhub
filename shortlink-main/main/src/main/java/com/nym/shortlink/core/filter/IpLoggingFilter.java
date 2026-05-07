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
        if (request instanceof HttpServletRequest httpReq) {
            String uri = httpReq.getRequestURI();
            if (!isStaticResource(uri)) {
                String xff        = httpReq.getHeader("X-Forwarded-For");
                String xRealIp    = httpReq.getHeader("X-Real-IP");
                String remoteAddr = request.getRemoteAddr();
                String resolved   = resolveRealIp(xff, xRealIp, remoteAddr);

                log.info("[IP-LOG] {} {} | resolved={} | X-Forwarded-For={} | X-Real-IP={} | remoteAddr={}",
                        httpReq.getMethod(), uri,
                        resolved,
                        xff       != null ? xff       : "(null)",
                        xRealIp   != null ? xRealIp   : "(null)",
                        remoteAddr);
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveRealIp(String xff, String xRealIp, String remoteAddr) {
        if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
            for (String part : xff.split(",")) {
                String ip = part.trim();
                if (!isPrivateIp(ip)) {
                    return ip;
                }
            }
            return xff.split(",")[0].trim();
        }
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return remoteAddr;
    }

    private boolean isPrivateIp(String ip) {
        if (ip == null || ip.isEmpty())           return true;
        if (ip.startsWith("127."))                return true;
        if (ip.startsWith("10."))                 return true;
        if (ip.startsWith("192.168."))            return true;
        if ("::1".equals(ip) || ip.startsWith("fe80:")) return true;
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                return second >= 16 && second <= 31;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private boolean isStaticResource(String uri) {
        if (uri == null) return false;
        return uri.endsWith(".js")    || uri.endsWith(".css")
            || uri.endsWith(".png")   || uri.endsWith(".jpg")
            || uri.endsWith(".ico")   || uri.endsWith(".svg")
            || uri.endsWith(".woff")  || uri.endsWith(".woff2")
            || uri.equals("/healthz")
            || uri.startsWith("/actuator");
    }
}
