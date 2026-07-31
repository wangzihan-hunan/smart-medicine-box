package com.example.medicinebox.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 处方实体类
 * 对应数据库表：prescriptions
 * 业务含义：存储用户上传的处方信息，包括处方图片、诊断结果、用药说明等
 */
@Entity
@Table(name = "prescriptions", indexes = {
        @Index(name = "idx_prescriptions_user_id", columnList = "user_id"),
        @Index(name = "idx_prescriptions_upload_date", columnList = "upload_date"),
        @Index(name = "idx_prescriptions_medicine_id", columnList = "medicine_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {

    // 处方唯一标识，主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 处方图片/文件的访问URL
    @Column(name = "file_url", length = 255)
    private String fileUrl;

    // 原始文件名
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    // 所属用户ID
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // 关联的药品ID（AI识别后关联）
    @Column(name = "medicine_id")
    private Integer medicineId;

    // 诊断结果/病情描述
    @Column(length = 500)
    private String diagnosis;

    // 用药说明/用法用量
    @Column(name = "dosage_instructions", length = 1000)
    private String dosageInstructions;

    // 处方上传时间
    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    // 记录创建时间
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 记录最后更新时间
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 乐观锁版本号，用于并发控制
    @Version
    @Column(name = "version")
    private Long version;

    // 实体持久化前自动设置上传时间、创建时间、更新时间
    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // 实体更新前自动设置更新时间
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 领域方法 ====================

    /** 更新诊断结果和用药说明 */
    public void updateInfo(String diagnosis, String dosageInstructions) {
        this.diagnosis = diagnosis;
        this.dosageInstructions = dosageInstructions;
    }

    /** 关联药品（AI识别后关联） */
    public void associateMedicine(Integer medicineId) {
        this.medicineId = medicineId;
    }
}