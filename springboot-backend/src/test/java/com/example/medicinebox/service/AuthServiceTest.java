package com.example.medicinebox.service;

import com.example.medicinebox.dto.request.UserLoginRequest;
import com.example.medicinebox.dto.request.UserRegisterRequest;
import com.example.medicinebox.dto.response.LoginResponse;
import com.example.medicinebox.dto.response.RegisterResponse;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.AccountLockedException;
import com.example.medicinebox.exception.BadRequestException;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private MetricsService metricsService;

    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User testUser;
    private UserRegisterRequest registerRequest;
    private UserLoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil, authenticationManager, metricsService);
        ReflectionTestUtils.setField(authService, "self", authService);

        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setPassword("encodedPassword");
        testUser.setName("张三");
        testUser.setRole("USER");
        testUser.setStatus("ACTIVE");
        testUser.setLoginAttempts(0);

        registerRequest = new UserRegisterRequest("13800138000", "Password123", "张三");
        loginRequest = new UserLoginRequest("13800138000", "Password123");
    }

    @Test
    @DisplayName("注册 - 新用户成功")
    void register_NewUser_Success() {
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        RegisterResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals(1, response.getUserId());
        verify(userRepository).existsByPhone("13800138000");
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertEquals("13800138000", savedUser.getPhone());
        assertEquals("张三", savedUser.getName());
    }

    @Test
    @DisplayName("注册 - 手机号已存在抛出异常")
    void register_PhoneExists_ThrowsBadRequestException() {
        when(userRepository.existsByPhone(anyString())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, 
                () -> authService.register(registerRequest));

        assertEquals("该手机号已注册，请直接登录！", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("登录 - 手机号不存在抛出异常")
    void login_PhoneNotFound_ThrowsBadCredentialsException() {
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, 
                () -> authService.login(loginRequest));

        assertEquals("手机号或密码错误！", exception.getMessage());
    }

    @Test
    @DisplayName("登录 - 账号已锁定且未过期抛出异常")
    void login_AccountLockedAndNotExpired_ThrowsAccountLockedException() {
        testUser.setStatus("LOCKED");
        testUser.setLockTime(LocalDateTime.now().minusMinutes(10));
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(testUser));

        AccountLockedException exception = assertThrows(AccountLockedException.class, 
                () -> authService.login(loginRequest));

        assertTrue(exception.getMessage().contains("账号已被锁定"));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("登录 - 账号锁定时间过期自动解锁")
    void login_AccountLockedAndExpired_AutomaticallyUnlocked() {
        testUser.setStatus("LOCKED");
        testUser.setLockTime(LocalDateTime.now().minusMinutes(31));
        testUser.setLoginAttempts(5);
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyString())).thenReturn("test-jwt-token");

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        verify(userRepository, times(2)).save(userCaptor.capture());
        
        User unlockedUser = userCaptor.getAllValues().get(0);
        assertEquals("ACTIVE", unlockedUser.getStatus());
        assertNull(unlockedUser.getLockTime());
        assertEquals(0, unlockedUser.getLoginAttempts());
    }

    @Test
    @DisplayName("增加登录尝试 - 正常增加")
    void incrementLoginAttempts_NormalCase_Increments() {
        testUser.setLoginAttempts(2);
        
        authService.incrementLoginAttempts(testUser);

        verify(userRepository).save(userCaptor.capture());
        assertEquals(3, userCaptor.getValue().getLoginAttempts());
        assertEquals("ACTIVE", userCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("增加登录尝试 - 达到5次锁定账号")
    void incrementLoginAttempts_FiveAttempts_LocksAccount() {
        testUser.setLoginAttempts(4);
        
        authService.incrementLoginAttempts(testUser);

        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals(5, updatedUser.getLoginAttempts());
        assertEquals("LOCKED", updatedUser.getStatus());
        assertNotNull(updatedUser.getLockTime());
    }

    @Test
    @DisplayName("重置登录尝试 - 成功重置")
    void resetLoginAttempts_Success_Resets() {
        testUser.setLoginAttempts(3);
        
        authService.resetLoginAttempts(testUser);

        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals(0, updatedUser.getLoginAttempts());
        assertNotNull(updatedUser.getLastLoginTime());
    }
}