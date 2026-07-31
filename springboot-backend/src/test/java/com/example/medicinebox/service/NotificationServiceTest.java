package com.example.medicinebox.service;

import com.example.medicinebox.entity.Notification;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.NotificationRepository;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.util.TestSecurityContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationService 单元测试
 * 覆盖通知发送、分页查询、统计、已读标记、删除等核心功能
 * Mock 数量：3（NotificationRepository, WebSocketPushService, UserRepository）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationService 单元测试")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WebSocketPushService webSocketPushService;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(userRepository, notificationRepository, webSocketPushService);

        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setName("测试用户");

        testNotification = new Notification();
        testNotification.setId(1);
        testNotification.setUserId(1);
        testNotification.setTitle("用药提醒");
        testNotification.setContent("请按时服用阿司匹林");
        testNotification.setType("MEDICINE_REMINDER");
        testNotification.setReadStatus(false);

        TestSecurityContextHelper.setCurrentUser(testUser.getPhone(), "USER");
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        TestSecurityContextHelper.clearContext();
    }

    // ========== 发送通知测试 ==========

    @Test
    @DisplayName("发送通知 - 保存通知并推送 WebSocket 消息")
    void sendNotification_ValidParams_SavesAndPushes() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(captor.capture())).thenReturn(testNotification);

        notificationService.sendNotification(1, "用药提醒", "请按时服用阿司匹林", "MEDICINE_REMINDER");

        Notification saved = captor.getValue();
        assertEquals(1, saved.getUserId(), "用户ID应匹配");
        assertEquals("用药提醒", saved.getTitle(), "标题应匹配");
        assertEquals("请按时服用阿司匹林", saved.getContent(), "内容应匹配");
        assertEquals("MEDICINE_REMINDER", saved.getType(), "类型应匹配");
        assertFalse(saved.getReadStatus(), "新通知应为未读状态");

        verify(webSocketPushService).pushToUser(eq(1), any());
    }

    // ========== 分页查询通知测试 ==========

    @Test
    @DisplayName("分页查询通知 - 有通知时返回列表")
    void getNotifications_HasNotifications_ReturnsList() {
        Page<Notification> page = new PageImpl<>(Arrays.asList(testNotification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(anyInt(), any(Pageable.class)))
                .thenReturn(page);

        List<?> response = notificationService.getNotifications(0, 10);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.size(), "应有1条通知");
    }

    @Test
    @DisplayName("分页查询通知 - 无通知时返回空列表")
    void getNotifications_NoNotifications_ReturnsEmptyList() {
        Page<Notification> emptyPage = new PageImpl<>(Collections.emptyList());
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(anyInt(), any(Pageable.class)))
                .thenReturn(emptyPage);

        List<?> response = notificationService.getNotifications(0, 10);

        assertNotNull(response, "响应不应为空");
        assertEquals(0, response.size(), "应无通知");
    }

    // ========== 通知统计测试 ==========

    @Test
    @DisplayName("统计通知 - 有通知时返回正确的总数和未读数")
    void getNotificationStats_HasNotifications_ReturnsCorrectStats() {
        when(notificationRepository.countStatsByUserId(1))
                .thenReturn(Collections.singletonList(new Object[]{5L, 3L}));

        Map<String, Object> stats = notificationService.getNotificationStats();

        assertNotNull(stats, "统计结果不应为空");
        assertEquals(5L, stats.get("totalCount"), "总数应为5");
        assertEquals(3L, stats.get("unreadCount"), "未读数应为3");
    }

    @Test
    @DisplayName("统计通知 - 无通知时返回零值")
    void getNotificationStats_NoNotifications_ReturnsZero() {
        when(notificationRepository.countStatsByUserId(1))
                .thenReturn(Collections.emptyList());

        Map<String, Object> stats = notificationService.getNotificationStats();

        assertNotNull(stats, "统计结果不应为空");
        assertEquals(0L, stats.get("totalCount"), "总数应为0");
        assertEquals(0L, stats.get("unreadCount"), "未读数应为0");
    }

    @Test
    @DisplayName("统计通知 - 聚合结果为 null 时返回零值")
    void getNotificationStats_NullAggregationResult_ReturnsZero() {
        when(notificationRepository.countStatsByUserId(1))
                .thenReturn(Collections.singletonList(new Object[]{null, null}));

        Map<String, Object> stats = notificationService.getNotificationStats();

        assertNotNull(stats, "统计结果不应为空");
        assertEquals(0L, stats.get("totalCount"), "总数应为0");
        assertEquals(0L, stats.get("unreadCount"), "未读数应为0");
    }

    // ========== 标记单条已读测试 ==========

    @Test
    @DisplayName("标记单条已读 - 通知存在且属于当前用户时成功标记")
    void markAsRead_NotificationExists_MarksAsRead() {
        when(notificationRepository.findById(1)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.markAsRead(1);

        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketPushService).syncNotificationState(eq(1), eq("READ"));
    }

    @Test
    @DisplayName("标记单条已读 - 通知不存在时抛出 ResourceNotFoundException")
    void markAsRead_NotificationNotFound_ThrowsResourceNotFoundException() {
        when(notificationRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(999),
                "不存在的通知应抛出异常");
    }

    @Test
    @DisplayName("标记单条已读 - 通知不属于当前用户时抛出 SecurityException")
    void markAsRead_NotificationNotOwned_ThrowsSecurityException() {
        Notification otherUserNotification = new Notification();
        otherUserNotification.setId(1);
        otherUserNotification.setUserId(999); // 属于其他用户
        otherUserNotification.setTitle("其他用户的通知");

        when(notificationRepository.findById(1)).thenReturn(Optional.of(otherUserNotification));

        assertThrows(SecurityException.class,
                () -> notificationService.markAsRead(1),
                "无权操作的通知应抛出异常");
    }

    // ========== 批量标记已读测试 ==========

    @Test
    @DisplayName("批量标记已读 - 有未读通知时批量更新并推送")
    void markAllAsRead_HasUnreadNotifications_BatchUpdates() {
        when(notificationRepository.markAllAsReadByUserId(eq(1), any(LocalDateTime.class)))
                .thenReturn(5);

        notificationService.markAllAsRead();

        verify(notificationRepository).markAllAsReadByUserId(eq(1), any(LocalDateTime.class));
        verify(webSocketPushService).syncNotificationState(eq(1), eq("READ_ALL"));
    }

    @Test
    @DisplayName("批量标记已读 - 无未读通知时批量更新返回0")
    void markAllAsRead_NoUnreadNotifications_ReturnsZero() {
        when(notificationRepository.markAllAsReadByUserId(eq(1), any(LocalDateTime.class)))
                .thenReturn(0);

        notificationService.markAllAsRead();

        verify(notificationRepository).markAllAsReadByUserId(eq(1), any(LocalDateTime.class));
        verify(webSocketPushService).syncNotificationState(eq(1), eq("READ_ALL"));
    }

    // ========== 删除单条通知测试 ==========

    @Test
    @DisplayName("删除单条通知 - 通知存在时成功删除")
    void deleteNotification_NotificationExists_DeletesSuccessfully() {
        when(notificationRepository.findById(1)).thenReturn(Optional.of(testNotification));
        doNothing().when(notificationRepository).delete(any(Notification.class));

        notificationService.deleteNotification(1);

        verify(notificationRepository).delete(any(Notification.class));
        verify(webSocketPushService).syncNotificationState(eq(1), eq("DELETE"));
    }

    @Test
    @DisplayName("删除单条通知 - 通知不存在时抛出 ResourceNotFoundException")
    void deleteNotification_NotificationNotFound_ThrowsResourceNotFoundException() {
        when(notificationRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.deleteNotification(999),
                "不存在的通知应抛出异常");
    }

    @Test
    @DisplayName("删除单条通知 - 通知不属于当前用户时抛出 SecurityException")
    void deleteNotification_NotificationNotOwned_ThrowsSecurityException() {
        Notification otherUserNotification = new Notification();
        otherUserNotification.setId(1);
        otherUserNotification.setUserId(999);

        when(notificationRepository.findById(1)).thenReturn(Optional.of(otherUserNotification));

        assertThrows(SecurityException.class,
                () -> notificationService.deleteNotification(1),
                "无权操作的通知应抛出异常");
    }

    // ========== 删除所有通知测试 ==========

    @Test
    @DisplayName("删除所有通知 - 成功删除当前用户的所有通知")
    void deleteAllNotifications_DeletesAllAndPushes() {
        doNothing().when(notificationRepository).deleteByUserId(1);

        notificationService.deleteAllNotifications();

        verify(notificationRepository).deleteByUserId(1);
        verify(webSocketPushService).syncNotificationState(eq(1), eq("DELETE_ALL"));
    }
}