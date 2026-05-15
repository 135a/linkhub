package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.service.GroupService;
import com.nym.shortlink.core.service.RecycleBinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Group & RecycleBin - 分组与回收站 API")
class GroupControllerTest {

    @Mock
    private GroupService groupService;
    @Mock
    private RecycleBinService recycleBinService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GroupController groupController = new GroupController(groupService);
        RecycleBinController recycleBinController = new RecycleBinController(recycleBinService);
        mockMvc = MockMvcBuilders.standaloneSetup(groupController, recycleBinController).build();
    }

    @Test
    @DisplayName("查询分组列表返回 200")
    void listGroupReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/short-link/admin/v1/group"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    @DisplayName("创建分组返回 200")
    void saveGroupReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/short-link/admin/v1/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "new-group"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    @DisplayName("移入回收站返回 200")
    void saveRecycleBinReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/short-link/admin/v1/recycle-bin/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullShortUrl": "s.test/abc123", "gid": "test-gid"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    @DisplayName("回收站分页查询返回 200")
    void pageRecycleBinReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/short-link/admin/v1/recycle-bin/page")
                        .param("gid", "test-gid")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    @DisplayName("从回收站恢复返回 200")
    void recoverRecycleBinReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/short-link/admin/v1/recycle-bin/recover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullShortUrl": "s.test/abc123", "gid": "test-gid"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }
}
