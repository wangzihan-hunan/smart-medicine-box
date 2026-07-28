package com.example.medicinebox.controller;

import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.dto.response.SystemLogListResponse;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.service.SystemLogService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 系统日志控制器
 * 提供用户查询自己的系统日志记录功能
 * 路由前缀：/api/v1/logs
 */
@RestController
@RequestMapping("/api/v1/logs")
public class SystemLogController {

    private final SystemLogService systemLogService;

    public SystemLogController(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    /**
     * 获取当前登录用户
     * 私有辅助方法，从SecurityContext中获取当前登录用户实体
     * 
     * @return 当前用户实体
     */
    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return systemLogService.getUserByPhone(userDetails.getUsername());
    }

    /**
     * 获取最近10条日志接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @return 最近10条系统日志列表
     */
    @GetMapping("/recent")
    public BaseResponse<SystemLogListResponse> getRecentLogs() {
        User user = getCurrentUser();
        SystemLogListResponse response = systemLogService.getTop10LogsByUserId(user.getId());
        return BaseResponse.success("获取最近日志成功", response);
    }

    /**
     * 分页查询系统日志接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @return 日志列表响应，包含总数和日志信息列表
     */
    @GetMapping
    public BaseResponse<SystemLogListResponse> getLogs(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        User user = getCurrentUser();
        SystemLogListResponse response = systemLogService.getLogsByUserId(user.getId(), page, size);
        return BaseResponse.success("获取日志列表成功", response);
    }

    /**
     * 按模块查询系统日志接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @param module 日志模块名称
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @return 指定模块的日志列表响应
     */
    @GetMapping("/module/{module}")
    public BaseResponse<SystemLogListResponse> getLogsByModule(
            @PathVariable String module,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        User user = getCurrentUser();
        SystemLogListResponse response = systemLogService.getLogsByModuleAndUserId(module, user.getId(), page, size);
        return BaseResponse.success("获取" + module + "模块日志成功", response);
    }
}
