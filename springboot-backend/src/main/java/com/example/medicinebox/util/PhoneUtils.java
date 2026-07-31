package com.example.medicinebox.util;

/**
 * 手机号工具类
 * 提供手机号脱敏等通用工具方法
 */
public final class PhoneUtils {

    private PhoneUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 手机号脱敏处理
     * 将手机号中间4位替换为****，用于日志输出和响应数据中保护用户隐私。
     * 例如：13812345678 -> 138****5678
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号，如果手机号为 null 或长度不足7位则原样返回
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}