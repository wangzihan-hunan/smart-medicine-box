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
     * 根据用户ID查询所有指令
     * @param userId 用户ID
     * @return 指令列表
     */
    List<Command> findByUserId(Integer userId);

    /**
     * 根据指令ID和用户ID查询指令
     * @param id 指令ID
     * @param userId 用户ID
     * @return 指令对象（可选）
     */
    java.util.Optional<Command> findByIdAndUserId(Integer id, Integer userId);

    /**
     * 根据用户ID分页查询指令
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页指令列表
     */
    Page<Command> findByUserId(Integer userId, Pageable pageable);

    /**
     * 查询用户的所有不重复设备编号
     * @param userId 用户ID
     * @return 设备编号列表
     */
    @Query("SELECT DISTINCT c.deviceId FROM Command c WHERE c.userId = :userId AND c.deviceId IS NOT NULL")
    List<String> findDistinctDeviceIdsByUserId(@Param("userId") Integer userId);

    /**
     * 根据用户ID和设备编号查询指令列表（按时间降序）
     * @param userId 用户ID
     * @param deviceId 设备编号
     * @return 指令列表
     */
    @Query("SELECT c FROM Command c WHERE c.userId = :userId AND c.deviceId = :deviceId ORDER BY c.time DESC")
    List<Command> findByUserIdAndDeviceId(@Param("userId") Integer userId, @Param("deviceId") String deviceId);
}
