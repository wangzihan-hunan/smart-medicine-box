package com.example.medicinebox.constant;

/**
 * 用户账号状态枚举
 */
public enum UserStatus {

    ACTIVE("正常"),
    LOCKED("锁定"),
    BLOCKED("封禁"),
    SUSPENDED("冻结");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}