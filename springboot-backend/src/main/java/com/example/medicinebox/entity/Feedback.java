package com.example.medicinebox.entity;

import com.example.medicinebox.constant.FeedbackStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户反馈实体类
 * 对应数据库表：feedbacks
 * 业务含义：存储用户提交的反馈意见、问题报告、功能建议等，以及管理员的回复处理
 */
@Entity
@Table(name = "feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private FeedbackStatus status = FeedbackStatus.PENDING;

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
        if (status == null) status = FeedbackStatus.PENDING;
    }

    // 实体更新前自动设置更新时间
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 领域方法 ====================

    /** 管理员回复反馈 */
    public void reply(String replyContent) {
        this.reply = replyContent;
        this.status = FeedbackStatus.REPLIED;
        this.repliedAt = LocalDateTime.now();
    }

    /** 更新处理状态，若状态为已回复且未设置回复时间则自动补全 */
    public void advanceStatus(FeedbackStatus newStatus) {
        this.status = newStatus;
        if (FeedbackStatus.REPLIED == newStatus && this.repliedAt == null) {
            this.repliedAt = LocalDateTime.now();
        }
    }
}
