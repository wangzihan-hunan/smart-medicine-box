package com.example.medicinebox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 设备指令列表响应DTO
 * 用于分页返回设备指令列表信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandListResponse {
    // 总记录数
    private Integer totalElements;
    // 总页数
    private Integer totalPages;
    // 当前页码
    private Integer currentPage;
    // 指令列表数据
    private List<CommandResponse> list;

    public CommandListResponse(Integer totalElements, List<CommandResponse> list) {
        this.totalElements = totalElements;
        this.list = list;
    }
}
