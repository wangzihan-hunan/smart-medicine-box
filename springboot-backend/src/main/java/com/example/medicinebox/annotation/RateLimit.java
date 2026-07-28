package com.example.medicinebox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 *
 * <p>用于标记需要进行限流的Controller方法。
 * 配合RateLimitInterceptor使用，对标记的方法按IP进行限流。
 *
 * <p>使用示例：
 * <pre>
 * {@literal @}RateLimit(maxRequests = 20, timeWindowSeconds = 60)
 *  public ResponseEntity&lt;?&gt; someMethod() {
 *      // 方法体
 *  }
 * </pre>
 *
 * <p>限流规则：在指定的时间窗口内，每个IP最多只能发起maxRequests次请求。
 * 超过限制后返回429 Too Many Requests响应。
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 时间窗口内允许的最大请求数
     * 默认值：10次
     *
     * @return 最大请求数
     */
    int maxRequests() default 10;

    /**
     * 限流时间窗口（秒）
     * 默认值：60秒
     *
     * @return 时间窗口大小（秒）
     */
    int timeWindowSeconds() default 60;
}
