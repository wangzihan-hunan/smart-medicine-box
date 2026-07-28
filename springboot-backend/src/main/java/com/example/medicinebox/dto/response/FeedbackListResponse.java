package com.example.medicinebox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户反馈列表响应DTO
 * 用于分页返回用户反馈列表信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackListResponse {

    // 总记录数
    private long totalElements;
    // 总页数
    private int totalPages;
    // 反馈列表数据
    private List<FeedbackResponse> list;

    /**
     * 构建反馈列表响应
     * @param totalElements 总记录数
     * @param totalPages 总页数
     * @param list 反馈列表
     * @return 反馈列表响应对象
     */
    public static FeedbackListResponse of(long totalElements, int totalPages, List<FeedbackResponse> list) {
        return new FeedbackListResponse(totalElements, totalPages, list);
    }
}
