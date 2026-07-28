package com.example.medicinebox.controller;

import com.example.medicinebox.dto.request.ReminderCreateRequest;
import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.dto.response.ReminderListResponse;
import com.example.medicinebox.dto.response.ReminderResponse;
import com.example.medicinebox.service.ReminderService;
import com.example.medicinebox.util.PaginationUtil;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用药提醒控制器
 * 管理用药提醒的创建、查询、更新、删除以及服药标记等功能
 * 路由前缀：/api/v1/medication
 */
@RestController
@RequestMapping("/api/v1/medication")
public class MedicationController {

    private final ReminderService reminderService;

    public MedicationController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /**
     * 添加用药提醒接口
     * HTTP方法：POST
     * 权限要求：已登录用户
     * 
     * @param request 提醒创建请求体，包含药品名称、服药时间、剂量等信息
     * @return 创建成功的提醒详细信息
     */
    @PostMapping("/reminders")
    public BaseResponse<ReminderResponse> addReminder(@Valid @RequestBody ReminderCreateRequest request) {
        ReminderResponse response = reminderService.addReminder(request);
        return BaseResponse.success("成功添加 " + request.getTime() + " 服用 " + request.getMedicineName() + " 的提醒！", response);
    }

    /**
     * 获取今日用药提醒列表接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @return 今日提醒列表响应，包含总数和提醒信息列表
     */
    @GetMapping("/today")
    public BaseResponse<ReminderListResponse> getTodayReminders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        ReminderListResponse response = reminderService.getTodayReminders(validatedPage, validatedSize);
        return BaseResponse.success("获取今日提醒成功", response);
    }

    /**
     * 标记药品已服用接口
     * HTTP方法：POST
     * 权限要求：已登录用户
     * 
     * @param id 提醒ID
     * @return 更新后的提醒详细信息
     */
    @PostMapping("/reminders/{id}/mark-taken")
    public BaseResponse<ReminderResponse> markTaken(@PathVariable Integer id) {
        ReminderResponse response = reminderService.markTaken(id);
        return BaseResponse.success("已标记为已服药", response);
    }

    /**
     * 更新用药提醒接口
     * HTTP方法：PUT
     * 权限要求：已登录用户
     * 
     * @param id 提醒ID
     * @param request 提醒更新请求体，包含新的提醒信息
     * @return 更新后的提醒详细信息
     */
    @PutMapping("/reminders/{id}")
    public BaseResponse<ReminderResponse> updateReminder(@PathVariable Integer id, @RequestBody ReminderCreateRequest request) {
        ReminderResponse response = reminderService.updateReminder(id, request);
        return BaseResponse.success("更新提醒成功", response);
    }

    /**
     * 删除用药提醒接口
     * HTTP方法：DELETE
     * 权限要求：已登录用户
     * 
     * @param id 提醒ID
     * @return 操作结果，无返回数据
     */
    @DeleteMapping("/reminders/{id}")
    public BaseResponse<Void> deleteReminder(@PathVariable Integer id) {
        reminderService.deleteReminder(id);
        return BaseResponse.success("删除提醒成功");
    }
}
