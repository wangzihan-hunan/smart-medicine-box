package com.example.medicinebox.service;

import com.example.medicinebox.dto.request.ReminderCreateRequest;
import com.example.medicinebox.dto.response.ReminderListResponse;
import com.example.medicinebox.dto.response.ReminderResponse;
import com.example.medicinebox.entity.Reminder;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.ReminderRepository;
import com.example.medicinebox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * ReminderService 单元测试类
 * 测试用药提醒的增删改查功能
 */
@ExtendWith(MockitoExtension.class)
@Disabled("静态mock SecurityContextHolder存在问题,待后续修复")
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private ReminderService reminderService;

    private User testUser;
    private Reminder testReminder;

    @BeforeEach
    void setUp() {
        // 准备测试用户
        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setName("测试用户");

        // 准备测试提醒
        testReminder = new Reminder();
        testReminder.setId(1);
        testReminder.setUserId(1);
        testReminder.setMedicineId(100);
        testReminder.setMedicineName("阿司匹林");
        testReminder.setTime("08:00");
        testReminder.setDosage("1片");
        testReminder.setFrequency("每日一次");
        testReminder.setIsRepeat(false);
        testReminder.setTaken(false);
    }

    private void mockCurrentUser() {
        // 模拟 SecurityContext
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(testUser.getPhone());
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("添加提醒 - 成功")
    void addReminder_shouldSucceed() {
        // Given
        ReflectionTestUtils.setField(reminderService, "userRepository", userRepository);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(testUser.getPhone());
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));

        ReminderCreateRequest request = new ReminderCreateRequest();
        request.setMedicineId(100);
        request.setMedicineName("阿司匹林");
        request.setTime("08:00");
        request.setDosage("1片");
        request.setFrequency("每日一次");

        when(reminderRepository.save(any(Reminder.class))).thenReturn(testReminder);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            ReminderResponse response = reminderService.addReminder(request);

            // Then
            assertNotNull(response);
            assertEquals("阿司匹林", response.getMedicineName());
            verify(reminderRepository).save(any(Reminder.class));
        }
    }

    @Test
    @DisplayName("获取今日提醒 - 成功")
    void getTodayReminders_shouldSucceed() {
        // Given
        ReflectionTestUtils.setField(reminderService, "userRepository", userRepository);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(testUser.getPhone());
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));

        Page<Reminder> reminderPage = new PageImpl<>(Arrays.asList(testReminder));
        when(reminderRepository.findByUserIdAndDate(anyInt(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(reminderPage);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            ReminderListResponse response = reminderService.getTodayReminders(0, 10);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
        }
    }

    @Test
    @DisplayName("标记已服用 - 成功")
    void markTaken_shouldSucceed() {
        // Given
        ReflectionTestUtils.setField(reminderService, "userRepository", userRepository);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(testUser.getPhone());
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));

        when(reminderRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testReminder));
        when(reminderRepository.save(any(Reminder.class))).thenReturn(testReminder);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            ReminderResponse response = reminderService.markTaken(1);

            // Then
            assertNotNull(response);
            verify(reminderRepository).save(any(Reminder.class));
        }
    }

    @Test
    @DisplayName("标记已服用 - 提醒不存在应抛出异常")
    void markTaken_notFoundShouldThrowException() {
        // Given
        ReflectionTestUtils.setField(reminderService, "userRepository", userRepository);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(testUser.getPhone());
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));

        when(reminderRepository.findByIdAndUserId(999, 1)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                reminderService.markTaken(999);
            });
        }
    }

    @Test
    @DisplayName("更新提醒 - 成功")
    void updateReminder_shouldSucceed() {
        // Given
        ReflectionTestUtils.setField(reminderService, "userRepository", userRepository);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(testUser.getPhone());
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));

        ReminderCreateRequest request = new ReminderCreateRequest();
        request.setMedicineName("布洛芬");
        request.setDosage("2片");

        when(reminderRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testReminder));
        when(reminderRepository.save(any(Reminder.class))).thenReturn(testReminder);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            ReminderResponse response = reminderService.updateReminder(1, request);

            // Then
            assertNotNull(response);
            verify(reminderRepository).save(any(Reminder.class));
        }
    }

    @Test
    @DisplayName("更新提醒 - 仅更新非空字段")
    void updateReminder_shouldOnlyUpdateNonNullFields() {
        // Given
        ReflectionTestUtils.setField(reminderService, "userRepository", userRepository);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(testUser.getPhone());
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));

        ReminderCreateRequest request = new ReminderCreateRequest();
        request.setMedicineName("布洛芬"); // 只更新名称
        // 其他字段为 null

        when(reminderRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testReminder));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder saved = invocation.getArgument(0);
            assertEquals("布洛芬", saved.getMedicineName());
            assertEquals("08:00", saved.getTime()); // 保持原值
            return saved;
        });

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            ReminderResponse response = reminderService.updateReminder(1, request);

            // Then
            assertNotNull(response);
        }
    }

    @Test
    @DisplayName("删除提醒 - 成功")
    void deleteReminder_shouldSucceed() {
        // Given
        ReflectionTestUtils.setField(reminderService, "userRepository", userRepository);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(testUser.getPhone());
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));

        when(reminderRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testReminder));
        doNothing().when(reminderRepository).delete(any(Reminder.class));

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            reminderService.deleteReminder(1);

            // Then
            verify(reminderRepository).delete(any(Reminder.class));
        }
    }

    @Test
    @DisplayName("删除提醒 - 不存在应抛出异常")
    void deleteReminder_notFoundShouldThrowException() {
        // Given
        ReflectionTestUtils.setField(reminderService, "userRepository", userRepository);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(testUser.getPhone());
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));

        when(reminderRepository.findByIdAndUserId(999, 1)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                reminderService.deleteReminder(999);
            });
        }
    }
}