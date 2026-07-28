package com.example.medicinebox.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 修改密码请求DTO
 * 用于接收用户修改密码时提交的旧密码和新密码
 */
public class ChangePasswordRequest {

    // 当前旧密码
    @NotBlank(message = "当前密码不能为空")
    private String oldPassword;

    // 新密码（长度不少于8位，必须包含至少一个大写字母、一个小写字母和一个数字）
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "新密码长度不能少于8位")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$",
            message = "新密码必须包含至少一个大写字母、一个小写字母和一个数字")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
