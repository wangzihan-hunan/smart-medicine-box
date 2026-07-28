package com.example.medicinebox.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 处方更新请求DTO
 * 用于接收更新处方信息时提交的诊断结果和用药说明
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionUpdateRequest {
    // 诊断结果
    private String diagnosis;
    // 用药说明
    private String dosageInstructions;
}
