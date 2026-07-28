package com.example.medicinebox.service;

import com.example.medicinebox.dto.response.NotificationPushMessage;
import com.example.medicinebox.dto.response.NotificationResponse;
import com.example.medicinebox.entity.Notification;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知服务类
 * 核心功能：通知发送、通知列表查询、通知已读标记、通知删除、通知统计等
 */
@Service
public class NotificationService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WebSocketPushService webSocketPushService;

    /**
     * 发送通知给指定用户
     *
     * @param userId 用户ID
     * @param title 通知标题
     * @param content 通知内容
     * @param type 通知类型
     */
    @Transactional
    public void sendNotification(Integer userId, String title, String content, String type) {
        log.debug("发送通知, 用户ID: {}, 标题: {}, 类型: {}", userId, title, type);
        Notification notification = new Notification(userId, title, content, type);
        notificationRepository.save(notification);
        // 实时推送
        NotificationPushMessage message = NotificationPushMessage.fromEntity(notification);
        message.setAction("NEW");
        webSocketPushService.pushToUser(userId, message);
        log.info("通知发送成功, 用户ID: {}, 标题: {}", userId, title);
    }

    /**
     * 分页查询当前用户的通知列表
     * 按创建时间倒序排列
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 通知响应列表
     */
    public List<NotificationResponse> getNotifications(int page, int size) {
        User user = getCurrentUser();
        log.debug("查询通知列表, 用户ID: {}", user.getId());
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        
        List<NotificationResponse> responseList = notificationPage.getContent().stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
        
        log.debug("查询通知列表成功, 用户ID: {}, 总数: {}", user.getId(), notificationPage.getTotalElements());
        return responseList;
    }

    /**
     * 获取当前用户的通知统计
     * 包含未读数量和总数量
     *
     * @return 通知统计Map（unreadCount-未读数，totalCount-总数）
     */
    public Map<String, Object> getNotificationStats() {
        User user = getCurrentUser();
        long unreadCount = notificationRepository.countByUserIdAndReadStatus(user.getId(), false);
        long totalCount = notificationRepository.countByUserIdAndReadStatus(user.getId(), true) + unreadCount;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("unreadCount", unreadCount);
        stats.put("totalCount", totalCount);
        
        return stats;
    }

    /**
     * 标记单条通知为已读
     * 校验通知归属权，只能操作自己的通知
     *
     * @param notificationId 通知ID
     * @throws ResourceNotFoundException 通知不存在时抛出
     * @throws SecurityException 无权限操作时抛出
     */
    @Transactional
    public void markAsRead(Integer notificationId) {
        User user = getCurrentUser();
        log.debug("标记通知已读, 用户ID: {}, 通知ID: {}", user.getId(), notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在"));
        
        // 校验通知归属权
        if (!notification.getUserId().equals(user.getId())) {
            throw new SecurityException("无权操作此通知");
        }
        
        notification.setReadStatus(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);

        webSocketPushService.syncNotificationState(user.getId(), "READ");
        log.info("通知已标记为已读, 用户ID: {}, 通知ID: {}", user.getId(), notificationId);
    }

    /**
     * 标记当前用户所有未读通知为已读
     * 批量处理（每次最多100条）
     */
    @Transactional
    public void markAllAsRead() {
        User user = getCurrentUser();
        log.debug("标记所有通知已读, 用户ID: {}", user.getId());
        
        Pageable pageable = PageRequest.of(0, 100);
        Page<Notification> unreadNotifications = notificationRepository.findByUserIdAndReadStatusOrderByCreatedAtDesc(user.getId(), false, pageable);
        
        // 批量标记已读
        for (Notification notification : unreadNotifications.getContent()) {
            notification.setReadStatus(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        webSocketPushService.syncNotificationState(user.getId(), "READ_ALL");
        log.info("所有通知已标记为已读, 用户ID: {}", user.getId());
    }

    /**
     * 删除单条通知
     * 校验通知归属权，只能删除自己的通知
     *
     * @param notificationId 通知ID
     * @throws ResourceNotFoundException 通知不存在时抛出
     * @throws SecurityException 无权限操作时抛出
     */
    @Transactional
    public void deleteNotification(Integer notificationId) {
        User user = getCurrentUser();
        log.debug("删除通知, 用户ID: {}, 通知ID: {}", user.getId(), notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在"));
        
        // 校验通知归属权
        if (!notification.getUserId().equals(user.getId())) {
            throw new SecurityException("无权操作此通知");
        }
        
        notificationRepository.delete(notification);

        webSocketPushService.syncNotificationState(user.getId(), "DELETE");
        log.info("通知删除成功, 用户ID: {}, 通知ID: {}", user.getId(), notificationId);
    }

    /**
     * 删除当前用户的所有通知
     */
    @Transactional
    public void deleteAllNotifications() {
        User user = getCurrentUser();
        log.debug("删除所有通知, 用户ID: {}", user.getId());
        
        notificationRepository.deleteByUserId(user.getId());

        webSocketPushService.syncNotificationState(user.getId(), "DELETE_ALL");
        log.info("所有通知删除成功, 用户ID: {}", user.getId());
    }
}