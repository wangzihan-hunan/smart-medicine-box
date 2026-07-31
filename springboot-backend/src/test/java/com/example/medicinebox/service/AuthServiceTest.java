package com.example.medicinebox.service;

import com.example.medicinebox.config.AuthProperties;
import com.example.medicinebox.constant.UserRole;
import com.example.medicinebox.constant.UserStatus;
import com.example.medicinebox.dto.request.UserLoginRequest;
import com.example.medicinebox.dto.response.LoginResponse;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.AccountLockedException;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 * <p>
 * 仅覆盖集成测试难以验证的细粒度业务逻辑：
 * 登录失败计数、账号锁定/解锁阈值、登录尝试重置。
 * 注册和登录成功/失败流程已由 AuthControllerTest 集成测试覆盖。
 * <p>
 * Mock 数量：4（UserRepository, JwtUtil, AuthenticationManager, MetricsService），较优化前减少 33%。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private MetricsService metricsService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private AuthService authService;
    private AuthProperties authProperties;
    private User testUser;
    private UserLoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // 使用真实 AuthProperties 实例替代 Mock，减少不必要的模拟
        authProperties = new AuthProperties();
        authProperties.setMaxLoginAttempts(5);
        authProperties.setLockDurationMinutes(30L);

        // 构造 AuthService，PasswordEncoder 在保留的测试中未使用，传 null
        authService = new AuthService(userRepository, null, jwtUtil,
                authenticationManager, metricsService, authProperties, null);
        ReflectionTestUtils.setField(authService, "self", authService);

        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setPassword("encodedPassword");
        testUser.setName("张三");
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setLoginAttempts(0);

        loginRequest = new UserLoginRequest("13800138000", "Password123");
    }

    @Test
    @DisplayName("登录 - 账号已锁定且未过期抛出异常")
    void login_AccountLockedAndNotExpired_ThrowsAccountLockedException() {
        testUser.setStatus(UserStatus.LOCKED);
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
        testUser.setStatus(UserStatus.LOCKED);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(31));
        testUser.setLoginAttempts(5);
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("test-jwt-token");

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        verify(userRepository, times(2)).save(userCaptor.capture());

        User unlockedUser = userCaptor.getAllValues().get(0);
        assertEquals(UserStatus.ACTIVE, unlockedUser.getStatus());
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
        assertEquals(UserStatus.ACTIVE, userCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("增加登录尝试 - 达到5次锁定账号")
    void incrementLoginAttempts_FiveAttempts_LocksAccount() {
        testUser.setLoginAttempts(4);

        authService.incrementLoginAttempts(testUser);

        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals(5, updatedUser.getLoginAttempts());
        assertEquals(UserStatus.LOCKED, updatedUser.getStatus());
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