package com.example.medicinebox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 药品识别响应DTO
 * 用于返回AI药品识别或药方识别的结果，包含识别文本、药品信息和诊断信息等
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineRecognitionResponse {

    // 识别是否成功
    private boolean success;
    // 响应消息
    private String message;
    // 识别到的原始文本
    private String recognizedText;
    // 单个药品信息（兼容旧版）
    private MedicineInfo medicineInfo;

    // AI 药方识别：药品列表（一张药方可能包含多种药品）
    private List<MedicineInfo> medicines;

    // AI 药方识别：诊断结果
    private String diagnosis;

    // AI 药方识别：医生姓名
    private String doctor;

    // AI 药方识别：医院名称
    private String hospital;

    // AI 药方识别：开具日期
    private String date;

    /**
     * 药品信息内部类
     * 用于封装识别到的单个药品的详细信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicineInfo {
        // 药品名称
        private String name;
        // 药品分类
        private String category;
        // 药品规格
        private String spec;
        // 生产厂家
        private String manufacturer;

        // 用法用量，如：每次1片
        private String dosage;

        // 用药频次，如：每日3次
        private String frequency;

        // 数量
        private String quantity;
    }
}
