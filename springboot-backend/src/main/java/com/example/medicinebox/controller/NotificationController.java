package com.example.medicinebox.controller;

import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.dto.response.NotificationResponse;
import com.example.medicinebox.service.NotificationService;
import com.example.medicinebox.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知管理控制器
 * 提供通知列表查询、统计、已读标记、删除等功能
 * 路由前缀：/api/v1/notifications
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 分页查询通知列表接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @return 通知列表
     */
    @GetMapping
    public BaseResponse<List<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        log.info("查询通知列表, page: {}, size: {}", validatedPage, validatedSize);
        List<NotificationResponse> notifications = notificationService.getNotifications(validatedPage, validatedSize);
        return BaseResponse.success("查询通知成功", notifications);
    }

    /**
     * 获取通知统计信息接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 返回未读数量、总数量等统计数据
     * 
     * @return 通知统计数据Map
     */
    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> getNotificationStats() {
        log.info("查询通知统计");
        Map<String, Object> stats = notificationService.getNotificationStats();
        return BaseResponse.success("查询统计成功", stats);
    }

    /**
     * 标记单条通知为已读接口
     * HTTP方法：PUT
     * 权限要求：已登录用户
     * 
     * @param id 通知ID
     * @return 操作结果，无返回数据
     */
    @PutMapping("/{id}/read")
    public BaseResponse<Void> markAsRead(@PathVariable Integer id) {
        log.info("标记通知已读, id: {}", id);
        notificationService.markAsRead(id);
        return BaseResponse.success("标记已读成功", null);
    }

    /**
     * 标记所有通知为已读接口
     * HTTP方法：PUT
     * 权限要求：已登录用户
     * 
     * @return 操作结果，无返回数据
     */
    @PutMapping("/read-all")
    public BaseResponse<Void> markAllAsRead() {
        log.info("标记所有通知已读");
        notificationService.markAllAsRead();
        return BaseResponse.success("所有通知已标记为已读", null);
    }

    /**
     * 删除单条通知接口
     * HTTP方法：DELETE
     * 权限要求：已登录用户
     * 
     * @param id 通知ID
     * @return 操作结果，无返回数据
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteNotification(@PathVariable Integer id) {
        log.info("删除通知, id: {}", id);
        notificationService.deleteNotification(id);
        return BaseResponse.success("删除通知成功", null);
    }

    /**
     * 删除所有通知接口
     * HTTP方法：DELETE
     * 权限要求：已登录用户
     * 
     * @return 操作结果，无返回数据
     */
    @DeleteMapping
    public BaseResponse<Void> deleteAllNotifications() {
        log.info("删除所有通知");
        notificationService.deleteAllNotifications();
        return BaseResponse.success("删除所有通知成功", null);
    }
}
