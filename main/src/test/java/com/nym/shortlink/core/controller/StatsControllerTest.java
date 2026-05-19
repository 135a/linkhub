package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.service.ShortLinkStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkStatsController - 统计查询 API")
class StatsControllerTest {

    @Mock
    private ShortLinkStatsService shortLinkStatsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ShortLinkStatsController(shortLinkStatsService)).build();
    }

    @Test
    @DisplayName("短链接统计查询返回 200")
    void shortLinkStatsReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/short-link/admin/v1/stats")
                        .param("fullShortUrl", "s.test/abc123")
                        .param("gid", "test-gid")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    @DisplayName("分组统计查询返回 200")
    void groupShortLinkStatsReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/short-link/admin/v1/stats/group")
                        .param("gid", "test-gid")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    @DisplayName("访问记录查询返回 200")
    void accessRecordReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/short-link/admin/v1/stats/access-record")
                        .param("fullShortUrl", "s.test/abc123")
                        .param("gid", "test-gid")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }
}
