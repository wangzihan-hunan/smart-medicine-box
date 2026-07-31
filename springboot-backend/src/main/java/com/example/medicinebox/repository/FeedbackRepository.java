package com.example.medicinebox.repository;

import com.example.medicinebox.constant.FeedbackStatus;
import com.example.medicinebox.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 用户反馈数据访问接口
 * 提供用户反馈相关的数据库操作方法
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer>, JpaSpecificationExecutor<Feedback> {

    /**
     * 根据用户ID分页查询反馈（按创建时间降序）
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页反馈列表
     */
    Page<Feedback> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    /**
     * 根据用户ID和状态分页查询反馈（按创建时间降序）
     * @param userId 用户ID
     * @param status 反馈状态
     * @param pageable 分页参数
     * @return 分页反馈列表
     */
    Page<Feedback> findByUserIdAndStatusOrderByCreatedAtDesc(Integer userId, FeedbackStatus status, Pageable pageable);

    /**
     * 分页查询所有反馈（按创建时间降序）
     * @param pageable 分页参数
     * @return 分页反馈列表
     */
    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 根据状态分页查询反馈（按创建时间降序）
     * @param status 反馈状态
     * @param pageable 分页参数
     * @return 分页反馈列表
     */
    Page<Feedback> findByStatusOrderByCreatedAtDesc(FeedbackStatus status, Pageable pageable);

    /**
     * 根据类型分页查询反馈（按创建时间降序）
     * @param type 反馈类型
     * @param pageable 分页参数
     * @return 分页反馈列表
     */
    Page<Feedback> findByTypeOrderByCreatedAtDesc(String type, Pageable pageable);

    /**
     * 统计指定状态的反馈数量
     * @param status 反馈状态
     * @return 反馈数量
     */
    long countByStatus(FeedbackStatus status);
}
