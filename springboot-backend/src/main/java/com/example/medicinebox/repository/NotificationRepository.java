package com.example.medicinebox.repository;

import com.example.medicinebox.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 通知消息数据访问接口
 * 提供通知消息相关的数据库操作方法
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    /**
     * 根据用户ID分页查询通知（按创建时间降序）
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页通知列表
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    /**
     * 根据用户ID和已读状态分页查询通知（按创建时间降序）
     * @param userId 用户ID
     * @param readStatus 是否已读
     * @param pageable 分页参数
     * @return 分页通知列表
     */
    Page<Notification> findByUserIdAndReadStatusOrderByCreatedAtDesc(Integer userId, Boolean readStatus, Pageable pageable);

    /**
     * 统计用户未读或已读通知的数量
     * @param userId 用户ID
     * @param readStatus 已读状态
     * @return 通知数量
     */
    long countByUserIdAndReadStatus(Integer userId, Boolean readStatus);

    /**
     * 删除用户的所有通知
     * @param userId 用户ID
     */
    void deleteByUserId(Integer userId);
}
