package com.example.medicinebox.service;

import com.example.medicinebox.dto.response.FeedbackListResponse;
import com.example.medicinebox.dto.response.FeedbackResponse;
import com.example.medicinebox.entity.Feedback;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.FeedbackRepository;
import com.example.medicinebox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FeedbackService 单元测试类
 * 测试意见反馈功能
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private FeedbackService feedbackService;

    private User testUser;
    private Feedback testFeedback;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setName("测试用户");
        testUser.setRole("USER");

        testFeedback = new Feedback();
        testFeedback.setId(1);
        testFeedback.setUserId(1);
        testFeedback.setType("BUG");
        testFeedback.setTitle("测试标题");
        testFeedback.setContent("测试内容");
        testFeedback.setStatus("PENDING");
        testFeedback.setCreatedAt(LocalDateTime.now());
    }

    private void mockCurrentUser(User user) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(user.getPhone());
        when(userRepository.findByPhone(user.getPhone())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("提交反馈 - 成功")
    void submitFeedback_shouldSucceed() {
        // Given
        mockCurrentUser(testUser);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            FeedbackResponse response = feedbackService.submitFeedback("BUG", "标题", "内容", "contact@test.com");

            // Then
            assertNotNull(response);
            verify(feedbackRepository).save(any(Feedback.class));
        }
    }

    @Test
    @DisplayName("获取我的反馈列表 - 成功")
    void getMyFeedbacks_shouldSucceed() {
        // Given
        mockCurrentUser(testUser);
        Page<Feedback> feedbackPage = new PageImpl<>(Arrays.asList(testFeedback));
        when(feedbackRepository.findByUserIdOrderByCreatedAtDesc(anyInt(), any(Pageable.class)))
                .thenReturn(feedbackPage);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            FeedbackListResponse response = feedbackService.getMyFeedbacks(0, 10, null);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
        }
    }

    @Test
    @DisplayName("获取我的反馈列表 - 按状态筛选")
    void getMyFeedbacks_withStatusFilter() {
        // Given
        mockCurrentUser(testUser);
        Page<Feedback> feedbackPage = new PageImpl<>(Arrays.asList(testFeedback));
        when(feedbackRepository.findByUserIdAndStatusOrderByCreatedAtDesc(anyInt(), anyString(), any(Pageable.class)))
                .thenReturn(feedbackPage);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            FeedbackListResponse response = feedbackService.getMyFeedbacks(0, 10, "PENDING");

            // Then
            assertNotNull(response);
            verify(feedbackRepository).findByUserIdAndStatusOrderByCreatedAtDesc(eq(1), eq("PENDING"), any(Pageable.class));
        }
    }

    @Test
    @DisplayName("获取反馈详情 - 自己的反馈")
    void getFeedbackDetail_ownFeedback() {
        // Given
        mockCurrentUser(testUser);
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            FeedbackResponse response = feedbackService.getFeedbackDetail(1);

            // Then
            assertNotNull(response);
        }
    }

    @Test
    @DisplayName("获取反馈详情 - 管理员查看他人反馈")
    void getFeedbackDetail_adminViewOthers() {
        // Given
        User admin = new User();
        admin.setId(2);
        admin.setPhone("admin");
        admin.setRole("ADMIN");
        mockCurrentUser(admin);
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            FeedbackResponse response = feedbackService.getFeedbackDetail(1);

            // Then
            assertNotNull(response);
        }
    }

    @Test
    @DisplayName("获取反馈详情 - 无权限应抛出异常")
    void getFeedbackDetail_noPermissionShouldThrow() {
        // Given
        User otherUser = new User();
        otherUser.setId(2);
        otherUser.setPhone("other");
        otherUser.setRole("USER");
        mockCurrentUser(otherUser);
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                feedbackService.getFeedbackDetail(1);
            });
        }
    }

    @Test
    @DisplayName("删除反馈 - 成功")
    void deleteFeedback_shouldSucceed() {
        // Given
        mockCurrentUser(testUser);
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));
        doNothing().when(feedbackRepository).delete(any(Feedback.class));

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            feedbackService.deleteFeedback(1);

            // Then
            verify(feedbackRepository).delete(any(Feedback.class));
        }
    }

    @Test
    @DisplayName("删除反馈 - 不存在应抛出异常")
    void deleteFeedback_notFoundShouldThrow() {
        // Given
        mockCurrentUser(testUser);
        when(feedbackRepository.findById(999)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                feedbackService.deleteFeedback(999);
            });
        }
    }

    @Test
    @DisplayName("管理员查询所有反馈 - 成功")
    void getAllFeedbacks_shouldSucceed() {
        // Given
        Page<Feedback> feedbackPage = new PageImpl<>(Arrays.asList(testFeedback));
        when(feedbackRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(feedbackPage);

        // When
        FeedbackListResponse response = feedbackService.getAllFeedbacks(0, 10, null, null);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("管理员回复反馈 - 成功")
    void replyFeedback_shouldSucceed() {
        // Given
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);

        // When
        FeedbackResponse response = feedbackService.replyFeedback(1, "这是回复内容");

        // Then
        assertNotNull(response);
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("更新反馈状态 - 成功")
    void updateFeedbackStatus_shouldSucceed() {
        // Given
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);

        // When
        FeedbackResponse response = feedbackService.updateFeedbackStatus(1, "REPLIED");

        // Then
        assertNotNull(response);
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("获取待处理反馈数量")
    void getPendingCount_shouldReturnCount() {
        // Given
        when(feedbackRepository.countByStatus("PENDING")).thenReturn(5L);

        // When
        long count = feedbackService.getPendingCount();

        // Then
        assertEquals(5L, count);
    }
}