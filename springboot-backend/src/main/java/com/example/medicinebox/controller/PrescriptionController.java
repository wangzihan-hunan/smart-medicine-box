package com.example.medicinebox.controller;

import com.example.medicinebox.dto.request.PrescriptionUpdateRequest;
import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.dto.response.PrescriptionListResponse;
import com.example.medicinebox.dto.response.PrescriptionResponse;
import com.example.medicinebox.service.PrescriptionService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 药方管理控制器
 * 提供药方上传、历史查询、更新和删除等功能
 * 路由前缀：/api/v1/prescription
 */
@RestController
@RequestMapping("/api/v1/prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    /**
     * 上传药方接口
     * HTTP方法：POST
     * 权限要求：已登录用户
     * 支持上传药方图片文件，并可附加诊断信息和用药说明
     * 
     * @param file 药方图片文件
     * @param diagnosis 诊断信息（可选）
     * @param dosageInstructions 用药说明（可选）
     * @return 上传成功的药方详细信息
     * @throws IOException 文件读取异常
     */
    @PostMapping("/upload")
    public BaseResponse<PrescriptionResponse> uploadPrescription(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "diagnosis", required = false) String diagnosis,
            @RequestParam(value = "dosageInstructions", required = false) String dosageInstructions) throws IOException {
        PrescriptionResponse response = prescriptionService.uploadPrescription(file, diagnosis, dosageInstructions);
        return BaseResponse.success("药方上传成功！", response);
    }

    /**
     * 获取药方历史记录接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @return 药方历史列表响应，包含总数和药方信息列表
     */
    @GetMapping("/history")
    public BaseResponse<PrescriptionListResponse> getPrescriptionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PrescriptionListResponse response = prescriptionService.getPrescriptionHistory(page, size);
        return BaseResponse.success("获取药方历史成功！", response);
    }

    /**
     * 删除药方接口
     * HTTP方法：DELETE
     * 权限要求：已登录用户
     * 
     * @param prescriptionId 药方ID
     * @return 操作结果，无返回数据
     */
    @DeleteMapping("/{prescriptionId}")
    public BaseResponse<Void> deletePrescription(@PathVariable Integer prescriptionId) {
        prescriptionService.deletePrescription(prescriptionId);
        return BaseResponse.success("删除药方成功！");
    }

    /**
     * 更新药方信息接口
     * HTTP方法：PUT
     * 权限要求：已登录用户
     * 
     * @param prescriptionId 药方ID
     * @param request 药方更新请求体，包含诊断信息和用药说明
     * @return 更新后的药方详细信息
     */
    @PutMapping("/{prescriptionId}")
    public BaseResponse<PrescriptionResponse> updatePrescription(
            @PathVariable Integer prescriptionId,
            @Valid @RequestBody PrescriptionUpdateRequest request) {
        PrescriptionResponse response = prescriptionService.updatePrescription(prescriptionId, request.getDiagnosis(), request.getDosageInstructions());
        return BaseResponse.success("药方更新成功！", response);
    }
}
