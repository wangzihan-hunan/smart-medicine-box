package com.example.medicinebox.dto.response;

import com.example.medicinebox.entity.Notification;

/**
 * 通知消息详情响应DTO
 * 用于返回单条通知消息的详细信息
 */
public class NotificationResponse {

    // 通知ID
    private Integer id;
    // 通知标题
    private String title;
    // 通知内容
    private String content;
    // 通知类型
    private String type;
    // 是否已读
    private Boolean readStatus;
    // 创建时间
    private String createdAt;

    public NotificationResponse() {
    }

    public NotificationResponse(Integer id, String title, String content, String type, 
                                Boolean readStatus, String createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.readStatus = readStatus;
        this.createdAt = createdAt;
    }

    /**
     * 从实体对象转换为响应DTO
     * @param notification 通知实体
     * @return 通知响应DTO
     */
    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getReadStatus(),
                notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null
        );
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Boolean readStatus) {
        this.readStatus = readStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
