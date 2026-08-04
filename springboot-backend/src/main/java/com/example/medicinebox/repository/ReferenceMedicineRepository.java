package com.example.medicinebox.repository;

import com.example.medicinebox.entity.ReferenceMedicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 参考药品数据访问接口
 * 用于全局药品标准库的查询操作
 */
@Repository
public interface ReferenceMedicineRepository extends JpaRepository<ReferenceMedicine, Integer> {

    /**
     * 按名称模糊搜索（用于匹配时查询所有候选）
     * @return 所有参考药品列表
     */
    List<ReferenceMedicine> findAll();
}