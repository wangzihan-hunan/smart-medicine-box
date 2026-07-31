package com.example.medicinebox.service;

import com.example.medicinebox.dto.request.ReminderCreateRequest;
import com.example.medicinebox.dto.response.ReminderListResponse;
import com.example.medicinebox.dto.response.ReminderResponse;
import com.example.medicinebox.entity.Reminder;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.ReminderRepository;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.strategy.RepeatStrategyFactory;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ReminderService 单元测试
 * 覆盖提醒增删改查、标记已服用、定时触发、重复结束条件等核心功能
 * Mock 数量：2（ReminderRepository, UserRepository），较优化前减少 60%
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReminderService 单元测试")
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RepeatStrategyFactory repeatStrategyFactory;

    private ReminderService reminderService;

    private User testUser;
    private Reminder testReminder;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService(reminderRepository, userRepository, repeatStrategyFactory);

        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setName("测试用户");

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

        // 默认策略：每日重复
        when(repeatStrategyFactory.calculateNext(anyString(), any()))
                .thenAnswer(inv -> {
                    java.time.LocalDateTime dt = inv.getArgument(1);
                    return dt != null ? dt.plusDays(1) : java.time.LocalDateTime.now().plusDays(1);
                });

        TestSecurityContextHelper.setCurrentUser(testUser.getPhone(), "USER");
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        TestSecurityContextHelper.clearContext();
    }

    // ========== 提醒 CRUD 测试 ==========

    @Test
    @DisplayName("添加提醒 - 成功创建并返回提醒信息")
    void addReminder_ValidRequest_ReturnsReminderWithCorrectFields() {
        ReminderCreateRequest request = new ReminderCreateRequest();
        request.setMedicineId(100);
        request.setMedicineName("阿司匹林");
        request.setTime("08:00");
        request.setDosage("1片");
        request.setFrequency("每日一次");

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        when(reminderRepository.save(captor.capture())).thenReturn(testReminder);

        ReminderResponse response = reminderService.addReminder(request);

        assertNotNull(response, "响应不应为空");
        assertEquals("阿司匹林", response.getMedicineName(), "药品名称应匹配");
        Reminder saved = captor.getValue();
        assertEquals("阿司匹林", saved.getMedicineName(), "保存的药品名称应匹配");
        assertEquals("1片", saved.getDosage(), "保存的剂量应匹配");
        assertEquals(1, saved.getUserId(), "用户ID应匹配");
    }

    @Test
    @DisplayName("获取今日提醒 - 有提醒时返回分页列表")
    void getTodayReminders_HasReminders_ReturnsPaginatedList() {
        Page<Reminder> reminderPage = new PageImpl<>(Arrays.asList(testReminder));
        when(reminderRepository.findByUserIdAndDate(anyInt(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(reminderPage);

        ReminderListResponse response = reminderService.getTodayReminders(0, 10);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements(), "总数应为1");
    }

    @Test
    @DisplayName("获取今日提醒 - 无提醒时返回空列表")
    void getTodayReminders_NoReminders_ReturnsEmptyList() {
        Page<Reminder> emptyPage = new PageImpl<>(Collections.emptyList());
        when(reminderRepository.findByUserIdAndDate(anyInt(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        ReminderListResponse response = reminderService.getTodayReminders(0, 10);

        assertNotNull(response, "响应不应为空");
        assertEquals(0, response.getTotalElements(), "总数应为0");
    }

    @Test
    @DisplayName("标记已服用 - 提醒存在时成功标记")
    void markTaken_ReminderExists_MarksAsTaken() {
        when(reminderRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testReminder));
        when(reminderRepository.save(any(Reminder.class))).thenReturn(testReminder);

        ReminderResponse response = reminderService.markTaken(1);

        assertNotNull(response, "响应不应为空");
        verify(reminderRepository).save(any(Reminder.class));
    }

    @Test
    @DisplayName("标记已服用 - 提醒不存在时抛出 ResourceNotFoundException")
    void markTaken_ReminderNotFound_ThrowsResourceNotFoundException() {
        when(reminderRepository.findByIdAndUserId(999, 1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reminderService.markTaken(999),
                "不存在的提醒应抛出异常");
    }

    @Test
    @DisplayName("更新提醒 - 成功更新指定字段")
    void updateReminder_ValidRequest_UpdatesFields() {
        ReminderCreateRequest request = new ReminderCreateRequest();
        request.setMedicineName("布洛芬");
        request.setDosage("2片");

        when(reminderRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testReminder));
        when(reminderRepository.save(any(Reminder.class))).thenReturn(testReminder);

        ReminderResponse response = reminderService.updateReminder(1, request);

        assertNotNull(response, "响应不应为空");
        verify(reminderRepository).save(any(Reminder.class));
    }

    @Test
    @DisplayName("更新提醒 - 仅更新非空字段，保持原值不变")
    void updateReminder_PartialUpdate_OnlyUpdatesNonNullFields() {
        ReminderCreateRequest request = new ReminderCreateRequest();
        request.setMedicineName("布洛芬");

        when(reminderRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testReminder));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder saved = invocation.getArgument(0);
            assertEquals("布洛芬", saved.getMedicineName(), "药品名称应更新");
            assertEquals("08:00", saved.getTime(), "时间应保持原值");
            return saved;
        });

        ReminderResponse response = reminderService.updateReminder(1, request);

        assertNotNull(response, "响应不应为空");
    }

    @Test
    @DisplayName("删除提醒 - 提醒存在时成功删除")
    void deleteReminder_ReminderExists_DeletesSuccessfully() {
        when(reminderRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testReminder));
        doNothing().when(reminderRepository).delete(any(Reminder.class));

        reminderService.deleteReminder(1);

        verify(reminderRepository).delete(any(Reminder.class));
    }

    @Test
    @DisplayName("删除提醒 - 提醒不存在时抛出 ResourceNotFoundException")
    void deleteReminder_ReminderNotFound_ThrowsResourceNotFoundException() {
        when(reminderRepository.findByIdAndUserId(999, 1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reminderService.deleteReminder(999),
                "不存在的提醒应抛出异常");
    }

    // ========== 定时触发提醒测试 ==========

    /**
     * 创建分页查询 Mock 的辅助方法
     */
    @SuppressWarnings("unchecked")
    private void mockTriggerQuery(List<Reminder> reminders) {
        Page<Reminder> page = new PageImpl<>(reminders, PageRequest.of(0, 100), reminders.size());
        when(reminderRepository
                .findByTimeAndIsActiveTrueAndTakenFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        any(String.class), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page);
    }

    @Test
    @DisplayName("triggerReminders - 匹配当前时间的未服用提醒应被触发")
    void triggerReminders_MatchingTimeAndNotTaken_Triggers() {
        String currentTime = LocalTime.now().withSecond(0).withNano(0)
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        testReminder.setTime(currentTime);
        testReminder.setTaken(false);

        mockTriggerQuery(Arrays.asList(testReminder));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        reminderService.triggerReminders();

        verify(reminderRepository).save(any(Reminder.class));
    }

    @Test
    @DisplayName("triggerReminders - 无提醒时正常执行不报错")
    void triggerReminders_NoReminders_NoError() {
        mockTriggerQuery(Collections.emptyList());

        assertDoesNotThrow(() -> reminderService.triggerReminders(),
                "无提醒时不应抛出异常");
    }

    @Test
    @DisplayName("triggerReminders - 重复提醒触发后更新下次提醒时间")
    void triggerReminders_RepeatReminder_UpdatesNextRemindTime() {
        String currentTime = LocalTime.now().withSecond(0).withNano(0)
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        testReminder.setTime(currentTime);
        testReminder.setTaken(false);
        testReminder.setIsRepeat(true);
        testReminder.setNextRemindTime(LocalDateTime.now());

        mockTriggerQuery(Arrays.asList(testReminder));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        reminderService.triggerReminders();

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(captor.capture());
        assertNotNull(captor.getValue().getNextRemindTime(), "重复提醒应更新下次提醒时间");
        assertFalse(captor.getValue().getTaken(), "重复提醒不应标记为已服用");
    }

    @Test
    @DisplayName("triggerReminders - 分页查询多页数据全部处理")
    void triggerReminders_Pagination_ProcessesAllPages() {
        String currentTime = LocalTime.now().withSecond(0).withNano(0)
                .format(DateTimeFormatter.ofPattern("HH:mm"));

        // 创建150条提醒（超过100的批次大小，触发分页）
        List<Reminder> firstPageReminders = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Reminder r = new Reminder();
            r.setId(i);
            r.setUserId(1);
            r.setMedicineName("药品" + i);
            r.setTime(currentTime);
            r.setTaken(false);
            r.setIsRepeat(false);
            firstPageReminders.add(r);
        }
        List<Reminder> secondPageReminders = new ArrayList<>();
        for (int i = 100; i < 150; i++) {
            Reminder r = new Reminder();
            r.setId(i);
            r.setUserId(1);
            r.setMedicineName("药品" + i);
            r.setTime(currentTime);
            r.setTaken(false);
            r.setIsRepeat(false);
            secondPageReminders.add(r);
        }

        Page<Reminder> firstPage = new PageImpl<>(firstPageReminders, PageRequest.of(0, 100), 150);
        Page<Reminder> secondPage = new PageImpl<>(secondPageReminders, PageRequest.of(1, 100), 150);

        when(reminderRepository
                .findByTimeAndIsActiveTrueAndTakenFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(currentTime), any(LocalDate.class), any(LocalDate.class), eq(PageRequest.of(0, 100))))
                .thenReturn(firstPage);
        when(reminderRepository
                .findByTimeAndIsActiveTrueAndTakenFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(currentTime), any(LocalDate.class), any(LocalDate.class), eq(PageRequest.of(1, 100))))
                .thenReturn(secondPage);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        reminderService.triggerReminders();

        // 150条提醒全部保存
        verify(reminderRepository, times(150)).save(any(Reminder.class));
    }

    // ========== 重复提醒结束条件测试（实体领域方法） ==========

    @Test
    @DisplayName("shouldStopRepeat - 非重复提醒返回 false")
    void shouldStopRepeat_NonRepeat_ReturnsFalse() {
        testReminder.setIsRepeat(false);

        boolean result = testReminder.shouldStopRepeat();

        assertFalse(result, "非重复提醒不应结束");
    }

    @Test
    @DisplayName("shouldStopRepeat - 按次数结束：达到上限后停用并返回 true")
    void shouldStopRepeat_CountReached_DeactivatesReminder() {
        Reminder reminder = new Reminder();
        reminder.setId(1);
        reminder.setIsRepeat(true);
        reminder.setRepeatEndType("count");
        reminder.setRepeatCount(3);
        reminder.setTriggeredCount(2); // 已触发2次，本次是第3次

        boolean result = reminder.shouldStopRepeat();

        assertTrue(result, "达到次数上限应返回 true");
        assertFalse(reminder.getIsActive(), "提醒应被停用");
        assertEquals(3, reminder.getTriggeredCount(), "触发次数应递增到3");
    }

    @Test
    @DisplayName("shouldStopRepeat - 按次数结束：未达上限时继续触发")
    void shouldStopRepeat_CountNotReached_Continues() {
        Reminder reminder = new Reminder();
        reminder.setId(1);
        reminder.setIsRepeat(true);
        reminder.setRepeatEndType("count");
        reminder.setRepeatCount(5);
        reminder.setTriggeredCount(1);

        boolean result = reminder.shouldStopRepeat();

        assertFalse(result, "未达次数上限应返回 false");
        assertTrue(reminder.getIsActive(), "提醒应保持激活");
        assertEquals(2, reminder.getTriggeredCount(), "触发次数应递增到2");
    }

    @Test
    @DisplayName("shouldStopRepeat - 按日期结束：超过结束日期后停用并返回 true")
    void shouldStopRepeat_EndDatePassed_DeactivatesReminder() {
        Reminder reminder = new Reminder();
        reminder.setId(1);
        reminder.setIsRepeat(true);
        reminder.setRepeatEndType("endDate");
        reminder.setRepeatEndDate(LocalDate.now().minusDays(1));

        boolean result = reminder.shouldStopRepeat();

        assertTrue(result, "超过结束日期应返回 true");
        assertFalse(reminder.getIsActive(), "提醒应被停用");
    }

    @Test
    @DisplayName("shouldStopRepeat - 按日期结束：未到结束日期时继续触发")
    void shouldStopRepeat_EndDateNotPassed_Continues() {
        Reminder reminder = new Reminder();
        reminder.setId(1);
        reminder.setIsRepeat(true);
        reminder.setRepeatEndType("endDate");
        reminder.setRepeatEndDate(LocalDate.now().plusDays(7));

        boolean result = reminder.shouldStopRepeat();

        assertFalse(result, "未到结束日期应返回 false");
        assertTrue(reminder.getIsActive(), "提醒应保持激活");
    }

    @Test
    @DisplayName("shouldStopRepeat - 永不结束的重复提醒返回 false")
    void shouldStopRepeat_NeverEnds_ReturnsFalse() {
        Reminder reminder = new Reminder();
        reminder.setId(1);
        reminder.setIsRepeat(true);
        reminder.setRepeatEndType("never");

        boolean result = reminder.shouldStopRepeat();

        assertFalse(result, "永不结束的提醒应返回 false");
    }

    @Test
    @DisplayName("shouldStopRepeat - triggeredCount 为 null 时默认为 0")
    void shouldStopRepeat_NullTriggeredCount_DefaultsToZero() {
        Reminder reminder = new Reminder();
        reminder.setId(1);
        reminder.setIsRepeat(true);
        reminder.setRepeatEndType("count");
        reminder.setRepeatCount(1);
        reminder.setTriggeredCount(null); // null 应视为 0

        boolean result = reminder.shouldStopRepeat();

        assertTrue(result, "triggeredCount=null 视为0，第1次触发即达到上限");
        assertEquals(1, reminder.getTriggeredCount(), "触发次数应设为1");
    }
}