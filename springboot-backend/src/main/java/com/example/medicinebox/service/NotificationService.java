package com.example.medicinebox.service;

import com.example.medicinebox.dto.response.NotificationPushMessage;
import com.example.medicinebox.dto.response.NotificationResponse;
import com.example.medicinebox.entity.Notification;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.NotificationRepository;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final NotificationRepository notificationRepository;
    private final WebSocketPushService webSocketPushService;

    public NotificationService(UserRepository userRepository, NotificationRepository notificationRepository,
                               WebSocketPushService webSocketPushService) {
        super(userRepository);
        this.notificationRepository = notificationRepository;
        this.webSocketPushService = webSocketPushService;
    }

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
     * 按创建时间倒序排列，pageSize 上限 100
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 通知响应列表
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(int page, int size) {
        User user = getCurrentUser();
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        log.debug("查询通知列表, 用户ID: {}, 页码: {}, 大小: {}", user.getId(), validatedPage, validatedSize);
        
        Pageable pageable = PageRequest.of(validatedPage, validatedSize);
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        
        List<NotificationResponse> responseList = notificationPage.getContent().stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
        
        log.debug("查询通知列表成功, 用户ID: {}, 总数: {}", user.getId(), notificationPage.getTotalElements());
        return responseList;
    }

    /**
     * 获取当前用户的通知统计
     *
     * <p>使用一次聚合查询（COUNT + SUM + CASE WHEN）同时返回总数和未读数。
     *
     * @return 通知统计Map（unreadCount-未读数，totalCount-总数）
     */
    public Map<String, Object> getNotificationStats() {
        User user = getCurrentUser();
        
        List<Object[]> result = notificationRepository.countStatsByUserId(user.getId());
        long totalCount = 0;
        long unreadCount = 0;
        
        if (!result.isEmpty() && result.get(0)[0] != null) {
            totalCount = ((Number) result.get(0)[0]).longValue();
            unreadCount = result.get(0)[1] != null ? ((Number) result.get(0)[1]).longValue() : 0;
        }
        
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
        
        if (!notification.getUserId().equals(user.getId())) {
            throw new SecurityException("无权操作此通知");
        }
        
        notification.markAsRead();
        notificationRepository.save(notification);

        webSocketPushService.syncNotificationState(user.getId(), "READ");
        log.info("通知已标记为已读, 用户ID: {}, 通知ID: {}", user.getId(), notificationId);
    }

    /**
     * 标记当前用户所有未读通知为已读
     *
     * <p>使用一条 UPDATE 语句批量更新所有未读通知，无数量限制。
     */
    @Transactional
    public void markAllAsRead() {
        User user = getCurrentUser();
        log.debug("标记所有通知已读, 用户ID: {}", user.getId());
        
        int updated = notificationRepository.markAllAsReadByUserId(user.getId(), LocalDateTime.now());
        
        webSocketPushService.syncNotificationState(user.getId(), "READ_ALL");
        log.info("所有通知已标记为已读, 用户ID: {}, 更新条数: {}", user.getId(), updated);
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