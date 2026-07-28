package com.example.medicinebox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * 告警配置属性类
 *
 * <p>从配置文件中读取alerting前缀的配置项，用于系统告警功能的配置。
 * 使用@ConfigurationProperties注解实现类型安全的配置绑定。
 *
 * <p>配置内容包括：
 * <ul>
 *   <li>告警总开关</li>
 *   <li>邮件告警配置（SMTP服务器、发件人、收件人等）</li>
 *   <li>告警规则配置（错误率、响应时间、CPU使用率、内存使用率阈值）</li>
 * </ul>
 *
 * <p>配置示例（application.yml）：
 * <pre>
 * alerting:
 *   enabled: true
 *   email:
 *     enabled: true
 *     from: alert@example.com
 *     to: admin@example.com
 *     host: smtp.example.com
 *     port: 465
 *     username: user
 *     password: pass
 *   rules:
 *     error-rate:
 *       threshold: 0.05
 *       window-seconds: 300
 *     response-time:
 *       threshold-ms: 2000
 *       window-seconds: 300
 *     cpu-usage:
 *       threshold: 0.85
 *     memory-usage:
 *       threshold: 0.90
 * </pre>
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alerting")
public class AlertingProperties {

    /**
     * 是否启用告警功能
     * 默认值：false
     */
    private boolean enabled = false;

    /**
     * 邮件告警配置
     */
    private Email email = new Email();

    /**
     * 告警规则配置
     */
    private Rules rules = new Rules();

    /**
     * 邮件告警配置类
     *
     * <p>配置邮件告警的SMTP服务器信息和收发件人信息。
     */
    @Data
    public static class Email {
        /** 是否启用邮件告警，默认false */
        private boolean enabled = false;
        /** 发件人邮箱地址 */
        private String from;
        /** 收件人邮箱地址 */
        private String to;
        /** SMTP服务器地址 */
        private String host;
        /** SMTP服务器端口，默认465 */
        private int port = 465;
        /** SMTP认证用户名 */
        private String username;
        /** SMTP认证密码 */
        private String password;
    }

    /**
     * 告警规则配置类
     *
     * <p>配置各项监控指标的告警阈值。
     */
    @Data
    public static class Rules {
        /** 错误率告警规则 */
        private ErrorRate errorRate = new ErrorRate();
        /** 响应时间告警规则 */
        private ResponseTime responseTime = new ResponseTime();
        /** CPU使用率告警规则 */
        private CpuUsage cpuUsage = new CpuUsage();
        /** 内存使用率告警规则 */
        private MemoryUsage memoryUsage = new MemoryUsage();
    }

    /**
     * 错误率告警规则类
     *
     * <p>当接口错误率超过阈值时触发告警。
     */
    @Data
    public static class ErrorRate {
        /** 错误率阈值，默认0.05（5%） */
        private double threshold = 0.05;
        /** 统计时间窗口（秒），默认300秒（5分钟） */
        private int windowSeconds = 300;
    }

    /**
     * 响应时间告警规则类
     *
     * <p>当接口平均响应时间超过阈值时触发告警。
     */
    @Data
    public static class ResponseTime {
        /** 响应时间阈值（毫秒），默认2000ms（2秒） */
        private long thresholdMs = 2000;
        /** 统计时间窗口（秒），默认300秒（5分钟） */
        private int windowSeconds = 300;
    }

    /**
     * CPU使用率告警规则类
     *
     * <p>当系统CPU使用率超过阈值时触发告警。
     */
    @Data
    public static class CpuUsage {
        /** CPU使用率阈值，默认0.85（85%） */
        private double threshold = 0.85;
    }

    /**
     * 内存使用率告警规则类
     *
     * <p>当系统内存使用率超过阈值时触发告警。
     */
    @Data
    public static class MemoryUsage {
        /** 内存使用率阈值，默认0.90（90%） */
        private double threshold = 0.90;
    }
}
