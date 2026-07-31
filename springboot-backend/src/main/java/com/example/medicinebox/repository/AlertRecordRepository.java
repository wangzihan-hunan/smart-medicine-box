package com.example.medicinebox.repository;

import com.example.medicinebox.constant.AlertStatus;
import com.example.medicinebox.entity.AlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 告警记录数据访问接口
 * 提供系统告警记录相关的数据库操作方法
 */
@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    /**
     * 根据状态分页查询告警记录
     * @param status 告警状态
     * @param pageable 分页参数
     * @return 分页告警记录列表
     */
    Page<AlertRecord> findByStatus(AlertStatus status, Pageable pageable);

    /**
     * 根据级别分页查询告警记录
     * @param level 告警级别
     * @param pageable 分页参数
     * @return 分页告警记录列表
     */
    Page<AlertRecord> findByLevel(String level, Pageable pageable);

    /**
     * 根据类型分页查询告警记录
     * @param type 告警类型
     * @param pageable 分页参数
     * @return 分页告警记录列表
     */
    Page<AlertRecord> findByType(String type, Pageable pageable);

    /**
     * 统计指定状态的告警记录数量
     * @param status 告警状态
     * @return 告警记录数量
     */
    long countByStatus(AlertStatus status);

    /**
     * 聚合统计告警总数及各状态数量
     * 一次查询替代四次 countByStatus 调用
     * @return [total, pending, resolved, ignored]
     */
    @Query("SELECT COUNT(a), " +
           "SUM(CASE WHEN a.status = 'PENDING' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN a.status = 'RESOLVED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN a.status = 'IGNORED' THEN 1 ELSE 0 END) " +
           "FROM AlertRecord a")
    List<Object[]> countStats();
}
