package com.example.medicinebox.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流配置类
 *
 * <p>基于Google Guava的RateLimiter实现API请求限流功能。
 * 使用令牌桶算法控制请求频率，保护系统免受恶意请求攻击。
 *
 * <p>限流策略：
 * <ul>
 *   <li>登录接口：每IP每分钟10次请求</li>
 *   <li>注册接口：每IP每分钟5次请求</li>
 *   <li>自定义限流：通过@RateLimit注解配置</li>
 * </ul>
 *
 * <p>限流存储器使用ConcurrentHashMap，支持高并发场景。
 * 注意：当前为单机版限流，分布式环境需改用Redis等共享存储。
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Configuration
public class RateLimitConfig {

    /** 登录接口限流key前缀 */
    public static final String LOGIN_KEY_PREFIX = "rate_limit:login:";

    /** 注册接口限流key前缀 */
    public static final String REGISTER_KEY_PREFIX = "rate_limit:register:";

    /**
     * 限流器缓存Map
     * key: 限流标识（如"rate_limit:login:127.0.0.1"）
     * value: Guava RateLimiter实例
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    /**
     * 获取登录接口限流器
     *
     * <p>根据客户端IP获取对应的登录接口限流器。
     * 如果不存在则创建一个新的限流器，速率为10次/分钟。
     *
     * <p>限流策略：每个IP每分钟最多10次登录请求，
     * 防止暴力破解密码攻击。
     *
     * @param clientIP 客户端IP地址
     * @return RateLimiter登录接口限流器
     */
    public RateLimiter getLoginRateLimiter(String clientIP) {
        String key = LOGIN_KEY_PREFIX + clientIP;
        return rateLimiters.computeIfAbsent(key, k -> RateLimiter.create(10.0 / 60.0));
    }

    /**
     * 获取注册接口限流器
     *
     * <p>根据客户端IP获取对应的注册接口限流器。
     * 如果不存在则创建一个新的限流器，速率为5次/分钟。
     *
     * <p>限流策略：每个IP每分钟最多5次注册请求，
     * 防止恶意批量注册账号。
     *
     * @param clientIP 客户端IP地址
     * @return RateLimiter注册接口限流器
     */
    public RateLimiter getRegisterRateLimiter(String clientIP) {
        String key = REGISTER_KEY_PREFIX + clientIP;
        return rateLimiters.computeIfAbsent(key, k -> RateLimiter.create(5.0 / 60.0));
    }

    /**
     * 获取自定义限流器
     *
     * <p>根据限流key获取或创建自定义限流器。
     * 支持通过@RateLimit注解灵活配置不同接口的限流规则。
     *
     * <p>限流速率计算：maxRequests / timeWindowSeconds（每秒令牌数）
     *
     * @param key 限流唯一标识（如"rate_limit:/api/xxx:127.0.0.1"）
     * @param maxRequests 时间窗口内最大请求数
     * @param timeWindowSeconds 时间窗口大小（秒）
     * @return RateLimiter自定义限流器
     */
    public RateLimiter getCustomRateLimiter(String key, int maxRequests, int timeWindowSeconds) {
        return rateLimiters.computeIfAbsent(key, k ->
                RateLimiter.create((double) maxRequests / timeWindowSeconds));
    }

    /**
     * 获取登录限流的时间窗口（秒）
     *
     * <p>用于返回429响应时设置Retry-After头，
     * 告知客户端需要等待多久才能重试。
     *
     * @return 登录限流时间窗口（60秒）
     */
    public long getLoginRateLimitSeconds() {
        return 60;
    }

    /**
     * 获取注册限流的时间窗口（秒）
     *
     * <p>用于返回429响应时设置Retry-After头，
     * 告知客户端需要等待多久才能重试。
     *
     * @return 注册限流时间窗口（60秒）
     */
    public long getRegisterRateLimitSeconds() {
        return 60;
    }
}
