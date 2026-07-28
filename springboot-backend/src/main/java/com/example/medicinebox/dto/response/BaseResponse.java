package com.example.medicinebox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应基类DTO
 * 用于封装所有接口的统一返回结果，包含状态码、状态、消息和数据
 * @param <T> 响应数据的泛型类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {

    // 响应状态码（200表示成功，其他表示错误）
    private Integer code;
    // 响应状态（success/error）
    private String status;
    // 响应消息
    private String message;
    // 响应数据
    private T data;

    /**
     * 成功响应（带消息和数据）
     * @param message 成功消息
     * @param data 响应数据
     * @return 成功响应对象
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(200, "success", message, data);
    }

    /**
     * 成功响应（仅带消息）
     * @param message 成功消息
     * @return 成功响应对象
     */
    public static <T> BaseResponse<T> success(String message) {
        return new BaseResponse<>(200, "success", message, null);
    }

    /**
     * 错误响应
     * @param code 错误码
     * @param message 错误消息
     * @return 错误响应对象
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, "error", message, null);
    }
}
