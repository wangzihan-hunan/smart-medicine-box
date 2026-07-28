package com.example.medicinebox.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * AI药方识别测试记录实体类
 * 对应数据库表：recognition_test_records
 * 业务含义：用于收集AI识别实验数据，评估识别准确率，记录识别结果与人工标注的对比
 */
@Entity
@Table(name = "recognition_test_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionTestRecord {

    // 测试记录唯一标识，主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 测试批次编号
    @Column(name = "batch_id", length = 50)
    private String batchId;

    // 图片文件名
    @Column(name = "image_name", length = 255)
    private String imageName;

    // 图片类型：handwritten(手写)、printed(打印)、blurry(模糊)
    @Column(name = "image_type", length = 20)
    private String imageType;

    // 识别方式：AI 或 OCR
    @Column(name = "recognition_method", length = 10)
    private String recognitionMethod;

    // 识别是否成功
    @Column(name = "success")
    private Boolean success;

    // 识别耗时（毫秒）
    @Column(name = "cost_time_ms")
    private Long costTimeMs;

    // 识别出的药品数量
    @Column(name = "medicine_count")
    private Integer medicineCount;

    // 人工标注的药品数量（用于对比校验）
    @Column(name = "ground_truth_count")
    private Integer groundTruthCount;

    // 正确识别的药品数量
    @Column(name = "correct_count")
    private Integer correctCount;

    // 原始识别文本
    @Column(name = "raw_text", length = 4000)
    private String rawText;

    // 识别结果JSON
    @Column(name = "result_json", length = 4000)
    private String resultJson;

    // 错误信息（识别失败时记录）
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // 记录创建时间
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 实体持久化前自动设置创建时间
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
