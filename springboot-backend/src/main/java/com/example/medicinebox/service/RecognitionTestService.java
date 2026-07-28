package com.example.medicinebox.service;

import com.example.medicinebox.dto.response.MedicineRecognitionResponse;
import com.example.medicinebox.entity.RecognitionTestRecord;
import com.example.medicinebox.repository.RecognitionTestRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 识别测试数据采集服务类
 * 核心功能：用于挑战杯实验数据采集，记录药品识别测试结果，
 * 支持按批次统计识别成功率、平均耗时、识别药品数量等指标，
 * 提供测试记录的增删查和批量统计分析功能
 */
@Service
public class RecognitionTestService {

    private static final Logger log = LoggerFactory.getLogger(RecognitionTestService.class);

    // 识别测试记录数据访问层
    private final RecognitionTestRecordRepository testRecordRepository;
    // JSON序列化工具
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，注入数据访问层和JSON工具
     *
     * @param testRecordRepository 识别测试记录数据访问层
     * @param objectMapper JSON序列化工具
     */
    public RecognitionTestService(RecognitionTestRecordRepository testRecordRepository,
                                  ObjectMapper objectMapper) {
        this.testRecordRepository = testRecordRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存一条识别测试记录
     * 记录批次ID、图片名称、图片类型、识别方法、是否成功、耗时、识别结果等信息
     *
     * @param batchId 测试批次ID
     * @param imageName 图片名称
     * @param imageType 图片类型（如药方、药品包装等）
     * @param recognitionMethod 识别方法（如OCR、AI_VISION等）
     * @param success 是否识别成功
     * @param costTimeMs 识别耗时（毫秒）
     * @param response 识别响应结果
     */
    @Transactional
    public void saveRecord(String batchId, String imageName, String imageType,
                           String recognitionMethod, boolean success, long costTimeMs,
                           MedicineRecognitionResponse response) {
        RecognitionTestRecord record = new RecognitionTestRecord();
        record.setBatchId(batchId);
        record.setImageName(imageName);
        record.setImageType(imageType);
        record.setRecognitionMethod(recognitionMethod);
        record.setSuccess(success);
        record.setCostTimeMs(costTimeMs);

        if (response != null) {
            // 记录识别到的药品数量
            record.setMedicineCount(response.getMedicines() != null ? response.getMedicines().size() : 0);
            // 记录原始识别文本
            record.setRawText(response.getRecognizedText());
            try {
                // 将完整响应序列化为JSON存储
                record.setResultJson(objectMapper.writeValueAsString(response));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize response: {}", e.getMessage());
            }
            // 识别失败时记录错误信息
            if (!response.isSuccess()) {
                record.setErrorMessage(response.getMessage());
            }
        }

        testRecordRepository.save(record);
        log.debug("Saved test record for batch {}, image {}", batchId, imageName);
    }

    /**
     * 获取指定批次的统计数据
     * 包含总测试数、成功数、失败数、成功率、平均耗时、总药品数、平均药品数，
     * 以及按图片类型分类的详细统计
     *
     * @param batchId 测试批次ID
     * @return 统计数据Map
     */
    public Map<String, Object> getBatchStatistics(String batchId) {
        List<RecognitionTestRecord> records = testRecordRepository.findByBatchId(batchId);

        int total = records.size();
        int successCount = 0;
        long totalTime = 0;
        int totalMedicines = 0;

        // 按图片类型统计总数和成功数
        Map<String, Integer> typeStats = new HashMap<>();
        Map<String, Integer> typeSuccess = new HashMap<>();

        for (RecognitionTestRecord record : records) {
            // 累计成功数
            if (Boolean.TRUE.equals(record.getSuccess())) {
                successCount++;
            }
            // 累计总耗时
            if (record.getCostTimeMs() != null) {
                totalTime += record.getCostTimeMs();
            }
            // 累计总药品数
            if (record.getMedicineCount() != null) {
                totalMedicines += record.getMedicineCount();
            }

            // 按图片类型分类统计
            String type = record.getImageType() != null ? record.getImageType() : "unknown";
            typeStats.put(type, typeStats.getOrDefault(type, 0) + 1);
            if (Boolean.TRUE.equals(record.getSuccess())) {
                typeSuccess.put(type, typeSuccess.getOrDefault(type, 0) + 1);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("total", total);
        result.put("successCount", successCount);
        result.put("failureCount", total - successCount);
        // 计算成功率，保留两位小数
        result.put("successRate", total > 0 ? String.format("%.2f%%", (double) successCount / total * 100) : "0%");
        // 计算平均耗时
        result.put("avgTimeMs", total > 0 ? totalTime / total : 0);
        result.put("totalMedicines", totalMedicines);
        // 计算平均每张图片识别的药品数
        result.put("avgMedicines", total > 0 ? String.format("%.2f", (double) totalMedicines / total) : "0");

        // 按图片类型细分的统计数据
        Map<String, Object> typeBreakdown = new HashMap<>();
        for (String type : typeStats.keySet()) {
            Map<String, Object> typeInfo = new HashMap<>();
            int count = typeStats.get(type);
            int success = typeSuccess.getOrDefault(type, 0);
            typeInfo.put("total", count);
            typeInfo.put("success", success);
            typeInfo.put("successRate", String.format("%.2f%%", (double) success / count * 100));
            typeBreakdown.put(type, typeInfo);
        }
        result.put("typeBreakdown", typeBreakdown);

        return result;
    }

    /**
     * 获取所有测试批次ID列表
     * 去重后按字典序排序返回
     *
     * @return 批次ID列表
     */
    public List<String> getAllBatchIds() {
        return testRecordRepository.findAll()
                .stream()
                .map(RecognitionTestRecord::getBatchId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取指定批次的所有测试记录
     *
     * @param batchId 测试批次ID
     * @return 测试记录列表
     */
    public List<RecognitionTestRecord> getRecordsByBatch(String batchId) {
        return testRecordRepository.findByBatchId(batchId);
    }

    /**
     * 清除指定批次的所有测试记录
     *
     * @param batchId 测试批次ID
     */
    @Transactional
    public void clearBatch(String batchId) {
        List<RecognitionTestRecord> records = testRecordRepository.findByBatchId(batchId);
        testRecordRepository.deleteAll(records);
        log.info("Cleared {} records for batch {}", records.size(), batchId);
    }
}
