package com.example.medicinebox.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户反馈实体类
 * 对应数据库表：feedbacks
 * 业务含义：存储用户提交的反馈意见、问题报告、功能建议等，以及管理员的回复处理
 */
@Entity
@Table(name = "feedbacks")
public class Feedback {

    // 反馈唯一标识，主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 提交反馈的用户ID
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // 反馈类型：如 BUG(问题反馈)、SUGGESTION(功能建议)、COMPLAINT(投诉)、OTHER(其他)等
    @Column(name = "type", length = 50, nullable = false)
    private String type;

    // 反馈标题
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    // 反馈详细内容
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    // 联系方式（手机号/邮箱等，方便客服联系用户）
    @Column(name = "contact", length = 100)
    private String contact;

    // 处理状态：PENDING(待处理)、PROCESSING(处理中)、RESOLVED(已解决)、CLOSED(已关闭)等
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    // 管理员回复内容
    @Column(name = "reply", columnDefinition = "TEXT")
    private String reply;

    // 回复时间
    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    // 反馈提交时间
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 记录最后更新时间
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 实体持久化前自动设置创建时间、更新时间及默认值
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }

    // 实体更新前自动设置更新时间
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Feedback() {
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

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
