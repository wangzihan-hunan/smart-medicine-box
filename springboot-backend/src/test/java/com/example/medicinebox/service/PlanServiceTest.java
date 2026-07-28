package com.example.medicinebox.service;

import com.example.medicinebox.entity.User;
import com.example.medicinebox.enums.PlanType;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.service.PlanService.PlanInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PlanService 单元测试类
 * 测试套餐管理功能
 */
@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PlanService planService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setPlan("FREE");
        testUser.setMonthAiCount(0);
        testUser.setMonthAiLimit(10);
        testUser.setMonthResetAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("获取用户套餐 - 免费版用户")
    void getUserPlan_freeUserShouldReturnFree() {
        // Given
        testUser.setPlan("FREE");
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        PlanType plan = planService.getUserPlan(1);

        // Then
        assertEquals(PlanType.FREE, plan);
    }

    @Test
    @DisplayName("获取用户套餐 - 专业版用户")
    void getUserPlan_proUserShouldReturnProfessional() {
        // Given
        testUser.setPlan("PROFESSIONAL");
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        PlanType plan = planService.getUserPlan(1);

        // Then
        assertEquals(PlanType.PROFESSIONAL, plan);
    }

    @Test
    @DisplayName("获取用户套餐 - 用户不存在返回免费版")
    void getUserPlan_nonExistentUserShouldReturnFree() {
        // Given
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // When
        PlanType plan = planService.getUserPlan(999);

        // Then
        assertEquals(PlanType.FREE, plan);
    }

    @Test
    @DisplayName("检查套餐有效性 - 免费版永久有效")
    void hasActivePlan_freePlanAlwaysActive() {
        // Given
        testUser.setPlan("FREE");
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        boolean active = planService.hasActivePlan(1);

        // Then
        assertTrue(active);
    }

    @Test
    @DisplayName("检查套餐有效性 - 付费版未过期")
    void hasActivePlan_paidPlanNotExpired() {
        // Given
        testUser.setPlan("PROFESSIONAL");
        testUser.setPlanExpireAt(LocalDateTime.now().plusMonths(1));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        boolean active = planService.hasActivePlan(1);

        // Then
        assertTrue(active);
    }

    @Test
    @DisplayName("检查套餐有效性 - 付费版已过期")
    void hasActivePlan_paidPlanExpired() {
        // Given
        testUser.setPlan("PROFESSIONAL");
        testUser.setPlanExpireAt(LocalDateTime.now().minusDays(1));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        boolean active = planService.hasActivePlan(1);

        // Then
        assertFalse(active);
    }

    @Test
    @DisplayName("检查套餐有效性 - 用户不存在返回false")
    void hasActivePlan_nonExistentUserShouldReturnFalse() {
        // Given
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // When
        boolean active = planService.hasActivePlan(999);

        // Then
        assertFalse(active);
    }

    @Test
    @DisplayName("AI识别次数检查 - 免费版在限额内")
    void canUseAiRecognition_freePlanWithinLimit() {
        // Given
        testUser.setPlan("FREE");
        testUser.setMonthAiCount(5);
        testUser.setMonthAiLimit(10);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        boolean canUse = planService.canUseAiRecognition(1);

        // Then
        assertTrue(canUse);
    }

    @Test
    @DisplayName("AI识别次数检查 - 免费版超限")
    void canUseAiRecognition_freePlanExceededLimit() {
        // Given
        testUser.setPlan("FREE");
        testUser.setMonthAiCount(10);
        testUser.setMonthAiLimit(10);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        boolean canUse = planService.canUseAiRecognition(1);

        // Then
        assertFalse(canUse);
    }

    @Test
    @DisplayName("AI识别次数检查 - 专业版不限次数")
    void canUseAiRecognition_proPlanUnlimited() {
        // Given
        testUser.setPlan("PROFESSIONAL");
        testUser.setPlanExpireAt(LocalDateTime.now().plusMonths(1));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        boolean canUse = planService.canUseAiRecognition(1);

        // Then
        assertTrue(canUse);
    }

    @Test
    @DisplayName("消耗AI识别次数 - 成功")
    void consumeAiRecognition_shouldSucceed() {
        // Given
        testUser.setPlan("FREE");
        testUser.setMonthAiCount(5);
        testUser.setMonthAiLimit(10);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean result = planService.consumeAiRecognition(1);

        // Then
        assertTrue(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("消耗AI识别次数 - 超限应失败")
    void consumeAiRecognition_exceededShouldFail() {
        // Given
        testUser.setPlan("FREE");
        testUser.setMonthAiCount(10);
        testUser.setMonthAiLimit(10);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        boolean result = planService.consumeAiRecognition(1);

        // Then
        assertFalse(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("消耗AI识别次数 - 专业版不扣减")
    void consumeAiRecognition_proPlanNoDeduction() {
        // Given
        testUser.setPlan("PROFESSIONAL");
        testUser.setPlanExpireAt(LocalDateTime.now().plusMonths(1));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        boolean result = planService.consumeAiRecognition(1);

        // Then
        assertTrue(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("升级套餐 - 成功")
    void upgradePlan_shouldSucceed() {
        // Given
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        planService.upgradePlan(1, PlanType.PROFESSIONAL, 12);

        // Then
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("升级套餐 - 用户不存在应抛出异常")
    void upgradePlan_nonExistentUserShouldThrow() {
        // Given
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            planService.upgradePlan(999, PlanType.PROFESSIONAL, 12);
        });
    }

    @Test
    @DisplayName("获取剩余AI次数 - 免费版")
    void getRemainingAiCount_freePlan() {
        // Given
        testUser.setPlan("FREE");
        testUser.setMonthAiCount(3);
        testUser.setMonthAiLimit(10);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        int remaining = planService.getRemainingAiCount(1);

        // Then
        assertEquals(7, remaining);
    }

    @Test
    @DisplayName("获取剩余AI次数 - 专业版无限")
    void getRemainingAiCount_proPlanUnlimited() {
        // Given
        testUser.setPlan("PROFESSIONAL");
        testUser.setPlanExpireAt(LocalDateTime.now().plusMonths(1));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        int remaining = planService.getRemainingAiCount(1);

        // Then
        assertEquals(Integer.MAX_VALUE, remaining);
    }

    @Test
    @DisplayName("获取套餐详情 - 成功")
    void getPlanInfo_shouldSucceed() {
        // Given
        testUser.setPlan("FREE");
        testUser.setMonthAiCount(5);
        testUser.setMonthAiLimit(10);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        PlanInfo info = planService.getPlanInfo(1);

        // Then
        assertNotNull(info);
        assertEquals("FREE", info.getPlan());
        assertEquals("免费版", info.getPlanName());
        assertTrue(info.isActive());
    }

    @Test
    @DisplayName("获取套餐详情 - 用户不存在返回null")
    void getPlanInfo_nonExistentUserShouldReturnNull() {
        // Given
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // When
        PlanInfo info = planService.getPlanInfo(999);

        // Then
        assertNull(info);
    }
}