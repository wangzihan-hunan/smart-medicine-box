package com.example.medicinebox.repository;

import com.example.medicinebox.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统日志数据访问接口
 * 提供系统操作日志相关的数据库操作方法
 */
@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Integer> {

    /**
     * 根据用户ID分页查询日志
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页日志列表
     */
    Page<SystemLog> findByUserId(Integer userId, Pageable pageable);

    /**
     * 根据模块分页查询日志
     * @param module 模块名称
     * @param pageable 分页参数
     * @return 分页日志列表
     */
    Page<SystemLog> findByModule(String module, Pageable pageable);

    /**
     * 根据模块和用户ID分页查询日志
     * @param module 模块名称
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页日志列表
     */
    Page<SystemLog> findByModuleAndUserId(String module, Integer userId, Pageable pageable);

    /**
     * 根据操作是否成功分页查询日志
     * @param success 是否成功
     * @param pageable 分页参数
     * @return 分页日志列表
     */
    Page<SystemLog> findBySuccess(Boolean success, Pageable pageable);

    /**
     * 根据时间范围查询日志（分页）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 分页日志列表
     */
    @Query("SELECT l FROM SystemLog l WHERE l.createdAt BETWEEN :startTime AND :endTime")
    Page<SystemLog> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                    @Param("endTime") LocalDateTime endTime, 
                                    Pageable pageable);

    /**
     * 查询最近10条系统日志（按创建时间降序）
     * @return 最近10条日志列表
     */
    @Query("SELECT l FROM SystemLog l ORDER BY l.createdAt DESC")
    List<SystemLog> findTop10ByOrderByCreatedAtDesc();

    /**
     * 查询指定用户最近10条日志（按创建时间降序）
     * @param userId 用户ID
     * @return 最近10条日志列表
     */
    @Query("SELECT l FROM SystemLog l WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    List<SystemLog> findTop10ByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    /**
     * 查询所有不重复的模块名称（按模块名排序）
     * @return 模块名称列表
     */
    @Query("SELECT DISTINCT l.module FROM SystemLog l ORDER BY l.module")
    List<String> findDistinctModules();

    /**
     * 统计指定时间之后的日志数量
     * @param dateTime 时间点
     * @return 日志数量
     */
    long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 统计成功或失败的日志数量
     * @param success 是否成功
     * @return 日志数量
     */
    long countBySuccess(Boolean success);
}
