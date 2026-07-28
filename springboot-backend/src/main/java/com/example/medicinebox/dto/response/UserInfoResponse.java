package com.example.medicinebox.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 * 用于返回用户的详细信息，包括基本信息、套餐信息和登录状态等
 */
@Data
@NoArgsConstructor
public class UserInfoResponse {
    // 用户ID
    private Integer id;
    // 用户手机号（脱敏处理）
    private String phone;
    // 用户姓名
    private String name;
    // 用户角色（USER/ADMIN等）
    private String role;
    // 手机号是否已验证
    private Boolean phoneVerified;
    // 用户状态（ACTIVE/LOCKED等）
    private String status;
    // 用户套餐类型（FREE/PREMIUM等）
    private String plan;
    // 套餐到期时间
    private LocalDateTime planExpireAt;
    // 本月已使用AI次数
    private Integer monthAiCount;
    // 本月AI使用次数限制
    private Integer monthAiLimit;
    // 登录失败尝试次数
    private Integer loginAttempts;
    // 账号创建时间
    private LocalDateTime createdAt;
    // 最后登录时间
    private LocalDateTime lastLoginTime;
    // 账号锁定时间
    private LocalDateTime lockTime;
    // 最后登录IP地址
    private String lastIp;

    public UserInfoResponse(Integer id, String phone, String name, String role, Boolean phoneVerified) {
        this.id = id;
        this.phone = maskPhone(phone);
        this.name = name;
        this.role = role;
        this.phoneVerified = phoneVerified;
    }

    /**
     * 手机号脱敏处理
     * 将手机号中间4位替换为****
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
