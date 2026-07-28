package com.example.medicinebox.interceptor;

import com.example.medicinebox.annotation.RateLimit;
import com.example.medicinebox.config.RateLimitConfig;
import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.util.IpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 限流拦截器
 *
 * <p>实现HandlerInterceptor接口，在请求处理前进行限流检查。
 * 基于Guava RateLimiter实现令牌桶限流算法，防止恶意请求刷屏。
 *
 * <p>限流策略：
 * <ul>
 *   <li>登录接口：每IP每分钟10次</li>
 *   <li>注册接口：每IP每分钟5次</li>
 *   <li>自定义限流：通过@RateLimit注解在方法上配置</li>
 * </ul>
 *
 * <p>当请求被限流时，返回429 Too Many Requests状态码和JSON格式的错误信息，
 * 并设置Retry-After响应头告知客户端重试等待时间。
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    /** 限流配置类，提供各种限流器 */
    private final RateLimitConfig rateLimitConfig;

    /** JSON对象映射器，用于序列化响应 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，注入依赖
     *
     * @param rateLimitConfig 限流配置类
     * @param objectMapper JSON对象映射器
     */
    public RateLimitInterceptor(RateLimitConfig rateLimitConfig, ObjectMapper objectMapper) {
        this.rateLimitConfig = rateLimitConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * 请求预处理 - 限流检查
     *
     * <p>在Controller方法执行前进行限流检查：
     * <ol>
     *   <li>如果处理器不是方法（如静态资源），直接放行</li>
     *   <li>登录接口：检查登录限流规则</li>
     *   <li>注册接口：检查注册限流规则</li>
     *   <li>其他接口：检查方法上是否有@RateLimit注解，有则执行自定义限流</li>
     *   <li>通过限流检查则返回true继续处理，否则返回false中断请求</li>
     * </ol>
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理器对象
     * @return true-继续处理请求；false-中断请求
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是方法处理器（如静态资源），直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        String requestURI = request.getRequestURI();
        String clientIP = IpUtils.getClientIP(request);

        // 登录接口限流检查
        if (requestURI.contains("/api/v1/auth/login")) {
            return checkLoginRateLimit(clientIP, response);
        }

        // 注册接口限流检查
        if (requestURI.contains("/api/v1/auth/register")) {
            return checkRegisterRateLimit(clientIP, response);
        }

        // 方法上有@RateLimit注解时，执行自定义限流
        if (method.isAnnotationPresent(RateLimit.class)) {
            RateLimit rateLimit = method.getAnnotation(RateLimit.class);
            return checkCustomRateLimit(clientIP, requestURI, rateLimit, response);
        }

        // 无限制流规则，放行
        return true;
    }

    /**
     * 检查登录接口限流
     *
     * <p>每IP每分钟最多10次登录请求，防止暴力破解密码。
     *
     * @param clientIP 客户端IP地址
     * @param response HTTP响应对象
     * @return true-未超限，继续处理；false-已超限，中断请求
     * @throws IOException 写入响应时可能抛出的IO异常
     */
    private boolean checkLoginRateLimit(String clientIP, HttpServletResponse response) throws IOException {
        RateLimiter rateLimiter = rateLimitConfig.getLoginRateLimiter(clientIP);

        // 尝试获取令牌，成功则返回true
        if (rateLimiter.tryAcquire()) {
            return true;
        }

        // 限流触发，记录日志并返回429响应
        log.warn("登录请求频率超限: IP={}", clientIP);
        returnRateLimitResponse(response, rateLimitConfig.getLoginRateLimitSeconds());
        return false;
    }

    /**
     * 检查注册接口限流
     *
     * <p>每IP每分钟最多5次注册请求，防止恶意批量注册。
     *
     * @param clientIP 客户端IP地址
     * @param response HTTP响应对象
     * @return true-未超限，继续处理；false-已超限，中断请求
     * @throws IOException 写入响应时可能抛出的IO异常
     */
    private boolean checkRegisterRateLimit(String clientIP, HttpServletResponse response) throws IOException {
        RateLimiter rateLimiter = rateLimitConfig.getRegisterRateLimiter(clientIP);

        // 尝试获取令牌，成功则返回true
        if (rateLimiter.tryAcquire()) {
            return true;
        }

        // 限流触发，记录日志并返回429响应
        log.warn("注册请求频率超限: IP={}", clientIP);
        returnRateLimitResponse(response, rateLimitConfig.getRegisterRateLimitSeconds());
        return false;
    }

    /**
     * 检查自定义限流
     *
     * <p>根据@RateLimit注解配置的参数进行限流检查。
     * 限流key由请求URI和客户端IP组成，确保按IP限流。
     *
     * @param clientIP 客户端IP地址
     * @param requestURI 请求URI
     * @param rateLimit 限流注解，包含限流参数
     * @param response HTTP响应对象
     * @return true-未超限，继续处理；false-已超限，中断请求
     * @throws IOException 写入响应时可能抛出的IO异常
     */
    private boolean checkCustomRateLimit(String clientIP, String requestURI,
                                         RateLimit rateLimit, HttpServletResponse response) throws IOException {
        String key = "rate_limit:" + requestURI + ":" + clientIP;
        RateLimiter rateLimiter = rateLimitConfig.getCustomRateLimiter(key,
                rateLimit.maxRequests(), rateLimit.timeWindowSeconds());

        // 尝试获取令牌，成功则返回true
        if (rateLimiter.tryAcquire()) {
            return true;
        }

        // 限流触发，记录日志并返回429响应
        log.warn("请求频率超限: URI={}, IP={}", requestURI, clientIP);
        returnRateLimitResponse(response, rateLimit.timeWindowSeconds());
        return false;
    }

    /**
     * 返回限流响应
     *
     * <p>构建429 Too Many Requests响应，包含：
     * <ul>
     *   <li>HTTP状态码：429</li>
     *   <li>Content-Type：application/json</li>
     *   <li>Retry-After头：重试等待时间（秒）</li>
     *   <li>响应体：统一格式的JSON错误信息</li>
     * </ul>
     *
     * @param response HTTP响应对象
     * @param retryAfterSeconds 重试等待时间（秒）
     * @throws IOException 写入响应时可能抛出的IO异常
     */
    private void returnRateLimitResponse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        // 设置HTTP状态码 429 Too Many Requests
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        // 设置响应内容类型为JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 设置字符编码为UTF-8
        response.setCharacterEncoding("UTF-8");
        // 设置Retry-After响应头，告知客户端多久后可以重试
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        // 构建统一的基础响应对象
        BaseResponse<Void> baseResponse = new BaseResponse<>(429, "error",
                "请求过于频繁，请稍后再试", null);
        // 将响应对象序列化为JSON并写入响应体
        response.getWriter().write(objectMapper.writeValueAsString(baseResponse));
    }
}
