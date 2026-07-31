package com.example.medicinebox.constant;

/**
 * 业务常量定义
 * 集中管理系统中所有魔法数字和业务常量，提高可维护性
 */
public final class BusinessConstants {

    private BusinessConstants() {
        // 工具类，禁止实例化
    }

    // ==================== 登录安全策略 ====================

    /** 登录失败次数阈值，达到后锁定账号 */
    public static final int MAX_LOGIN_ATTEMPTS = 5;

    /** 账号锁定时长（分钟） */
    public static final long LOCK_DURATION_MINUTES = 30;

    // ==================== 验证码策略 ====================

    /** 验证码过期时间（分钟） */
    public static final long CAPTCHA_EXPIRE_MINUTES = 5;

    /** 验证码发送间隔（秒） */
    public static final long CAPTCHA_SEND_INTERVAL_SECONDS = 60;

    /** 验证码长度（位数） */
    public static final int CAPTCHA_LENGTH = 4;

    // ==================== 默认值 ====================

    /** 默认AI识别月度限额 */
    public static final int DEFAULT_AI_LIMIT = 10;

    /** 默认药品低库存阈值 */
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    /** 表示无限制的标记值 */
    public static final int UNLIMITED = -1;

    // ==================== 时间换算 ====================

    /** 每分钟毫秒数 */
    public static final long MILLIS_PER_MINUTE = 60_000L;

    /** 每秒毫秒数 */
    public static final long MILLIS_PER_SECOND = 1_000L;
}