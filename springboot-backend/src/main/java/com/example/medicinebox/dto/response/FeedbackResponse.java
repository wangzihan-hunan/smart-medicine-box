package com.example.medicinebox.dto.response;

import com.example.medicinebox.entity.Feedback;

/**
 * 用户反馈详情响应DTO
 * 用于返回单条用户反馈的详细信息
 */
public class FeedbackResponse {

    // 反馈ID
    private Integer id;
    // 反馈用户ID
    private Integer userId;
    // 反馈类型
    private String type;
    // 反馈标题
    private String title;
    // 反馈内容
    private String content;
    // 联系方式
    private String contact;
    // 反馈状态
    private String status;
    // 管理员回复内容
    private String reply;
    // 提交时间
    private String createdAt;
    // 回复时间
    private String repliedAt;

    public FeedbackResponse() {
    }

    public FeedbackResponse(Integer id, Integer userId, String type, String title, String content,
                           String contact, String status, String reply, String createdAt, String repliedAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.contact = contact;
        this.status = status;
        this.reply = reply;
        this.createdAt = createdAt;
        this.repliedAt = repliedAt;
    }

    /**
     * 从实体对象转换为响应DTO
     * @param feedback 反馈实体
     * @return 反馈响应DTO
     */
    public static FeedbackResponse fromEntity(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getUserId(),
                feedback.getType(),
                feedback.getTitle(),
                feedback.getContent(),
                feedback.getContact(),
                feedback.getStatus().name(),
                feedback.getReply(),
                feedback.getCreatedAt() != null ? feedback.getCreatedAt().toString() : null,
                feedback.getRepliedAt() != null ? feedback.getRepliedAt().toString() : null
        );
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(String repliedAt) {
        this.repliedAt = repliedAt;
    }
}
