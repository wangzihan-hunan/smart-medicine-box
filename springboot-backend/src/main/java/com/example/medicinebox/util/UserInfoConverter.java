package com.example.medicinebox.util;

import com.example.medicinebox.dto.response.UserInfoResponse;
import com.example.medicinebox.entity.User;

/**
 * 用户信息转换工具类
 * 将 User 实体转换为 UserInfoResponse 响应对象，统一处理手机号脱敏
 */
public final class UserInfoConverter {

    private UserInfoConverter() {
        // 工具类，禁止实例化
    }

    /**
     * 根据用户实体构建用户信息响应对象（含手机号脱敏）
     *
     * @param user 用户实体
     * @return 用户信息响应对象
     */
    public static UserInfoResponse fromUser(User user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setPhone(PhoneUtils.maskPhone(user.getPhone()));
        response.setName(user.getName());
        response.setRole(user.getRole().name());
        response.setPhoneVerified(user.getPhoneVerified());
        response.setStatus(user.getStatus().name());
        response.setPlan(user.getPlan().name());
        response.setPlanExpireAt(user.getPlanExpireAt());
        response.setMonthAiCount(user.getMonthAiCount());
        response.setMonthAiLimit(user.getMonthAiLimit());
        response.setLoginAttempts(user.getLoginAttempts());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginTime(user.getLastLoginTime());
        response.setLockTime(user.getLockTime());
        response.setLastIp(user.getLastIp());
        return response;
    }
}