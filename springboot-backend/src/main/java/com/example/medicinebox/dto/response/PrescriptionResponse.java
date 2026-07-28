package com.example.medicinebox.dto.response;

import com.example.medicinebox.entity.Prescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

/**
 * 处方详情响应DTO
 * 用于返回单个处方的详细信息，包括文件信息和诊断用药说明
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponse {
    // 处方ID
    private Integer id;
    // 所属用户ID
    private Integer userId;
    // 关联药品ID
    private Integer medicineId;
    // 药品名称
    private String medicineName;
    // 文件名称
    private String fileName;
    // 文件类型（扩展名）
    private String fileType;
    // 文件访问URL
    private String fileUrl;
    // 诊断结果
    private String diagnosis;
    // 用药说明
    private String dosageInstructions;
    // 上传日期时间（格式化字符串）
    private String uploadDate;

    // 日期时间格式化器
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 从实体对象转换为响应DTO
     * 自动提取文件名、文件类型并格式化上传日期
     * @param prescription 处方实体
     * @return 处方响应DTO
     */
    public static PrescriptionResponse fromEntity(Prescription prescription) {
        PrescriptionResponse response = new PrescriptionResponse();
        response.setId(prescription.getId());
        response.setUserId(prescription.getUserId());
        response.setMedicineId(prescription.getMedicineId());
        response.setFileUrl(prescription.getFileUrl());
        response.setDiagnosis(prescription.getDiagnosis());
        response.setDosageInstructions(prescription.getDosageInstructions());

        String fileName = prescription.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            fileName = prescription.getFileUrl();
            if (fileName != null && fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
        }
        response.setFileName(fileName);

        String fileType = "未知";
        if (fileName != null && fileName.contains(".")) {
            fileType = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
        }
        response.setFileType(fileType);

        if (prescription.getUploadDate() != null) {
            response.setUploadDate(prescription.getUploadDate().format(FORMATTER));
        }

        return response;
    }
}
