package com.example.medicinebox.constant;

/**
 * 反馈处理状态枚举
 */
public enum FeedbackStatus {

    PENDING("待处理"),
    PROCESSING("处理中"),
    REPLIED("已回复"),
    CLOSED("已关闭");

    private final String description;

    FeedbackStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}