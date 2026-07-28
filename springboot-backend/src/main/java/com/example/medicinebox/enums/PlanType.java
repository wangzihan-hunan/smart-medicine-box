package com.example.medicinebox.enums;

/**
 * 用户套餐类型枚举
 *
 * <p>定义系统支持的所有用户套餐类型及其配置参数。
 * 每个套餐包含不同的价格和功能限制。
 *
 * <p>套餐列表：
 * <ul>
 *   <li>FREE（免费版）：0元，基础功能</li>
 *   <li>BASIC（基础版）：9.9元，中等功能限制</li>
 *   <li>PROFESSIONAL（专业版）：29.9元，无限制</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
public enum PlanType {
    /**
     * 免费版
     * 价格：0元
     * AI识别次数：5次/月
     * 药品数量：无限制
     * 提醒数量：无限制
     */
    FREE("免费版", 0, 5, Integer.MAX_VALUE, Integer.MAX_VALUE),

    /**
     * 基础版
     * 价格：9.9元（990分）
     * AI识别次数：50次/月
     * 药品数量：200种
     * 提醒数量：100个
     */
    BASIC("基础版", 990, 50, 200, 100),

    /**
     * 专业版
     * 价格：29.9元（2990分）
     * AI识别次数：无限制
     * 药品数量：无限制
     * 提醒数量：无限制
     */
    PROFESSIONAL("专业版", 2990, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    /** 套餐显示名称（中文） */
    private final String displayName;
    /** 套餐价格（单位：分，避免浮点数精度问题，例如 990 = 9.9元） */
    private final int priceCents;
    /** 每月AI识别次数限制 */
    private final int aiRecognitionLimit;
    /** 药品数量限制 */
    private final int medicineLimit;
    /** 提醒数量限制 */
    private final int reminderLimit;

    /**
     * 构造函数，初始化套餐类型
     *
     * @param displayName 套餐显示名称
     * @param priceCents 价格（单位：分）
     * @param aiRecognitionLimit AI识别次数限制
     * @param medicineLimit 药品数量限制
     * @param reminderLimit 提醒数量限制
     */
    PlanType(String displayName, int priceCents, int aiRecognitionLimit, int medicineLimit, int reminderLimit) {
        this.displayName = displayName;
        this.priceCents = priceCents;
        this.aiRecognitionLimit = aiRecognitionLimit;
        this.medicineLimit = medicineLimit;
        this.reminderLimit = reminderLimit;
    }

    /**
     * 获取套餐显示名称
     *
     * @return 套餐中文名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取套餐价格（单位：分）
     *
     * @return 价格（分）
     */
    public int getPriceCents() {
        return priceCents;
    }

    /**
     * 获取每月AI识别次数限制
     *
     * @return AI识别次数限制
     */
    public int getAiRecognitionLimit() {
        return aiRecognitionLimit;
    }

    /**
     * 获取药品数量限制
     *
     * @return 药品数量限制
     */
    public int getMedicineLimit() {
        return medicineLimit;
    }

    /**
     * 获取提醒数量限制
     *
     * @return 提醒数量限制
     */
    public int getReminderLimit() {
        return reminderLimit;
    }

    /**
     * 根据字符串获取套餐类型
     *
     * <p>将字符串转换为对应的PlanType枚举值，不区分大小写。
     * 如果传入null或无效值，默认返回FREE套餐。
     *
     * @param plan 套餐名称字符串
     * @return 对应的PlanType枚举值，无效时返回FREE
     */
    public static PlanType fromString(String plan) {
        if (plan == null) {
            return FREE;
        }
        try {
            return PlanType.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FREE;
        }
    }
}
