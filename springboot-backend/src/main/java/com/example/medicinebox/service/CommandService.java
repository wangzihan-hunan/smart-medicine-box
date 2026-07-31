package com.example.medicinebox.service;

import com.example.medicinebox.dto.request.CommandCreateRequest;
import com.example.medicinebox.dto.response.CommandListResponse;
import com.example.medicinebox.dto.response.CommandResponse;
import com.example.medicinebox.entity.Command;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.CommandRepository;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备指令服务类
 * 核心功能：设备指令下发、指令历史查询、指令执行、设备列表及详情查询等
 */
@Service
public class CommandService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(CommandService.class);

    private final CommandRepository commandRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CommandService(UserRepository userRepository, CommandRepository commandRepository) {
        super(userRepository);
        this.commandRepository = commandRepository;
    }

    /**
     * 下发设备指令
     * 创建新的指令记录并发送给指定设备
     *
     * @param request 指令创建请求（设备ID、动作、参数等）
     * @return 指令响应对象
     */
    @Transactional
    public CommandResponse forwardCommand(CommandCreateRequest request) {
        User user = getCurrentUser();
        log.debug("发送指令, 用户ID: {}, 设备ID: {}, 动作: {}", user.getId(), request.getDeviceId(), request.getAction());
        
        String currentTime = LocalDateTime.now().format(FORMATTER);

        Command command = new Command();
        command.setDeviceId(request.getDeviceId());
        command.setAction(request.getAction());
        command.setParams(request.getParams());
        command.setTime(currentTime);
        command.markSent();
        command.setUserId(user.getId());

        command = commandRepository.save(command);

        log.debug("发送指令成功, 指令ID: {}", command.getId());
        return CommandResponse.fromEntity(command);
    }

    /**
     * 分页查询指令历史
     * 按时间倒序排列，pageSize 上限 100
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页指令列表响应
     */
    @Transactional(readOnly = true)
    public CommandListResponse getCommandHistory(int page, int size) {
        User user = getCurrentUser();
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        log.debug("查询指令列表, 用户ID: {}, 页码: {}, 大小: {}", user.getId(), validatedPage, validatedSize);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "time"));
        Page<Command> commandPage = commandRepository.findByUserId(user.getId(), pageable);
        
        List<CommandResponse> responseList = commandPage.getContent().stream()
                .map(CommandResponse::fromEntity)
                .collect(Collectors.toList());

        log.debug("查询指令列表成功, 用户ID: {}, 总数: {}", user.getId(), commandPage.getTotalElements());
        return new CommandListResponse(
                (int) commandPage.getTotalElements(),
                commandPage.getTotalPages(),
                commandPage.getNumber(),
                responseList
        );
    }

    /**
     * 执行指令
     * 将指令状态更新为已执行
     *
     * @param id 指令ID
     * @return 更新后的指令响应对象
     * @throws ResourceNotFoundException 指令不存在或无权限时抛出
     */
    @Transactional
    public CommandResponse executeCommand(Integer id) {
        User user = getCurrentUser();
        log.debug("执行指令, 用户ID: {}, 指令ID: {}", user.getId(), id);
        
        Command command = commandRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> {
                    log.warn("指令不存在, 用户ID: {}, 指令ID: {}", user.getId(), id);
                    return new ResourceNotFoundException("指令不存在");
                });
        
        command.markExecuted();
        command = commandRepository.save(command);
        
        log.debug("执行指令成功, 指令ID: {}", command.getId());
        return CommandResponse.fromEntity(command);
    }

    /**
     * 删除指令
     * 校验指令归属权，只能删除自己的指令
     *
     * @param id 指令ID
     * @throws ResourceNotFoundException 指令不存在或无权限时抛出
     */
    @Transactional
    public void deleteCommand(Integer id) {
        User user = getCurrentUser();
        log.debug("删除指令, 用户ID: {}, 指令ID: {}", user.getId(), id);
        
        Command command = commandRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> {
                    log.warn("指令不存在或无权限删除, 用户ID: {}, 指令ID: {}", user.getId(), id);
                    return new ResourceNotFoundException("指令不存在或无权限删除");
                });
        
        commandRepository.delete(command);
        log.debug("删除指令成功, 指令ID: {}", id);
    }

    /**
     * 获取用户绑定的设备列表（从历史指令聚合）
     *
     * <p>使用 2 次查询（1次聚合统计 + 1次取最新指令），避免 N+1 查询问题。
     *
     * @return 设备信息列表
     */
    public List<Map<String, Object>> getDevices() {
        User user = getCurrentUser();
        log.debug("查询设备列表, 用户ID: {}", user.getId());
        
        // 查询1：聚合统计（一次查询替代 N 次全量加载）
        List<Object[]> deviceStats = commandRepository.findDeviceStatsByUserId(user.getId());
        
        // 查询2：每个设备的最新指令（用于获取 lastAction 和 lastStatus）
        List<Command> latestCommands = commandRepository.findLatestCommandsPerDevice(user.getId());
        Map<String, Command> latestCommandMap = new HashMap<>();
        for (Command cmd : latestCommands) {
            latestCommandMap.put(cmd.getDeviceId(), cmd);
        }
        
        List<Map<String, Object>> devices = new ArrayList<>();
        for (Object[] row : deviceStats) {
            String deviceId = (String) row[0];
            long totalCommands = ((Number) row[1]).longValue();
            long executedCommands = row[2] != null ? ((Number) row[2]).longValue() : 0;
            String lastCommandTime = (String) row[3];
            
            Map<String, Object> device = new HashMap<>();
            device.put("deviceId", deviceId);
            device.put("totalCommands", totalCommands);
            device.put("executedCommands", executedCommands);
            device.put("lastCommandTime", lastCommandTime);
            
            Command latest = latestCommandMap.get(deviceId);
            device.put("lastAction", latest != null ? latest.getAction() : null);
            device.put("lastStatus", latest != null ? latest.getStatus() : null);
            
            devices.add(device);
        }
        
        log.debug("查询设备列表成功, 用户ID: {}, 设备数: {}", user.getId(), devices.size());
        return devices;
    }

    /**
     * 获取设备详情（包含最近指令）
     *
     * <p>使用 3 次精准查询（统计 + 最新1条 + 最近10条），内存占用 O(1)。
     *
     * @param deviceId 设备ID
     * @return 设备详情Map
     * @throws ResourceNotFoundException 设备不存在或未绑定时抛出
     */
    public Map<String, Object> getDeviceDetail(String deviceId) {
        User user = getCurrentUser();
        log.debug("查询设备详情, 用户ID: {}, 设备ID: {}", user.getId(), deviceId);
        
        // 查询1：统计信息（COUNT + SUM，不加载指令实体）
        List<Object[]> statsResult = commandRepository.findDeviceStats(user.getId(), deviceId);
        if (statsResult.isEmpty() || statsResult.get(0)[0] == null || ((Number) statsResult.get(0)[0]).longValue() == 0) {
            throw new ResourceNotFoundException("设备不存在或未绑定");
        }
        Object[] stats = statsResult.get(0);
        long totalCommands = ((Number) stats[0]).longValue();
        long executedCommands = stats[1] != null ? ((Number) stats[1]).longValue() : 0;
        
        // 查询2：最新一条指令（获取 lastAction、lastStatus、lastCommandTime）
        List<Command> latestList = commandRepository.findTopByUserIdAndDeviceId(
                user.getId(), deviceId, PageRequest.of(0, 1));
        
        Map<String, Object> device = new HashMap<>();
        device.put("deviceId", deviceId);
        device.put("totalCommands", totalCommands);
        device.put("executedCommands", executedCommands);
        
        if (!latestList.isEmpty()) {
            Command latest = latestList.get(0);
            device.put("lastCommandTime", latest.getTime());
            device.put("lastAction", latest.getAction());
            device.put("lastStatus", latest.getStatus());
        }
        
        // 查询3：最近10条指令（分页查询，仅加载所需数据）
        List<Command> recentCommands = commandRepository.findTopByUserIdAndDeviceId(
                user.getId(), deviceId, PageRequest.of(0, 10));
        List<CommandResponse> recentList = recentCommands.stream()
                .map(CommandResponse::fromEntity)
                .collect(Collectors.toList());
        device.put("recentCommands", recentList);
        
        return device;
    }
}