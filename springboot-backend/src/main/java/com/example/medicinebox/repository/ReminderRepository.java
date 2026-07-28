package com.example.medicinebox.repository;

import com.example.medicinebox.entity.Reminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用药提醒数据访问接口
 * 提供用药提醒相关的数据库操作方法
 */
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Integer> {

    /**
     * 根据用户ID查询所有提醒
     * @param userId 用户ID
     * @return 提醒列表
     */
    List<Reminder> findByUserId(Integer userId);

    /**
     * 根据提醒ID和用户ID查询提醒
     * @param id 提醒ID
     * @param userId 用户ID
     * @return 提醒对象（可选）
     */
    java.util.Optional<Reminder> findByIdAndUserId(Integer id, Integer userId);

    /**
     * 根据用户ID分页查询提醒
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页提醒列表
     */
    Page<Reminder> findByUserId(Integer userId, Pageable pageable);

    /**
     * 根据用户ID和指定日期查询当天有效的提醒（分页）
     * 筛选条件：开始日期为空或小于等于指定日期，且结束日期为空或大于等于指定日期
     * 按提醒时间升序排列
     * @param userId 用户ID
     * @param date 查询日期
     * @param pageable 分页参数
     * @return 分页提醒列表
     */
    @Query("SELECT r FROM Reminder r WHERE r.userId = :userId AND " +
           "(r.startDate IS NULL OR r.startDate <= :date) AND " +
           "(r.endDate IS NULL OR r.endDate >= :date) " +
           "ORDER BY r.time ASC")
    Page<Reminder> findByUserIdAndDate(@Param("userId") Integer userId,
                                        @Param("date") LocalDate date,
                                        Pageable pageable);

    /**
     * 查询所有激活且下次提醒时间小于等于指定时间的提醒
     * @param time 指定时间
     * @return 提醒列表
     */
    List<Reminder> findByIsActiveTrueAndNextRemindTimeLessThanEqual(LocalDateTime time);

    /**
     * 查询所有激活、未服用且下次提醒时间小于等于指定时间的提醒
     * @param time 指定时间
     * @return 提醒列表
     */
    List<Reminder> findByIsActiveTrueAndTakenFalseAndNextRemindTimeLessThanEqual(LocalDateTime time);
}
