package com.example.medicinebox.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册请求DTO
 * 用于接收用户注册时提交的手机号、密码和姓名信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {

    // 用户手机号
    @NotBlank(message = "手机号不能为空")
    private String phone;

    // 用户密码（至少8位，包含大小写字母和数字）
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$", 
             message = "密码至少8位，包含大小写字母和数字")
    private String password;

    // 用户姓名
    private String name;
}
