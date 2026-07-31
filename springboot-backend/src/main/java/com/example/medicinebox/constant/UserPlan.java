package com.example.medicinebox.constant;

/**
 * 用户套餐类型枚举
 */
public enum UserPlan {

    FREE("免费版"),
    BASIC("基础版"),
    PROFESSIONAL("专业版");

    private final String description;

    UserPlan(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}