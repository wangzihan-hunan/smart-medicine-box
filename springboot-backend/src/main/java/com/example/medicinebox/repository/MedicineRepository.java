package com.example.medicinebox.repository;

import com.example.medicinebox.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 药品数据访问接口
 * 提供药品相关的数据库操作方法，支持 JPA Specification 动态查询
 */
@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer>, JpaSpecificationExecutor<Medicine> {

    /**
     * 根据用户ID查询所有药品
     * @param userId 用户ID
     * @return 药品列表
     */
    List<Medicine> findByUserId(Integer userId);

    /**
     * 根据药品ID和用户ID查询药品
     * @param id 药品ID
     * @param userId 用户ID
     * @return 药品对象
     */
    Medicine findByIdAndUserId(Integer id, Integer userId);

    /**
     * 根据用户ID分页查询药品
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页药品列表
     */
    Page<Medicine> findByUserId(Integer userId, Pageable pageable);

    /**
     * 根据用户ID查询库存小于等于各自阈值的药品（数据库层面比较 stock <= low_stock_threshold）
     * @param userId 用户ID
     * @return 低库存药品列表
     */
    @Query("SELECT m FROM Medicine m WHERE m.userId = :userId AND m.stock <= m.lowStockThreshold")
    List<Medicine> findLowStockByUserId(@Param("userId") Integer userId);

    /**
     * 根据用户ID查询已过期的药品列表
     * @param userId 用户ID
     * @param date 对比日期
     * @return 过期药品列表
     */
    List<Medicine> findByUserIdAndExpireDateBefore(Integer userId, LocalDateTime date);

    /**
     * 根据用户ID查询过期日期早于指定日期的药品（分页）
     * @param userId 用户ID
     * @param expireDate 过期日期阈值
     * @param pageable 分页参数
     * @return 分页药品列表
     */
    Page<Medicine> findByUserIdAndExpireDateBefore(Integer userId, LocalDateTime expireDate, Pageable pageable);

    /**
     * 查询用户所有不重复的药品分类（数据库层面去重）
     * @param userId 用户ID
     * @return 分类名称列表
     */
    @Query("SELECT DISTINCT m.category FROM Medicine m WHERE m.userId = :userId AND m.category IS NOT NULL AND m.category <> ''")
    List<String> findDistinctCategoriesByUserId(@Param("userId") Integer userId);
}
