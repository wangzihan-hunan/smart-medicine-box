package com.example.medicinebox.dto.request;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录请求DTO
 * 用于接收用户登录时提交的手机号和密码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRequest {

    // 用户手机号
    @NotBlank(message = "手机号不能为空")
    private String phone;

    // 用户密码
    @NotBlank(message = "密码不能为空")
    private String password;
}
