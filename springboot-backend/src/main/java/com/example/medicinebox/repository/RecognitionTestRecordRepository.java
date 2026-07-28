package com.example.medicinebox.repository;

import com.example.medicinebox.entity.RecognitionTestRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 识别测试记录数据访问接口
 * 提供药品识别测试记录相关的数据库操作方法
 */
@Repository
public interface RecognitionTestRecordRepository extends JpaRepository<RecognitionTestRecord, Integer> {

    /**
     * 根据批次ID查询测试记录
     * @param batchId 批次ID
     * @return 测试记录列表
     */
    List<RecognitionTestRecord> findByBatchId(String batchId);

    /**
     * 根据图片类型查询测试记录
     * @param imageType 图片类型
     * @return 测试记录列表
     */
    List<RecognitionTestRecord> findByImageType(String imageType);

    /**
     * 根据识别方法查询测试记录
     * @param recognitionMethod 识别方法
     * @return 测试记录列表
     */
    List<RecognitionTestRecord> findByRecognitionMethod(String recognitionMethod);

    /**
     * 按图片类型统计指定批次的识别结果
     * 返回：[图片类型, 总数, 成功数, 平均耗时]
     * @param batchId 批次ID
     * @return 统计结果列表
     */
    @Query("SELECT r.imageType, COUNT(r), SUM(CASE WHEN r.success = true THEN 1 ELSE 0 END), " +
           "AVG(r.costTimeMs) FROM RecognitionTestRecord r WHERE r.batchId = ?1 GROUP BY r.imageType")
    List<Object[]> getStatisticsByBatchId(String batchId);

    /**
     * 按识别方法统计指定批次的识别结果
     * 返回：[识别方法, 总数, 成功数, 平均耗时]
     * @param batchId 批次ID
     * @return 统计结果列表
     */
    @Query("SELECT r.recognitionMethod, COUNT(r), SUM(CASE WHEN r.success = true THEN 1 ELSE 0 END), " +
           "AVG(r.costTimeMs) FROM RecognitionTestRecord r WHERE r.batchId = ?1 GROUP BY r.recognitionMethod")
    List<Object[]> getStatisticsByMethod(String batchId);
}
