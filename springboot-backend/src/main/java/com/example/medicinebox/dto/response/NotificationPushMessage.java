package com.example.medicinebox.dto.response;

import com.example.medicinebox.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

/**
 * WebSocket 推送消息 DTO
 * 用于向客户端推送通知及其状态变更
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPushMessage {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Integer id;
    private String title;
    private String content;
    private String type;
    private Boolean readStatus;
    private String createdAt;
    private String action;
    private Integer unreadCount;

    public static NotificationPushMessage fromEntity(Notification notification) {
        NotificationPushMessage message = new NotificationPushMessage();
        message.setId(notification.getId());
        message.setTitle(notification.getTitle());
        message.setContent(notification.getContent());
        message.setType(notification.getType());
        message.setReadStatus(notification.getReadStatus());
        if (notification.getCreatedAt() != null) {
            message.setCreatedAt(notification.getCreatedAt().format(FORMATTER));
        }
        return message;
    }
}
