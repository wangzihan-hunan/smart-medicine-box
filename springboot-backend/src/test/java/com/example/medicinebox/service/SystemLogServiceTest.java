package com.example.medicinebox.service;

import com.example.medicinebox.dto.response.SystemLogListResponse;
import com.example.medicinebox.entity.SystemLog;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.repository.SystemLogRepository;
import com.example.medicinebox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SystemLogService 单元测试类
 * 测试系统日志记录和查询功能
 */
@ExtendWith(MockitoExtension.class)
class SystemLogServiceTest {

    @Mock
    private SystemLogRepository systemLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SystemLogService systemLogService;

    private User testUser;
    private SystemLog testLog;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setName("测试用户");

        testLog = new SystemLog();
        testLog.setId(1);
        testLog.setUserId(1);
        testLog.setModule("用户认证");
        testLog.setOperation("登录成功");
        testLog.setDescription("用户登录成功");
        testLog.setIpAddress("127.0.0.1");
        testLog.setCreatedAt(LocalDateTime.now());
        testLog.setSuccess(true);
    }

    @Test
    @DisplayName("记录登录成功日志")
    void recordLoginSuccess_shouldSaveLog() {
        // Given
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(testUser));
        when(systemLogRepository.save(any(SystemLog.class))).thenReturn(testLog);

        // When
        systemLogService.recordLoginSuccess("13800138000", "127.0.0.1");

        // Then - verify log was saved
        verify(systemLogRepository).save(any(SystemLog.class));
    }

    @Test
    @DisplayName("记录登录失败日志")
    void recordLoginFailure_shouldSaveLog() {
        // Given
        when(systemLogRepository.save(any(SystemLog.class))).thenReturn(testLog);

        // When
        systemLogService.recordLoginFailure("13800138000", "127.0.0.1", "密码错误");

        // Then
        verify(systemLogRepository).save(any(SystemLog.class));
    }

    @Test
    @DisplayName("记录用户注册日志")
    void recordRegister_shouldSaveLog() {
        // Given
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(testUser));
        when(systemLogRepository.save(any(SystemLog.class))).thenReturn(testLog);

        // When
        systemLogService.recordRegister("13800138000", "测试用户");

        // Then
        verify(systemLogRepository).save(any(SystemLog.class));
    }

    @Test
    @DisplayName("获取最新10条日志")
    void getTop10Logs_shouldReturnList() {
        // Given
        Page<SystemLog> logPage = new PageImpl<>(Collections.singletonList(testLog));
        when(systemLogRepository.findTop10ByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(logPage);

        // When
        SystemLogListResponse response = systemLogService.getTop10Logs();

        // Then
        assertNotNull(response);
        assertEquals(1, response.getList().size());
    }

    @Test
    @DisplayName("按用户ID获取最新10条日志")
    void getTop10LogsByUserId_shouldReturnList() {
        // Given
        Page<SystemLog> logPage = new PageImpl<>(Collections.singletonList(testLog));
        when(systemLogRepository.findTop10ByUserIdOrderByCreatedAtDesc(eq(1), any(Pageable.class))).thenReturn(logPage);

        // When
        SystemLogListResponse response = systemLogService.getTop10LogsByUserId(1);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getList().size());
    }

    @Test
    @DisplayName("分页查询所有日志")
    void getLogs_shouldReturnPagedResult() {
        // Given
        Page<SystemLog> logPage = new PageImpl<>(Arrays.asList(testLog));
        when(systemLogRepository.findAll(any(Pageable.class))).thenReturn(logPage);

        // When
        SystemLogListResponse response = systemLogService.getLogs(0, 10);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("按模块分页查询日志")
    void getLogsByModule_shouldReturnPagedResult() {
        // Given
        Page<SystemLog> logPage = new PageImpl<>(Arrays.asList(testLog));
        when(systemLogRepository.findByModule(anyString(), any(Pageable.class))).thenReturn(logPage);

        // When
        SystemLogListResponse response = systemLogService.getLogsByModule("用户认证", 0, 10);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("按用户ID分页查询日志")
    void getLogsByUserId_shouldReturnPagedResult() {
        // Given
        Page<SystemLog> logPage = new PageImpl<>(Arrays.asList(testLog));
        when(systemLogRepository.findByUserId(any(Integer.class), any(Pageable.class))).thenReturn(logPage);

        // When
        SystemLogListResponse response = systemLogService.getLogsByUserId(1, 0, 10);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("获取所有模块名称")
    void findDistinctModules_shouldReturnList() {
        // Given
        when(systemLogRepository.findDistinctModules()).thenReturn(Arrays.asList("用户认证", "药品管理"));

        // When
        List<String> modules = systemLogService.findDistinctModules();

        // Then
        assertNotNull(modules);
        assertEquals(2, modules.size());
    }

    @Test
    @DisplayName("统计所有日志数量")
    void countAllLogs_shouldReturnCount() {
        // Given
        when(systemLogRepository.count()).thenReturn(100L);

        // When
        long count = systemLogService.countAllLogs();

        // Then
        assertEquals(100L, count);
    }

    @Test
    @DisplayName("统计今日日志数量")
    void countTodayLogs_shouldReturnCount() {
        // Given
        when(systemLogRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(10L);

        // When
        long count = systemLogService.countTodayLogs();

        // Then
        assertEquals(10L, count);
    }

    @Test
    @DisplayName("统计失败日志数量")
    void countFailedLogs_shouldReturnCount() {
        // Given
        when(systemLogRepository.countBySuccess(false)).thenReturn(5L);

        // When
        long count = systemLogService.countFailedLogs();

        // Then
        assertEquals(5L, count);
    }

    @Test
    @DisplayName("根据手机号获取用户")
    void getUserByPhone_shouldReturnUser() {
        // Given
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(testUser));

        // When
        User user = systemLogService.getUserByPhone("13800138000");

        // Then
        assertNotNull(user);
        assertEquals("13800138000", user.getPhone());
    }

    @Test
    @DisplayName("根据手机号获取用户 - 不存在应抛出异常")
    void getUserByPhone_notFoundShouldThrow() {
        // Given
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            systemLogService.getUserByPhone("99999999999");
        });
    }
}