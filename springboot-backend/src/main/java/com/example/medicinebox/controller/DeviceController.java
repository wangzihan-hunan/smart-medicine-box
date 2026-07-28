package com.example.medicinebox.controller;

import com.example.medicinebox.dto.request.CommandCreateRequest;
import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.dto.response.CommandListResponse;
import com.example.medicinebox.dto.response.CommandResponse;
import com.example.medicinebox.service.CommandService;
import com.example.medicinebox.util.PaginationUtil;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备管理控制器
 * 提供智能药箱设备的指令下发、历史查询、设备列表和详情等功能
 * 路由前缀：/api/v1/device
 */
@RestController
@RequestMapping("/api/v1/device")
public class DeviceController {

    private final CommandService commandService;

    public DeviceController(CommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * 向设备下发指令接口
     * HTTP方法：POST
     * 权限要求：已登录用户
     * 
     * @param request 指令创建请求体，包含设备ID、动作类型等信息
     * @return 指令下发结果，包含指令详细信息
     */
    @PostMapping("/forward")
    public BaseResponse<CommandResponse> forwardCommand(@Valid @RequestBody CommandCreateRequest request) {
        CommandResponse response = commandService.forwardCommand(request);
        return BaseResponse.success("成功向药箱 " + request.getDeviceId() + " 发送指令：" + request.getAction(), response);
    }

    /**
     * 获取指令历史记录接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @return 指令历史列表响应，包含总数和指令信息列表
     */
    @GetMapping("/history")
    public BaseResponse<CommandListResponse> getCommandHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        CommandListResponse response = commandService.getCommandHistory(validatedPage, validatedSize);
        return BaseResponse.success("获取历史指令成功", response);
    }

    /**
     * 执行指定指令接口
     * HTTP方法：POST
     * 权限要求：已登录用户
     * 
     * @param id 指令ID
     * @return 指令执行结果，包含指令详细信息
     */
    @PostMapping("/execute/{id}")
    public BaseResponse<CommandResponse> executeCommand(@PathVariable Integer id) {
        CommandResponse response = commandService.executeCommand(id);
        return BaseResponse.success("指令执行成功", response);
    }

    /**
     * 删除指令接口
     * HTTP方法：DELETE
     * 权限要求：已登录用户
     * 
     * @param id 指令ID
     * @return 操作结果，无返回数据
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteCommand(@PathVariable Integer id) {
        commandService.deleteCommand(id);
        return BaseResponse.success("删除指令成功！");
    }

    /**
     * 获取用户绑定的设备列表接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @return 设备列表，每个设备包含设备ID、名称等信息
     */
    @GetMapping("/list")
    public BaseResponse<List<Map<String, Object>>> getDevices() {
        List<Map<String, Object>> devices = commandService.getDevices();
        return BaseResponse.success("获取设备列表成功", devices);
    }

    /**
     * 获取设备详情接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @param deviceId 设备ID
     * @return 设备详细信息，包含设备状态、配置等
     */
    @GetMapping("/{deviceId}")
    public BaseResponse<Map<String, Object>> getDeviceDetail(@PathVariable String deviceId) {
        Map<String, Object> detail = commandService.getDeviceDetail(deviceId);
        return BaseResponse.success("获取设备详情成功", detail);
    }
}
