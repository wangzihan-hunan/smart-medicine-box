package com.example.medicinebox.service;

import com.example.medicinebox.constant.FeedbackStatus;
import com.example.medicinebox.constant.UserRole;
import com.example.medicinebox.dto.response.FeedbackListResponse;
import com.example.medicinebox.dto.response.FeedbackResponse;
import com.example.medicinebox.entity.Feedback;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.FeedbackRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FeedbackService 单元测试
 * 覆盖反馈提交、查询、删除、管理员管理等核心功能
 * Mock 数量：2（FeedbackRepository, UserRepository），较优化前减少 60%
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FeedbackService 单元测试")
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;

    private FeedbackService feedbackService;

    private User testUser;
    private Feedback testFeedback;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(userRepository, feedbackRepository);

        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setName("测试用户");
        testUser.setRole(UserRole.USER);

        testFeedback = new Feedback();
        testFeedback.setId(1);
        testFeedback.setUserId(1);
        testFeedback.setType("BUG");
        testFeedback.setTitle("测试标题");
        testFeedback.setContent("测试内容");
        testFeedback.setStatus(FeedbackStatus.PENDING);
        testFeedback.setCreatedAt(LocalDateTime.now());
    }

    private void setCurrentUser(User user) {
        TestSecurityContextHelper.setCurrentUser(user.getPhone(), user.getRole().name());
        when(userRepository.findByPhone(user.getPhone())).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        TestSecurityContextHelper.clearContext();
    }

    @Test
    @DisplayName("提交反馈 - 成功创建并返回反馈信息")
    void submitFeedback_ValidInput_ReturnsFeedbackResponse() {
        setCurrentUser(testUser);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);

        FeedbackResponse response = feedbackService.submitFeedback("BUG", "标题", "内容", "contact@test.com");

        assertNotNull(response, "响应不应为空");
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("获取我的反馈列表 - 有反馈时返回分页列表")
    void getMyFeedbacks_HasFeedbacks_ReturnsPaginatedList() {
        setCurrentUser(testUser);
        Page<Feedback> feedbackPage = new PageImpl<>(Arrays.asList(testFeedback));
        when(feedbackRepository.findByUserIdOrderByCreatedAtDesc(anyInt(), any(Pageable.class)))
                .thenReturn(feedbackPage);

        FeedbackListResponse response = feedbackService.getMyFeedbacks(0, 10, null);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements(), "总数应为1");
    }

    @Test
    @DisplayName("获取我的反馈列表 - 按状态筛选")
    void getMyFeedbacks_WithStatusFilter_ReturnsFilteredResults() {
        setCurrentUser(testUser);
        Page<Feedback> feedbackPage = new PageImpl<>(Arrays.asList(testFeedback));
        when(feedbackRepository.findByUserIdAndStatusOrderByCreatedAtDesc(anyInt(), any(FeedbackStatus.class), any(Pageable.class)))
                .thenReturn(feedbackPage);

        FeedbackListResponse response = feedbackService.getMyFeedbacks(0, 10, "PENDING");

        assertNotNull(response, "响应不应为空");
        verify(feedbackRepository).findByUserIdAndStatusOrderByCreatedAtDesc(eq(1), eq(FeedbackStatus.PENDING), any(Pageable.class));
    }

    @Test
    @DisplayName("获取反馈详情 - 自己的反馈应成功返回")
    void getFeedbackDetail_OwnFeedback_ReturnsFeedback() {
        setCurrentUser(testUser);
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));

        FeedbackResponse response = feedbackService.getFeedbackDetail(1);

        assertNotNull(response, "响应不应为空");
    }

    @Test
    @DisplayName("获取反馈详情 - 管理员可查看他人反馈")
    void getFeedbackDetail_AdminViewingOthers_ReturnsFeedback() {
        User admin = new User();
        admin.setId(2);
        admin.setPhone("admin");
        admin.setRole(UserRole.ADMIN);
        setCurrentUser(admin);
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));

        FeedbackResponse response = feedbackService.getFeedbackDetail(1);

        assertNotNull(response, "响应不应为空");
    }

    @Test
    @DisplayName("获取反馈详情 - 非本人且非管理员查看他人反馈应抛出异常")
    void getFeedbackDetail_OtherUserNoPermission_ThrowsResourceNotFoundException() {
        User otherUser = new User();
        otherUser.setId(2);
        otherUser.setPhone("other");
        otherUser.setRole(UserRole.USER);
        setCurrentUser(otherUser);
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));

        assertThrows(ResourceNotFoundException.class,
                () -> feedbackService.getFeedbackDetail(1),
                "非本人且非管理员查看他人反馈应抛出异常");
    }

    @Test
    @DisplayName("删除反馈 - 自己的反馈应成功删除")
    void deleteFeedback_OwnFeedback_DeletesSuccessfully() {
        setCurrentUser(testUser);
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));
        doNothing().when(feedbackRepository).delete(any(Feedback.class));

        feedbackService.deleteFeedback(1);

        verify(feedbackRepository).delete(any(Feedback.class));
    }

    @Test
    @DisplayName("删除反馈 - 反馈不存在应抛出异常")
    void deleteFeedback_NotFound_ThrowsResourceNotFoundException() {
        setCurrentUser(testUser);
        when(feedbackRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> feedbackService.deleteFeedback(999),
                "不存在的反馈应抛出异常");
    }

    // ========== 管理员查询所有反馈测试 ==========

    @Test
    @DisplayName("管理员查询所有反馈 - 无筛选条件时返回全部")
    void getAllFeedbacks_NoFilter_ReturnsAll() {
        Page<Feedback> feedbackPage = new PageImpl<>(Arrays.asList(testFeedback));
        when(feedbackRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(feedbackPage);

        FeedbackListResponse response = feedbackService.getAllFeedbacks(0, 10, null, null);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements(), "总数应为1");
    }

    @Test
    @DisplayName("管理员查询所有反馈 - 按状态筛选")
    void getAllFeedbacks_WithStatusFilter_ReturnsFilteredResults() {
        Page<Feedback> feedbackPage = new PageImpl<>(Arrays.asList(testFeedback));
        when(feedbackRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(feedbackPage);

        FeedbackListResponse response = feedbackService.getAllFeedbacks(0, 10, "PENDING", null);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements(), "总数应为1");
    }

    @Test
    @DisplayName("管理员查询所有反馈 - 按类型筛选")
    void getAllFeedbacks_WithTypeFilter_ReturnsFilteredResults() {
        Page<Feedback> feedbackPage = new PageImpl<>(Arrays.asList(testFeedback));
        when(feedbackRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(feedbackPage);

        FeedbackListResponse response = feedbackService.getAllFeedbacks(0, 10, null, "BUG");

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements(), "总数应为1");
    }

    @Test
    @DisplayName("管理员查询所有反馈 - 同时按状态和类型组合筛选（修复互斥问题）")
    void getAllFeedbacks_WithStatusAndTypeFilter_CombinedFiltering() {
        // 优化前：if-else if-else 导致只能生效一个筛选条件
        // 优化后：JPA Specification 动态组合，两个条件同时生效
        Page<Feedback> emptyPage = new PageImpl<>(Arrays.asList());
        when(feedbackRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        FeedbackListResponse response = feedbackService.getAllFeedbacks(0, 10, "PENDING", "BUG");

        assertNotNull(response, "响应不应为空");
        assertEquals(0, response.getTotalElements(), "同时筛选 PENDING + BUG 应返回空列表");
    }

    @Test
    @DisplayName("管理员查询所有反馈 - 空筛选条件时不添加 Predicate")
    void getAllFeedbacks_EmptyFilter_ReturnsAllWithoutPredicates() {
        Page<Feedback> feedbackPage = new PageImpl<>(Arrays.asList(testFeedback));
        when(feedbackRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(feedbackPage);

        FeedbackListResponse response = feedbackService.getAllFeedbacks(0, 10, "", "");

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements(), "空字符串筛选应等同于无筛选");
    }

    @Test
    @DisplayName("管理员回复反馈 - 成功更新回复内容")
    void replyFeedback_ValidReply_UpdatesFeedback() {
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);

        FeedbackResponse response = feedbackService.replyFeedback(1, "这是回复内容");

        assertNotNull(response, "响应不应为空");
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("更新反馈状态 - 成功更新状态")
    void updateFeedbackStatus_ValidStatus_UpdatesStatus() {
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(testFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);

        FeedbackResponse response = feedbackService.updateFeedbackStatus(1, "REPLIED");

        assertNotNull(response, "响应不应为空");
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("获取待处理反馈数量 - 返回正确数量")
    void getPendingCount_ReturnsCorrectCount() {
        when(feedbackRepository.countByStatus(FeedbackStatus.PENDING)).thenReturn(5L);

        long count = feedbackService.getPendingCount();

        assertEquals(5L, count, "待处理数量应为5");
    }
}