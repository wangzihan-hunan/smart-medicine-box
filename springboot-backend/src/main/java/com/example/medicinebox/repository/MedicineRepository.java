package com.example.medicinebox.repository;

import com.example.medicinebox.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 药品数据访问接口
 * 提供药品相关的数据库操作方法
 */
@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer> {

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
     * 根据用户ID和药品名称模糊查询（分页）
     * @param userId 用户ID
     * @param name 药品名称关键词
     * @param pageable 分页参数
     * @return 分页药品列表
     */
    Page<Medicine> findByUserIdAndNameContaining(Integer userId, String name, Pageable pageable);

    /**
     * 根据用户ID和药品分类查询（分页）
     * @param userId 用户ID
     * @param category 药品分类
     * @param pageable 分页参数
     * @return 分页药品列表
     */
    Page<Medicine> findByUserIdAndCategory(Integer userId, String category, Pageable pageable);

    /**
     * 根据用户ID查询库存小于等于指定值的药品（分页）
     * @param userId 用户ID
     * @param stock 库存阈值
     * @param pageable 分页参数
     * @return 分页药品列表
     */
    Page<Medicine> findByUserIdAndStockLessThanEqual(Integer userId, Integer stock, Pageable pageable);

    /**
     * 根据用户ID查询过期日期早于指定日期的药品（分页）
     * @param userId 用户ID
     * @param expireDate 过期日期阈值
     * @param pageable 分页参数
     * @return 分页药品列表
     */
    Page<Medicine> findByUserIdAndExpireDateBefore(Integer userId, LocalDateTime expireDate, Pageable pageable);

    /**
     * 根据用户ID、药品名称和分类组合查询（分页）
     * @param userId 用户ID
     * @param name 药品名称关键词
     * @param category 药品分类
     * @param pageable 分页参数
     * @return 分页药品列表
     */
    Page<Medicine> findByUserIdAndNameContainingAndCategory(Integer userId, String name, String category, Pageable pageable);

    /**
     * 根据用户ID查询库存小于等于阈值的药品列表
     * @param userId 用户ID
     * @param threshold 库存阈值
     * @return 低库存药品列表
     */
    List<Medicine> findByUserIdAndStockLessThanEqual(Integer userId, Integer threshold);

    /**
     * 根据用户ID查询已过期的药品列表
     * @param userId 用户ID
     * @param date 对比日期
     * @return 过期药品列表
     */
    List<Medicine> findByUserIdAndExpireDateBefore(Integer userId, LocalDateTime date);
}
