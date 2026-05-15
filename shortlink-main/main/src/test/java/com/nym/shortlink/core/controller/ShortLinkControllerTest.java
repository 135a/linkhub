package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.dto.req.ShortLinkCreateReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkCreateRespDTO;
import com.nym.shortlink.core.service.ShortLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkController - 短链接管理 API")
class ShortLinkControllerTest {

    @Mock
    private ShortLinkService shortLinkService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ShortLinkController(shortLinkService)).build();
    }

    @Test
    @DisplayName("创建短链接返回 200 和 fullShortUrl")
    void createShortLinkReturnsSuccess() throws Exception {
        ShortLinkCreateRespDTO mockResp = ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://s.test/abc123")
                .originUrl("https://example.com")
                .gid("test-gid")
                .build();

        when(shortLinkService.createShortLink(any(ShortLinkCreateReqDTO.class)))
                .thenReturn(mockResp);

        mockMvc.perform(post("/api/short-link/admin/v1/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "originUrl": "https://example.com",
                                    "gid": "test-gid",
                                    "createdType": 0,
                                    "validDateType": 0
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.fullShortUrl").value("http://s.test/abc123"));
    }

    @Test
    @DisplayName("更新短链接返回 200")
    void updateShortLinkReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/short-link/admin/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "fullShortUrl": "s.test/abc123",
                                    "originGid": "test-gid",
                                    "gid": "test-gid",
                                    "originUrl": "https://updated.com",
                                    "validDateType": 0
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    @DisplayName("分页查询返回 200")
    void pageShortLinkReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/short-link/admin/v1/page")
                        .param("gid", "test-gid")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }
}
