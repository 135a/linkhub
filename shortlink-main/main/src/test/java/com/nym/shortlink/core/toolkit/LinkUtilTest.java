package com.nym.shortlink.core.toolkit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkUtilTest {

    // ==================== IP 提取测试 ====================

    @Test
    @DisplayName("X-Forwarded-For 返回第一个公网 IP")
    void getActualIpWithXForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        assertEquals("203.0.113.1", LinkUtil.getActualIp(request));
    }

    @Test
    @DisplayName("X-Forwarded-For 逗号列表跳过私有 IP 取第一个公网 IP")
    void getActualIpSkipsPrivateIpsInCommaList() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("172.17.0.1, 203.0.113.5, 10.0.0.1");
        assertEquals("203.0.113.5", LinkUtil.getActualIp(request));
    }

    @Test
    @DisplayName("X-Forwarded-For 全为私有 IP 时取第一个兜底")
    void getActualIpAllPrivateFallsBack() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("172.17.0.1, 10.0.0.2");
        assertEquals("172.17.0.1", LinkUtil.getActualIp(request));
    }

    @Test
    @DisplayName("无代理头时返回 RemoteAddr")
    void getActualIpReturnsRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        assertEquals("192.168.1.100", LinkUtil.getActualIp(request));
    }

    // ==================== 域名提取测试 ====================

    @Test
    @DisplayName("提取标准 HTTPS URL 域名")
    void extractDomainFromHttpsUrl() {
        assertEquals("example.com", LinkUtil.extractDomain("https://example.com/path/to/page"));
    }

    @Test
    @DisplayName("提取带 www 前缀的域名时自动去除")
    void extractDomainStripsWww() {
        assertEquals("example.com", LinkUtil.extractDomain("https://www.example.com/page"));
    }

    @Test
    @DisplayName("无效 URL 返回 null")
    void extractDomainReturnsNullForInvalidUrl() {
        assertNull(LinkUtil.extractDomain("not-a-valid-url"));
    }

    // ==================== UA 解析测试 ====================

    @ParameterizedTest
    @CsvSource({
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64), Windows",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7), Mac OS",
            "Mozilla/5.0 (X11; Linux x86_64), Linux",
            "Mozilla/5.0 (Linux; Android 13) Mobile, Linux",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0), iOS",
            "Mozilla/5.0 (iPad; CPU OS 16_0), iOS",
    })
    @DisplayName("操作系统检测")
    void getOsDetectsCorrectly(String userAgent, String expectedOs) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn(userAgent);
        assertEquals(expectedOs, LinkUtil.getOs(request));
    }

    @ParameterizedTest
    @CsvSource({
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Edg/120.0, Microsoft Edge",
            "Mozilla/5.0 Chrome/120.0 Safari/537.36, Google Chrome",
            "Mozilla/5.0 Firefox/120.0, Mozilla Firefox",
            "Mozilla/5.0 Safari/605.1.15, Apple Safari",
            "Mozilla/5.0 Opera/9.80, Opera",
            "Mozilla/5.0 (compatible; MSIE 9.0), Internet Explorer",
            "Mozilla/4.0 (compatible; MSIE 7.0; Trident/4.0), Internet Explorer",
    })
    @DisplayName("浏览器检测")
    void getBrowserDetectsCorrectly(String userAgent, String expectedBrowser) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn(userAgent);
        assertEquals(expectedBrowser, LinkUtil.getBrowser(request));
    }

    @ParameterizedTest
    @CsvSource({
            "Mozilla/5.0 (Linux; Android 13) Mobile, Mobile",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64), PC",
    })
    @DisplayName("设备类型检测")
    void getDeviceDetectsCorrectly(String userAgent, String expectedDevice) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn(userAgent);
        assertEquals(expectedDevice, LinkUtil.getDevice(request));
    }

    @Test
    @DisplayName("Network 检测 - 私有 IP 返回 WIFI")
    void getNetworkReturnsWifiForPrivateIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        assertEquals("WIFI", LinkUtil.getNetwork(request));
    }

    // ==================== 缓存 TTL 测试 ====================

    @Test
    @DisplayName("永久有效返回默认缓存时间")
    void getLinkCacheValidTimeNullDateReturnsDefault() {
        long result = LinkUtil.getLinkCacheValidTime(null);
        assertTrue(result > 0);
        assertTrue(result >= 2000000000L, "Default cache time should be large (~30 days in ms)");
    }

    @Test
    @DisplayName("未来有效期返回正数 TTL")
    void getLinkCacheValidTimeFutureDateReturnsPositive() {
        Date futureDate = new Date(System.currentTimeMillis() + 86400000L); // 1 day later
        long result = LinkUtil.getLinkCacheValidTime(futureDate);
        assertTrue(result > 0);
        assertTrue(result <= 86400000L + 10000L, "TTL should be around 1 day in ms");
    }

    @Test
    @DisplayName("过期有效期返回绝对值（Hutool between 取绝对值）")
    void getLinkCacheValidTimeExpiredDateReturnsAbsoluteValue() {
        Date pastDate = new Date(System.currentTimeMillis() - 3600000L);
        long result = LinkUtil.getLinkCacheValidTime(pastDate);
        // Hutool DateUtil.between 返回绝对值
        assertTrue(Math.abs(result - 3600000L) < 10000L,
                "Expected ~3600000ms but got: " + result);
    }
}
