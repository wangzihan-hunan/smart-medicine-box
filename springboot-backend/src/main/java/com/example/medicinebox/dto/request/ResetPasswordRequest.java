package com.example.medicinebox.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 重置密码请求DTO
 * 用于通过验证码重置密码
 */
public class ResetPasswordRequest {

    /**
     * 手机号
     * 必须为11位数字
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号")
    private String phone;

    /**
     * 验证码
     * 必须为4位数字
     */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{4}$", message = "请输入4位验证码")
    private String captcha;

    /**
     * 新密码
     * 长度6-20位，包含字母和数字
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在6-20位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$", 
             message = "密码需包含字母和数字")
    private String newPassword;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String phone, String captcha, String newPassword) {
        this.phone = phone;
        this.captcha = captcha;
        this.newPassword = newPassword;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}