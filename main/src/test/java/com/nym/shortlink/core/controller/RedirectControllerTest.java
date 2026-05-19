package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.service.ShortLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkController - 短链接跳转")
class RedirectControllerTest {

    @Mock
    private ShortLinkService shortLinkService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ShortLinkController(shortLinkService)).build();
    }

    @Test
    @DisplayName("短链接跳转调用 restoreUrl 返回 302")
    void redirectCallsRestoreUrlAndReturns302() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(2, jakarta.servlet.http.HttpServletResponse.class);
            response.sendRedirect("https://example.com/original");
            return null;
        }).when(shortLinkService).restoreUrl(eq("abc123"), any(), any());

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/original"));
    }

    @Test
    @DisplayName("不存在的短链接返回 302 到 /page/notfound")
    void notfoundShortLinkReturnsRedirectToNotFound() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(2, jakarta.servlet.http.HttpServletResponse.class);
            response.sendRedirect("/page/notfound");
            return null;
        }).when(shortLinkService).restoreUrl(eq("nonexist"), any(), any());

        mockMvc.perform(get("/nonexist"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/page/notfound"));
    }
}
