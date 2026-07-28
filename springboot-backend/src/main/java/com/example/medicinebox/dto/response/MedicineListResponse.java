package com.example.medicinebox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 药品列表响应DTO
 * 用于分页返回药品列表信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineListResponse {
    // 总记录数
    private Integer totalElements;
    // 总页数
    private Integer totalPages;
    // 当前页码
    private Integer currentPage;
    // 药品列表数据
    private List<MedicineResponse> list;

    public MedicineListResponse(Integer totalElements, List<MedicineResponse> list) {
        this.totalElements = totalElements;
        this.list = list;
    }
}
