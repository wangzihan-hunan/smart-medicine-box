package com.example.medicinebox.repository;

import com.example.medicinebox.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 * 提供用户相关的数据库操作方法
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户对象（可选）
     */
    Optional<User> findByPhone(String phone);

    /**
     * 检查手机号是否已存在
     * @param phone 手机号
     * @return 是否已存在
     */
    boolean existsByPhone(String phone);

    /**
     * 根据手机号或姓名模糊查询用户（分页）
     * @param phone 手机号关键词
     * @param name 姓名关键词
     * @param pageable 分页参数
     * @return 分页用户列表
     */
    Page<User> findByPhoneContainingOrNameContaining(String phone, String name, Pageable pageable);

    /**
     * 聚合统计用户各状态数量（一次查询替代全量加载+内存过滤）
     * 返回：[total, active, locked, blocked, suspended, admin, paid]
     */
    @Query("SELECT COUNT(u), " +
           "SUM(CASE WHEN u.status = 'ACTIVE' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN u.status = 'LOCKED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN u.status = 'BLOCKED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN u.status = 'SUSPENDED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN u.role = 'ADMIN' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN u.plan <> 'FREE' THEN 1 ELSE 0 END) " +
           "FROM User u")
    List<Object[]> countUserStats();

    /**
     * 批量更新用户状态（排除管理员）
     */
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id IN :ids AND u.role <> 'ADMIN'")
    int batchUpdateStatus(@Param("ids") List<Integer> ids, @Param("status") String status);

    /**
     * 批量更新用户角色（排除管理员）
     */
    @Modifying
    @Query("UPDATE User u SET u.role = :role WHERE u.id IN :ids AND u.role <> 'ADMIN'")
    int batchUpdateRole(@Param("ids") List<Integer> ids, @Param("role") String role);

    /**
     * 批量删除用户（排除管理员）
     */
    @Modifying
    @Query("DELETE FROM User u WHERE u.id IN :ids AND u.role <> 'ADMIN'")
    int batchDelete(@Param("ids") List<Integer> ids);
}
