package com.example.medicinebox.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO
 * 用于返回用户登录成功后的访问令牌信息
 */
@Data
@NoArgsConstructor
public class LoginResponse {
    // 访问令牌（JWT Token）
    private String accessToken;
    // 令牌类型（如：Bearer）
    private String tokenType;
    // 是否需要强制修改密码（默认管理员账号首次登录时为true）
    private Boolean mustChangePassword;

    // 构造函数（兼容原有调用）
    public LoginResponse(String accessToken, String tokenType) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.mustChangePassword = false;
    }

    // 构造函数（含强制改密标识）
    public LoginResponse(String accessToken, String tokenType, Boolean mustChangePassword) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.mustChangePassword = mustChangePassword;
    }
}
