package com.example.medicinebox.repository;

import com.example.medicinebox.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
     * 删除用户的所有通知
     * @param userId 用户ID
     */
    void deleteByUserId(Integer userId);

    /**
     * 聚合统计用户通知总数和未读数
     * 一次查询替代两次 countByUserIdAndReadStatus 调用
     * @param userId 用户ID
     * @return [totalCount, unreadCount]
     */
    @Query("SELECT COUNT(n), SUM(CASE WHEN n.readStatus = false THEN 1 ELSE 0 END) " +
           "FROM Notification n WHERE n.userId = :userId")
    List<Object[]> countStatsByUserId(@Param("userId") Integer userId);

    /**
     * 批量标记用户所有未读通知为已读
     * 使用 UPDATE 语句直接更新数据库，避免逐条加载实体
     * @param userId 用户ID
     * @param now 当前时间
     * @return 受影响行数
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readStatus = true, n.readAt = :now " +
           "WHERE n.userId = :userId AND n.readStatus = false")
    int markAllAsReadByUserId(@Param("userId") Integer userId, @Param("now") LocalDateTime now);
}