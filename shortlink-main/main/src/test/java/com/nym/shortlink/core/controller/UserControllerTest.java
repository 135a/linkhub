package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.dto.req.UserLoginReqDTO;
import com.nym.shortlink.core.dto.resp.UserLoginRespDTO;
import com.nym.shortlink.core.service.UserService;
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
@DisplayName("UserController - 用户 API")
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();
    }

    @Test
    @DisplayName("注册成功返回 200")
    void registerReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/short-link/admin/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "testuser",
                                    "password": "password123",
                                    "phone": "13800000001"
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    @DisplayName("登录成功返回 token")
    void loginReturnsToken() throws Exception {
        when(userService.login(any(UserLoginReqDTO.class)))
                .thenReturn(new UserLoginRespDTO("mock-token-123"));

        mockMvc.perform(post("/api/short-link/admin/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "testuser",
                                    "password": "password123"
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.token").value("mock-token-123"));
    }

    @Test
    @DisplayName("检查用户名是否存在")
    void hasUsernameReturnsBoolean() throws Exception {
        when(userService.hasUsername("newuser")).thenReturn(true);

        mockMvc.perform(get("/api/short-link/admin/v1/user/has-username")
                        .param("username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").value(true));
    }
}
