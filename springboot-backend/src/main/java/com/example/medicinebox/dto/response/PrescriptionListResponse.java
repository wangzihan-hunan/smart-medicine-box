package com.example.medicinebox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 处方列表响应DTO
 * 用于分页返回处方列表信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionListResponse {
    // 总记录数
    private Integer totalElements;
    // 总页数
    private Integer totalPages;
    // 当前页码
    private Integer currentPage;
    // 处方列表数据
    private List<PrescriptionResponse> list;
}
