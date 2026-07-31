package com.example.medicinebox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * 认证安全配置属性类
 * 将登录安全策略从硬编码常量抽取为可配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** 登录失败次数阈值，达到后锁定账号 */
    private int maxLoginAttempts = 5;

    /** 账号锁定时长（分钟） */
    private long lockDurationMinutes = 30;

    /** 验证码过期时间（分钟） */
    private long captchaExpireMinutes = 5;

    /** 验证码发送间隔（秒） */
    private long captchaSendIntervalSeconds = 60;

    /** 验证码长度（位数） */
    private int captchaLength = 4;

    /** 默认AI识别月度限额 */
    private int defaultAiLimit = 10;
}