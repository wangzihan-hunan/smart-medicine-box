package com.example.medicinebox.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知消息实体类
 * 对应数据库表：notifications
 * 业务含义：存储系统发送给用户的通知消息，包括用药提醒、库存预警、系统公告等
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {

    // 通知唯一标识，主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 接收用户ID
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // 通知标题
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    // 通知内容
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // 通知类型：如 MEDICINE_REMINDER(用药提醒)、LOW_STOCK(库存预警)、SYSTEM(系统通知)等
    @Column(name = "type", length = 50)
    private String type;

    // 是否已读，true-已读，false-未读
    @Column(name = "read_status", nullable = false)
    private Boolean readStatus = false;

    // 通知创建时间
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 阅读时间
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // 实体持久化前自动设置创建时间
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Notification(Integer userId, String title, String content, String type) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.readStatus = false;
    }

    // ==================== 领域方法 ====================

    /** 标记通知为已读 */
    public void markAsRead() {
        this.readStatus = true;
        this.readAt = LocalDateTime.now();
    }

    /** 通知是否已读 */
    public boolean isRead() {
        return Boolean.TRUE.equals(readStatus);
    }
}