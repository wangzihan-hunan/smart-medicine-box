package com.example.medicinebox.service;

import com.example.medicinebox.dto.request.CommandCreateRequest;
import com.example.medicinebox.dto.response.CommandListResponse;
import com.example.medicinebox.dto.response.CommandResponse;
import com.example.medicinebox.entity.Command;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.CommandRepository;
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
    // 时间格式化器
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 构造函数，注入指令数据访问层
     *
     * @param commandRepository 指令数据访问层
     */
    public CommandService(CommandRepository commandRepository) {
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
        command.setStatus("已发送");
        command.setUserId(user.getId());

        command = commandRepository.save(command);

        log.debug("发送指令成功, 指令ID: {}", command.getId());
        return CommandResponse.fromEntity(command);
    }

    /**
     * 分页查询指令历史
     * 按时间倒序排列
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页指令列表响应
     */
    public CommandListResponse getCommandHistory(int page, int size) {
        User user = getCurrentUser();
        log.debug("查询指令列表, 用户ID: {}, 页码: {}, 大小: {}", user.getId(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "time"));
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
        
        command.setStatus("已执行");
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
     * 返回设备基本信息和指令统计数据
     *
     * @return 设备信息列表
     */
    public List<Map<String, Object>> getDevices() {
        User user = getCurrentUser();
        log.debug("查询设备列表, 用户ID: {}", user.getId());
        
        List<String> deviceIds = commandRepository.findDistinctDeviceIdsByUserId(user.getId());
        List<Map<String, Object>> devices = new ArrayList<>();
        
        for (String deviceId : deviceIds) {
            List<Command> commands = commandRepository.findByUserIdAndDeviceId(user.getId(), deviceId);
            if (commands.isEmpty()) continue;
            
            Map<String, Object> device = new HashMap<>();
            device.put("deviceId", deviceId);
            device.put("totalCommands", commands.size());
            device.put("lastCommandTime", commands.get(0).getTime());
            device.put("lastAction", commands.get(0).getAction());
            device.put("lastStatus", commands.get(0).getStatus());
            
            // 统计已执行指令数
            long executedCount = commands.stream()
                    .filter(c -> "已执行".equals(c.getStatus()))
                    .count();
            device.put("executedCommands", executedCount);
            
            devices.add(device);
        }
        
        log.debug("查询设备列表成功, 用户ID: {}, 设备数: {}", user.getId(), devices.size());
        return devices;
    }

    /**
     * 获取设备详情（包含最近指令）
     * 返回设备基本信息、指令统计和最近10条指令
     *
     * @param deviceId 设备ID
     * @return 设备详情Map
     * @throws ResourceNotFoundException 设备不存在或未绑定时抛出
     */
    public Map<String, Object> getDeviceDetail(String deviceId) {
        User user = getCurrentUser();
        log.debug("查询设备详情, 用户ID: {}, 设备ID: {}", user.getId(), deviceId);
        
        List<Command> commands = commandRepository.findByUserIdAndDeviceId(user.getId(), deviceId);
        if (commands.isEmpty()) {
            throw new ResourceNotFoundException("设备不存在或未绑定");
        }
        
        Map<String, Object> device = new HashMap<>();
        device.put("deviceId", deviceId);
        device.put("totalCommands", commands.size());
        device.put("lastCommandTime", commands.get(0).getTime());
        device.put("lastAction", commands.get(0).getAction());
        device.put("lastStatus", commands.get(0).getStatus());
        
        long executedCount = commands.stream()
                .filter(c -> "已执行".equals(c.getStatus()))
                .count();
        device.put("executedCommands", executedCount);
        
        // 最近10条指令
        List<CommandResponse> recentCommands = commands.stream()
                .limit(10)
                .map(CommandResponse::fromEntity)
                .collect(Collectors.toList());
        device.put("recentCommands", recentCommands);
        
        return device;
    }
}
