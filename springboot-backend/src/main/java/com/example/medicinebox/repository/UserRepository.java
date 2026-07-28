package com.example.medicinebox.repository;

import com.example.medicinebox.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
