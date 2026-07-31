package com.example.medicinebox.dto.request;

import com.example.medicinebox.constant.UserPlan;
import com.example.medicinebox.constant.UserRole;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 管理员创建用户请求DTO
 * 用于管理员后台创建用户时提交的用户信息
 */
public class AdminCreateUserRequest {

    // 用户手机号
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    // 用户姓名
    @NotBlank(message = "姓名不能为空")
    private String name;

    // 用户密码
    @NotBlank(message = "密码不能为空")
    private String password;

    // 用户角色（默认值：USER）
    private String role = UserRole.USER.name();

    // 用户套餐（默认值：FREE）
    private String plan = UserPlan.FREE.name();

    // 每月AI使用次数限制（默认值：10）
    private Integer monthAiLimit = 10;

    public AdminCreateUserRequest() {}

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public Integer getMonthAiLimit() { return monthAiLimit; }
    public void setMonthAiLimit(Integer monthAiLimit) { this.monthAiLimit = monthAiLimit; }
}
