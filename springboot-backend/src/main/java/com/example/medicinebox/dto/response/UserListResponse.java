package com.example.medicinebox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户列表响应DTO
 * 用于分页返回用户列表信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListResponse {
    // 总记录数
    private Integer totalElements;
    // 总页数
    private Integer totalPages;
    // 当前页码
    private Integer currentPage;
    // 每页大小
    private Integer pageSize;
    // 用户列表数据
    private List<UserInfoResponse> list;

    public UserListResponse(Integer totalElements, List<UserInfoResponse> list) {
        this.totalElements = totalElements;
        this.list = list;
    }
}
