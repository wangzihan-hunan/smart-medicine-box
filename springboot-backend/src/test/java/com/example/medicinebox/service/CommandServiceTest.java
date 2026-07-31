package com.example.medicinebox.service;

import com.example.medicinebox.dto.request.CommandCreateRequest;
import com.example.medicinebox.dto.response.CommandListResponse;
import com.example.medicinebox.dto.response.CommandResponse;
import com.example.medicinebox.entity.Command;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.CommandRepository;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CommandService 单元测试
 * 覆盖指令下发、历史查询、执行、删除、设备列表和设备详情
 * Mock 数量：2（CommandRepository, UserRepository）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CommandService 单元测试")
class CommandServiceTest {

    @Mock
    private CommandRepository commandRepository;

    @Mock
    private UserRepository userRepository;

    private CommandService commandService;

    private User testUser;
    private Command testCommand;

    @BeforeEach
    void setUp() {
        commandService = new CommandService(userRepository, commandRepository);

        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setName("测试用户");

        testCommand = new Command();
        testCommand.setId(1);
        testCommand.setUserId(1);
        testCommand.setDeviceId("DEV001");
        testCommand.setAction("出药");
        testCommand.setParams("出药: 硝苯地平");
        testCommand.setTime("2026-07-30 10:00:00");
        testCommand.setStatus("已发送");

        TestSecurityContextHelper.setCurrentUser(testUser.getPhone(), "USER");
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        TestSecurityContextHelper.clearContext();
    }

    // ========== 指令下发测试 ==========

    @Test
    @DisplayName("下发指令 - 成功创建并返回指令信息")
    void forwardCommand_ValidRequest_ReturnsCommand() {
        CommandCreateRequest request = new CommandCreateRequest();
        request.setDeviceId("DEV001");
        request.setAction("出药");
        request.setParams("出药: 硝苯地平");

        ArgumentCaptor<Command> captor = ArgumentCaptor.forClass(Command.class);
        when(commandRepository.save(captor.capture())).thenReturn(testCommand);

        CommandResponse response = commandService.forwardCommand(request);

        assertNotNull(response, "响应不应为空");
        assertEquals("DEV001", response.getDeviceId(), "设备ID应匹配");
        assertEquals("出药", captor.getValue().getAction(), "指令动作应匹配");
        assertEquals("已发送", captor.getValue().getStatus(), "状态应为已发送");
    }

    // ========== 指令历史查询测试 ==========

    @Test
    @DisplayName("查询指令历史 - 有记录时返回分页列表")
    void getCommandHistory_HasRecords_ReturnsPaginatedList() {
        Page<Command> page = new PageImpl<>(Arrays.asList(testCommand));
        when(commandRepository.findByUserId(anyInt(), any(Pageable.class))).thenReturn(page);

        CommandListResponse response = commandService.getCommandHistory(0, 20);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements(), "总数应为1");
    }

    @Test
    @DisplayName("查询指令历史 - 无记录时返回空列表")
    void getCommandHistory_NoRecords_ReturnsEmptyList() {
        Page<Command> emptyPage = new PageImpl<>(Collections.emptyList());
        when(commandRepository.findByUserId(anyInt(), any(Pageable.class))).thenReturn(emptyPage);

        CommandListResponse response = commandService.getCommandHistory(0, 20);

        assertNotNull(response, "响应不应为空");
        assertEquals(0, response.getTotalElements(), "总数应为0");
    }

    @Test
    @DisplayName("查询指令历史 - pageSize 超过上限时自动限制为 100")
    void getCommandHistory_LargeSize_ClampedToMax() {
        Page<Command> page = new PageImpl<>(Collections.emptyList());
        when(commandRepository.findByUserId(anyInt(), any(Pageable.class))).thenReturn(page);

        commandService.getCommandHistory(0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(commandRepository).findByUserId(anyInt(), captor.capture());
        assertEquals(100, captor.getValue().getPageSize(), "pageSize 应被限制为 100");
    }

    // ========== 指令执行测试 ==========

    @Test
    @DisplayName("执行指令 - 指令存在时更新状态")
    void executeCommand_Exists_UpdatesStatus() {
        when(commandRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testCommand));
        when(commandRepository.save(any(Command.class))).thenReturn(testCommand);

        CommandResponse response = commandService.executeCommand(1);

        assertNotNull(response, "响应不应为空");
        assertEquals("已执行", testCommand.getStatus(), "状态应更新为已执行");
    }

    @Test
    @DisplayName("执行指令 - 指令不存在时抛出异常")
    void executeCommand_NotFound_ThrowsResourceNotFoundException() {
        when(commandRepository.findByIdAndUserId(999, 1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commandService.executeCommand(999),
                "不存在的指令应抛出异常");
    }

    // ========== 指令删除测试 ==========

    @Test
    @DisplayName("删除指令 - 指令存在时成功删除")
    void deleteCommand_Exists_DeletesSuccessfully() {
        when(commandRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testCommand));
        doNothing().when(commandRepository).delete(any(Command.class));

        assertDoesNotThrow(() -> commandService.deleteCommand(1),
                "删除存在的指令不应抛出异常");
        verify(commandRepository).delete(any(Command.class));
    }

    @Test
    @DisplayName("删除指令 - 指令不存在时抛出异常")
    void deleteCommand_NotFound_ThrowsResourceNotFoundException() {
        when(commandRepository.findByIdAndUserId(999, 1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commandService.deleteCommand(999),
                "不存在的指令应抛出异常");
    }

    // ========== 设备列表测试 ==========

    @Test
    @DisplayName("getDevices - 有设备时返回设备列表及统计信息")
    void getDevices_HasDevices_ReturnsDeviceList() {
        // Mock 聚合统计
        List<Object[]> stats = Arrays.asList(
                new Object[]{"DEV001", 10L, 8L, "2026-07-30 10:00:00"},
                new Object[]{"DEV002", 5L, 3L, "2026-07-29 15:00:00"}
        );
        when(commandRepository.findDeviceStatsByUserId(1)).thenReturn(stats);

        // Mock 每个设备的最新指令
        Command latest1 = new Command();
        latest1.setDeviceId("DEV001");
        latest1.setAction("出药");
        latest1.setStatus("已执行");
        Command latest2 = new Command();
        latest2.setDeviceId("DEV002");
        latest2.setAction("蜂鸣");
        latest2.setStatus("已发送");
        when(commandRepository.findLatestCommandsPerDevice(1))
                .thenReturn(Arrays.asList(latest1, latest2));

        List<Map<String, Object>> devices = commandService.getDevices();

        assertEquals(2, devices.size(), "应有2个设备");
        assertEquals("DEV001", devices.get(0).get("deviceId"), "设备ID应匹配");
        assertEquals(10L, devices.get(0).get("totalCommands"), "指令总数应匹配");
        assertEquals(8L, devices.get(0).get("executedCommands"), "已执行数应匹配");
        assertEquals("出药", devices.get(0).get("lastAction"), "最后动作应匹配");
    }

    @Test
    @DisplayName("getDevices - 无设备时返回空列表")
    void getDevices_NoDevices_ReturnsEmptyList() {
        when(commandRepository.findDeviceStatsByUserId(1)).thenReturn(Collections.emptyList());
        when(commandRepository.findLatestCommandsPerDevice(1)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> devices = commandService.getDevices();

        assertNotNull(devices, "返回列表不应为空");
        assertTrue(devices.isEmpty(), "设备列表应为空");
    }

    // ========== 设备详情测试 ==========

    @Test
    @DisplayName("getDeviceDetail - 设备存在时返回详情和最近10条指令")
    void getDeviceDetail_Exists_ReturnsDetail() {
        // Mock 统计信息
        List<Object[]> stats = Collections.singletonList(new Object[]{15L, 12L});
        when(commandRepository.findDeviceStats(1, "DEV001")).thenReturn(stats);

        // Mock 最新指令（pageSize=1）
        Command latest = new Command();
        latest.setDeviceId("DEV001");
        latest.setAction("蜂鸣");
        latest.setTime("2026-07-30 16:00:00");
        latest.setStatus("已执行");
        when(commandRepository.findTopByUserIdAndDeviceId(eq(1), eq("DEV001"), eq(PageRequest.of(0, 1))))
                .thenReturn(Arrays.asList(latest));

        // Mock 最近10条指令（pageSize=10）
        when(commandRepository.findTopByUserIdAndDeviceId(eq(1), eq("DEV001"), eq(PageRequest.of(0, 10))))
                .thenReturn(Arrays.asList(latest));

        Map<String, Object> detail = commandService.getDeviceDetail("DEV001");

        assertEquals("DEV001", detail.get("deviceId"), "设备ID应匹配");
        assertEquals(15L, detail.get("totalCommands"), "指令总数应匹配");
        assertEquals(12L, detail.get("executedCommands"), "已执行数应匹配");
        assertEquals("蜂鸣", detail.get("lastAction"), "最后动作应匹配");
        assertNotNull(detail.get("recentCommands"), "最近指令不应为空");
    }

    @Test
    @DisplayName("getDeviceDetail - 设备不存在时抛出异常")
    void getDeviceDetail_NotFound_ThrowsResourceNotFoundException() {
        when(commandRepository.findDeviceStats(1, "DEV999"))
                .thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class,
                () -> commandService.getDeviceDetail("DEV999"),
                "不存在的设备应抛出异常");
    }

    @Test
    @DisplayName("getDeviceDetail - 设备无指令时抛出异常")
    void getDeviceDetail_NoCommands_ThrowsResourceNotFoundException() {
        // COUNT 返回 0
        List<Object[]> stats = Collections.singletonList(new Object[]{0L, null});
        when(commandRepository.findDeviceStats(1, "DEV001")).thenReturn(stats);

        assertThrows(ResourceNotFoundException.class,
                () -> commandService.getDeviceDetail("DEV001"),
                "无指令的设备应抛出异常");
    }
}