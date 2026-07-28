package com.example.medicinebox.controller;

import com.example.medicinebox.dto.response.AlertListResponse;
import com.example.medicinebox.dto.response.AlertRecordResponse;
import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.entity.AlertRecord;
import com.example.medicinebox.service.AlertingService;
import com.example.medicinebox.util.PaginationUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 告警管理控制器
 * 提供告警列表查询、统计、处理、测试告警触发以及业务指标查看等功能
 * 所有接口仅管理员可访问
 * 路由前缀：/api/v1/admin/alerts
 */
@RestController
@RequestMapping("/api/v1/admin/alerts")
@PreAuthorize("hasRole('ADMIN')")
public class AlertController {

    private final AlertingService alertingService;
    private final MeterRegistry meterRegistry;

    public AlertController(AlertingService alertingService, MeterRegistry meterRegistry) {
        this.alertingService = alertingService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 分页获取告警列表接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param status 告警状态筛选（可选）
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @return 告警列表响应，包含总数和告警信息列表
     */
    @GetMapping
    public BaseResponse<AlertListResponse> getAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        Page<AlertRecord> alertPage = alertingService.getAlerts(status, validatedPage, validatedSize);
        List<AlertRecordResponse> list = alertPage.getContent().stream()
                .map(AlertRecordResponse::from)
                .collect(Collectors.toList());
        
        AlertListResponse response = new AlertListResponse();
        response.setTotalElements((int) alertPage.getTotalElements());
        response.setTotalPages(alertPage.getTotalPages());
        response.setCurrentPage(page);
        response.setPageSize(size);
        response.setList(list);
        
        return BaseResponse.success("获取成功", response);
    }

    /**
     * 获取告警统计信息接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 返回各状态告警数量等统计数据
     * 
     * @return 告警统计数据Map
     */
    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        Map<String, Long> alertStats = alertingService.getAlertStats();
        stats.putAll(alertStats);
        return BaseResponse.success("获取成功", stats);
    }

    /**
     * 处理告警接口
     * HTTP方法：PUT
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param id 告警ID
     * @param status 告警处理后的状态
     * @param remark 处理备注（可选）
     * @return 处理后的告警详细信息
     */
    @PutMapping("/{id}/handle")
    public BaseResponse<AlertRecordResponse> handleAlert(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String remark) {
        AlertRecord alert = alertingService.handleAlert(id, "admin", remark, status);
        return BaseResponse.success("处理成功", AlertRecordResponse.from(alert));
    }

    /**
     * 手动触发测试告警接口
     * HTTP方法：POST
     * 权限要求：管理员角色（ADMIN）
     * 用于测试告警系统功能
     * 
     * @param level 告警级别，默认WARNING
     * @param title 告警标题，默认"测试告警"
     * @return 触发的告警详细信息
     */
    @PostMapping("/test")
    public BaseResponse<AlertRecordResponse> triggerTestAlert(
            @RequestParam(defaultValue = "WARNING") String level,
            @RequestParam(defaultValue = "测试告警") String title) {
        AlertRecord alert = alertingService.manualTrigger(
                "manual", level, title, "这是一条手动触发的测试告警");
        return BaseResponse.success("触发成功", AlertRecordResponse.from(alert));
    }

    /**
     * 获取业务指标摘要接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 返回用户注册、登录、AI识别、OCR识别、业务异常、药方上传等业务指标统计
     * 
     * @return 业务指标统计数据Map
     */
    @GetMapping("/metrics/summary")
    public BaseResponse<Map<String, Object>> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // 用户相关指标
        summary.put("user_register_total", 
                meterRegistry.counter("business.user.register").count());
        
        Double loginSuccess = meterRegistry.find("business.user.login")
                .tag("result", "success").counter() != null
                ? meterRegistry.find("business.user.login").tag("result", "success").counter().count()
                : 0.0;
        Double loginFail = meterRegistry.find("business.user.login")
                .tag("result", "fail").counter() != null
                ? meterRegistry.find("business.user.login").tag("result", "fail").counter().count()
                : 0.0;
        
        summary.put("user_login_success_total", loginSuccess);
        summary.put("user_login_fail_total", loginFail);
        
        // AI 识别指标
        Double aiSuccess = meterRegistry.find("business.ai.recognition")
                .tag("result", "success").counter() != null
                ? meterRegistry.find("business.ai.recognition").tag("result", "success").counter().count()
                : 0.0;
        Double aiFail = meterRegistry.find("business.ai.recognition")
                .tag("result", "fail").counter() != null
                ? meterRegistry.find("business.ai.recognition").tag("result", "fail").counter().count()
                : 0.0;
        
        summary.put("ai_recognition_success_total", aiSuccess);
        summary.put("ai_recognition_fail_total", aiFail);
        
        // OCR 识别
        summary.put("ocr_recognition_total", 
                meterRegistry.counter("business.ocr.recognition").count());
        
        // 业务异常
        summary.put("business_exception_total", 
                meterRegistry.counter("business.exception").count());
        
        // 药方上传
        summary.put("prescription_upload_total", 
                meterRegistry.counter("business.prescription.upload").count());
        
        return BaseResponse.success("获取成功", summary);
    }
}
