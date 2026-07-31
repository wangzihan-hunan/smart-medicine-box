package com.example.medicinebox.controller;

import com.example.medicinebox.dto.request.UserLoginRequest;
import com.example.medicinebox.dto.request.UserRegisterRequest;
import com.example.medicinebox.dto.response.LoginResponse;
import com.example.medicinebox.dto.response.RegisterResponse;
import com.example.medicinebox.dto.response.UserInfoResponse;
import com.example.medicinebox.dto.response.UserListResponse;
import com.example.medicinebox.exception.BadRequestException;
import com.example.medicinebox.interceptor.RateLimitInterceptor;
import com.example.medicinebox.service.AdminUserService;
import com.example.medicinebox.service.AuthService;
import com.example.medicinebox.service.SmsCaptchaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController 集成测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private SmsCaptchaService smsCaptchaService;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    private UserRegisterRequest registerRequest;
    private UserLoginRequest loginRequest;
    private RegisterResponse registerResponse;
    private LoginResponse loginResponse;
    private UserInfoResponse userInfoResponse;

    @BeforeEach
    void setUp() throws Exception {
        registerRequest = new UserRegisterRequest("13800138000", "Password123", "张三");
        loginRequest = new UserLoginRequest("13800138000", "Password123");
        registerResponse = new RegisterResponse(1);
        loginResponse = new LoginResponse("test-token", "bearer");
        // UserInfoResponse 构造函数会脱敏手机号：138****8000
        userInfoResponse = new UserInfoResponse(1, "13800138000", "张三", "USER", false);

        // Mock RateLimitInterceptor 放行所有请求（Mockito 默认返回 false 会阻塞请求）
        when(rateLimitInterceptor.preHandle(
                any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
    }

    @Test
    @DisplayName("注册 - 成功返回200")
    void register_Success_Returns200() throws Exception {
        when(authService.register(any(UserRegisterRequest.class))).thenReturn(registerResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功！去登录吧"))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    @DisplayName("注册 - 手机号已存在返回400")
    void register_PhoneExists_Returns400() throws Exception {
        when(authService.register(any(UserRegisterRequest.class)))
                .thenThrow(new BadRequestException("该手机号已注册，请直接登录！"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("该手机号已注册，请直接登录！"));
    }

    @Test
    @DisplayName("注册 - 参数校验失败返回400")
    void register_ValidationFailed_Returns400() throws Exception {
        UserRegisterRequest invalidRequest = new UserRegisterRequest("", "", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("登录 - 成功返回200和token")
    void login_Success_Returns200WithToken() throws Exception {
        when(authService.login(any(UserLoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功！"))
                .andExpect(jsonPath("$.data.accessToken").value("test-token"))
                .andExpect(jsonPath("$.data.tokenType").value("bearer"));
    }

    @Test
    @DisplayName("登录 - 密码错误返回401")
    void login_WrongPassword_Returns401() throws Exception {
        when(authService.login(any(UserLoginRequest.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("手机号或密码错误！"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("手机号或密码错误！"));
    }

    @Test
    @DisplayName("获取当前用户信息 - 登录后成功返回200")
    @WithMockUser(username = "13800138000", roles = {"USER"})
    void getCurrentUserInfo_LoggedIn_Success() throws Exception {
        when(authService.getCurrentUserInfo()).thenReturn(userInfoResponse);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.phone").value("138****8000"))
                .andExpect(jsonPath("$.data.name").value("张三"));
    }

    @Test
    @DisplayName("更新用户信息 - 登录后成功返回200")
    @WithMockUser(username = "13800138000", roles = {"USER"})
    void updateUserInfo_LoggedIn_Success() throws Exception {
        UserInfoResponse updatedInfo = new UserInfoResponse(1, "13800138000", "李四", "USER", false);
        when(authService.updateUserInfo(anyString())).thenReturn(updatedInfo);

        mockMvc.perform(put("/api/v1/auth/me")
                        .param("name", "李四"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("李四"));
    }

    @Test
    @DisplayName("获取用户列表 - 管理员登录后成功返回200")
    @WithMockUser(username = "13800138000", roles = {"ADMIN"})
    void getAllUsers_LoggedIn_Success() throws Exception {
        UserListResponse listResponse = new UserListResponse(1, Arrays.asList(userInfoResponse));
        when(adminUserService.getAllUsers(anyInt(), anyInt())).thenReturn(listResponse);

        mockMvc.perform(get("/api/v1/auth/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}