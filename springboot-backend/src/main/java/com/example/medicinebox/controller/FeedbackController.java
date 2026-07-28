package com.example.medicinebox.controller;

import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.dto.response.FeedbackListResponse;
import com.example.medicinebox.dto.response.FeedbackResponse;
import com.example.medicinebox.service.FeedbackService;
import com.example.medicinebox.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户反馈控制器
 * 提供用户反馈提交、查询、删除，以及管理员回复和状态管理等功能
 * 路由前缀：/api/v1/feedbacks
 */
@RestController
@RequestMapping("/api/v1/feedbacks")
public class FeedbackController {

    private static final Logger log = LoggerFactory.getLogger(FeedbackController.class);

    @Autowired
    private FeedbackService feedbackService;

    /**
     * 提交用户反馈接口
     * HTTP方法：POST
     * 权限要求：已登录用户
     * 
     * @param request 反馈请求体，包含类型、标题、内容、联系方式等
     * @return 提交成功的反馈详细信息
     */
    @PostMapping
    public BaseResponse<FeedbackResponse> submitFeedback(@RequestBody Map<String, String> request) {
        log.info("用户提交反馈");
        String type = request.get("type");
        String title = request.get("title");
        String content = request.get("content");
        String contact = request.get("contact");
        FeedbackResponse response = feedbackService.submitFeedback(type, title, content, contact);
        return BaseResponse.success("反馈提交成功", response);
    }

    /**
     * 查询我的反馈列表接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @param page 页码，默认0
     * @param size 每页数量，默认10
     * @param status 反馈状态筛选（可选）
     * @return 反馈列表响应，包含总数和反馈信息列表
     */
    @GetMapping("/my")
    public BaseResponse<FeedbackListResponse> getMyFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        log.info("查询我的反馈, page: {}, size: {}, status: {}", validatedPage, validatedSize, status);
        FeedbackListResponse response = feedbackService.getMyFeedbacks(validatedPage, validatedSize, status);
        return BaseResponse.success("查询成功", response);
    }

    /**
     * 查询反馈详情接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @param id 反馈ID
     * @return 反馈详细信息
     */
    @GetMapping("/{id}")
    public BaseResponse<FeedbackResponse> getFeedbackDetail(@PathVariable Integer id) {
        log.info("查询反馈详情, id: {}", id);
        FeedbackResponse response = feedbackService.getFeedbackDetail(id);
        return BaseResponse.success("查询成功", response);
    }

    /**
     * 删除反馈接口
     * HTTP方法：DELETE
     * 权限要求：已登录用户
     * 
     * @param id 反馈ID
     * @return 操作结果，无返回数据
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteFeedback(@PathVariable Integer id) {
        log.info("删除反馈, id: {}", id);
        feedbackService.deleteFeedback(id);
        return BaseResponse.success("删除成功", null);
    }

    /**
     * 管理员查询所有反馈列表接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param page 页码，默认0
     * @param size 每页数量，默认10
     * @param status 反馈状态筛选（可选）
     * @param type 反馈类型筛选（可选）
     * @return 反馈列表响应，包含总数和反馈信息列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<FeedbackListResponse> getAllFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        log.info("管理员查询反馈列表, page: {}, size: {}, status: {}, type: {}", validatedPage, validatedSize, status, type);
        FeedbackListResponse response = feedbackService.getAllFeedbacks(validatedPage, validatedSize, status, type);
        return BaseResponse.success("查询成功", response);
    }

    /**
     * 管理员回复反馈接口
     * HTTP方法：PUT
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param id 反馈ID
     * @param request 回复请求体，包含回复内容
     * @return 更新后的反馈详细信息
     */
    @PutMapping("/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<FeedbackResponse> replyFeedback(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        log.info("管理员回复反馈, id: {}", id);
        String reply = request.get("reply");
        FeedbackResponse response = feedbackService.replyFeedback(id, reply);
        return BaseResponse.success("回复成功", response);
    }

    /**
     * 更新反馈状态接口
     * HTTP方法：PUT
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param id 反馈ID
     * @param status 新的反馈状态
     * @return 更新后的反馈详细信息
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<FeedbackResponse> updateFeedbackStatus(
            @PathVariable Integer id,
            @RequestParam String status) {
        log.info("更新反馈状态, id: {}, status: {}", id, status);
        FeedbackResponse response = feedbackService.updateFeedbackStatus(id, status);
        return BaseResponse.success("状态更新成功", response);
    }

    /**
     * 查询待处理反馈数量接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 
     * @return 待处理反馈数量
     */
    @GetMapping("/stats/pending-count")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Long> getPendingCount() {
        log.info("查询待处理反馈数量");
        long count = feedbackService.getPendingCount();
        return BaseResponse.success("查询成功", count);
    }
}
