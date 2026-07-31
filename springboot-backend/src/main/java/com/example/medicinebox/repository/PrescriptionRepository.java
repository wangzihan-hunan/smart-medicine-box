package com.example.medicinebox.repository;

import com.example.medicinebox.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 处方数据访问接口
 * 提供处方相关的数据库操作方法
 */
@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {

    /**
     * 根据用户ID查询处方列表（按上传时间降序）
     * @param userId 用户ID
     * @return 处方列表
     */
    List<Prescription> findByUserIdOrderByUploadDateDesc(Integer userId);

    /**
     * 根据处方ID和用户ID查询处方
     * @param id 处方ID
     * @param userId 用户ID
     * @return 处方对象（Optional）
     */
    Optional<Prescription> findByIdAndUserId(Integer id, Integer userId);

    /**
     * 根据用户ID分页查询处方
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页处方列表
     */
    Page<Prescription> findByUserId(Integer userId, Pageable pageable);
}
