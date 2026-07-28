package com.example.medicinebox.repository;

import com.example.medicinebox.entity.AlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
    Page<AlertRecord> findByStatus(String status, Pageable pageable);

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
    long countByStatus(String status);
}
