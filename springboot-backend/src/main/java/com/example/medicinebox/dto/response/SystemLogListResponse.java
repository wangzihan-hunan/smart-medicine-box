package com.example.medicinebox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 系统日志列表响应DTO
 * 用于分页返回系统日志列表信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogListResponse {

    // 总记录数
    private long totalElements;
    // 总页数
    private int totalPages;
    // 日志列表数据
    private List<SystemLogResponse> list;

    /**
     * 构建日志列表响应（不分页场景）
     * @param logs 日志列表
     * @return 日志列表响应对象
     */
    public static SystemLogListResponse of(List<SystemLogResponse> logs) {
        return new SystemLogListResponse(logs.size(), 1, logs);
    }
}
