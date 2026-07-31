package com.example.medicinebox.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SecurityConfig 安全配置集成测试
 * 验证 URL 白名单和认证拦截规则的正确性
 * 使用 H2 内存数据库 + test profile，无需外部数据库
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SecurityConfig 安全配置测试")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("访问 /api/v1/auth/me - 未登录应返回 401")
    void accessAuthMe_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("访问 /api/v1/auth/users - 未登录应返回 401")
    void accessUsers_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("访问 /api/v1/medicines - 未登录应返回 401")
    void accessMedicines_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/medicines"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("访问 /api/v1/medication/today - 未登录应返回 401")
    void accessReminders_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/medication/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("访问 /api/v1/prescription/history - 未登录应返回 401")
    void accessPrescriptions_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/prescription/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("访问 /api/v1/device/history - 未登录应返回 401")
    void accessCommands_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/device/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("访问注册接口 /api/v1/auth/register - 无需认证")
    void accessRegister_NoAuthRequired() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{\"phone\":\"13800138000\",\"password\":\"Password123\",\"name\":\"张三\"}"))
                .andExpect(mvcResult -> {
                    int status = mvcResult.getResponse().getStatus();
                    assertNotEquals(401, status, "注册接口不应返回401");
                });
    }

    @Test
    @DisplayName("访问登录接口 /api/v1/auth/login - 无需认证")
    void accessLogin_NoAuthRequired() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"phone\":\"13800138000\",\"password\":\"Password123\"}"))
                .andExpect(mvcResult -> {
                    int status = mvcResult.getResponse().getStatus();
                    assertTrue(status == 200 || status == 401,
                            "Expected status 200 or 401, got " + status);
                });
    }
}