package com.example.medicinebox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册响应DTO
 * 用于返回用户注册成功后的用户ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    // 新注册用户的ID
    private Integer userId;
}
