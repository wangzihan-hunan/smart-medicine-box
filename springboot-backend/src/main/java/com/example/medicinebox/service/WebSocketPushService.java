package com.example.medicinebox.service;

import com.example.medicinebox.dto.response.NotificationPushMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketPushService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPushService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void pushToUser(Integer userId, NotificationPushMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notifications",
                message
            );
            log.debug("推送通知到用户 {}, action: {}", userId, message.getAction());
        } catch (Exception e) {
            log.error("推送通知失败, 用户ID: {}, 错误: {}", userId, e.getMessage());
        }
    }

    public void broadcastAnnouncement(NotificationPushMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/announcements", message);
            log.info("广播公告: {}", message.getTitle());
        } catch (Exception e) {
            log.error("广播公告失败: {}", e.getMessage());
        }
    }

    public void syncNotificationState(Integer userId, String action) {
        NotificationPushMessage message = new NotificationPushMessage();
        message.setAction(action);
        pushToUser(userId, message);
    }
}
