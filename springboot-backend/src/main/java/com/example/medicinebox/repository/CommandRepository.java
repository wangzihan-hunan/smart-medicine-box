package com.example.medicinebox.repository;

import com.example.medicinebox.entity.Command;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 设备指令数据访问接口
 * 提供设备控制指令相关的数据库操作方法
 */
@Repository
public interface CommandRepository extends JpaRepository<Command, Integer> {

    /**
     * 根据用户ID分页查询指令
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页指令列表
     */
    Page<Command> findByUserId(Integer userId, Pageable pageable);

    /**
     * 根据指令ID和用户ID查询指令
     * @param id 指令ID
     * @param userId 用户ID
     * @return 指令对象（可选）
     */
    java.util.Optional<Command> findByIdAndUserId(Integer id, Integer userId);

    /**
     * 查询用户所有设备的聚合统计信息
     * 一次查询返回每个设备的指令总数、已执行数和最后指令时间
     * @param userId 用户ID
     * @return [deviceId, totalCommands, executedCommands, lastCommandTime]
     */
    @Query("SELECT c.deviceId, COUNT(c), " +
           "SUM(CASE WHEN c.status = '已执行' THEN 1 ELSE 0 END), " +
           "MAX(c.time) " +
           "FROM Command c WHERE c.userId = :userId AND c.deviceId IS NOT NULL " +
           "GROUP BY c.deviceId ORDER BY MAX(c.time) DESC")
    List<Object[]> findDeviceStatsByUserId(@Param("userId") Integer userId);

    /**
     * 查询每个设备最新的一条指令（用于获取 lastAction 和 lastStatus）
     * 通过子查询取每个设备分组中 ID 最大的指令
     * @param userId 用户ID
     * @return 每个设备的最新指令
     */
    @Query("SELECT c FROM Command c WHERE c.id IN " +
           "(SELECT MAX(c2.id) FROM Command c2 WHERE c2.userId = :userId AND c2.deviceId IS NOT NULL GROUP BY c2.deviceId)")
    List<Command> findLatestCommandsPerDevice(@Param("userId") Integer userId);

    /**
     * 查询单个设备的统计信息
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @return [totalCommands, executedCommands]
     */
    @Query("SELECT COUNT(c), SUM(CASE WHEN c.status = '已执行' THEN 1 ELSE 0 END) " +
           "FROM Command c WHERE c.userId = :userId AND c.deviceId = :deviceId")
    List<Object[]> findDeviceStats(@Param("userId") Integer userId, @Param("deviceId") String deviceId);

    /**
     * 查询单个设备的最新指令（用于获取 lastAction 和 lastStatus）
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @return 最新指令
     */
    @Query("SELECT c FROM Command c WHERE c.userId = :userId AND c.deviceId = :deviceId ORDER BY c.time DESC, c.id DESC")
    List<Command> findTopByUserIdAndDeviceId(@Param("userId") Integer userId, @Param("deviceId") String deviceId, Pageable pageable);
}