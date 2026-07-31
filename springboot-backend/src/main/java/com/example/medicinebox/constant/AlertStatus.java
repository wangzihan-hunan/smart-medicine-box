package com.example.medicinebox.constant;

/**
 * 告警状态枚举
 */
public enum AlertStatus {

    PENDING("待处理"),
    RESOLVED("已解决"),
    IGNORED("已忽略");

    private final String description;

    AlertStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}