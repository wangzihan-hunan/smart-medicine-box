package com.example.medicinebox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用药提醒列表响应DTO
 * 用于分页返回用药提醒列表信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderListResponse {
    // 总记录数
    private Integer totalElements;
    // 总页数
    private Integer totalPages;
    // 当前页码
    private Integer currentPage;
    // 提醒列表数据
    private List<ReminderResponse> list;

    public ReminderListResponse(Integer totalReminders, List<ReminderResponse> list) {
        this.totalElements = totalReminders;
        this.list = list;
    }
}
