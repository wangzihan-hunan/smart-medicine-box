package com.example.medicinebox.interceptor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * API指标采集拦截器
 *
 * <p>实现HandlerInterceptor接口，自动采集所有HTTP API请求的性能指标。
 * 使用Micrometer记录请求响应时间和异常计数，便于Prometheus监控和告警。
 *
 * <p>采集的指标包括：
 * <ul>
 *   <li>http.custom.request.duration：HTTP请求响应时间（Timer）</li>
 *   <li>http.custom.request.exception：HTTP请求异常计数（Counter）</li>
 * </ul>
 *
 * <p>指标标签：
 * <ul>
 *   <li>method：HTTP请求方法（GET/POST/PUT/DELETE等）</li>
 *   <li>uri：请求URI（已简化，避免高基数问题）</li>
 *   <li>status：HTTP响应状态码</li>
 *   <li>exception：异常类名（仅异常指标有此标签）</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Component
public class MetricsInterceptor implements HandlerInterceptor {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(MetricsInterceptor.class);

    /** 请求开始时间属性名，用于在request作用域传递时间戳 */
    private static final String START_TIME_ATTRIBUTE = "metrics.start.time";

    /** Micrometer指标注册表，用于注册和记录指标 */
    private final MeterRegistry registry;

    /**
     * 构造函数，注入指标注册表
     *
     * @param registry Micrometer指标注册表
     */
    public MetricsInterceptor(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 请求预处理 - 记录开始时间
     *
     * <p>在Controller方法执行前记录请求开始时间，
     * 存储在request属性中供后续计算响应时间使用。
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理器对象
     * @return true-继续处理请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录请求开始时间（毫秒）
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    /**
     * 请求完成后处理 - 采集指标
     *
     * <p>在请求处理完成后（无论成功或失败）采集性能指标：
     * <ol>
     *   <li>从request属性中获取开始时间</li>
     *   <li>计算请求耗时（当前时间 - 开始时间）</li>
     *   <li>获取简化后的URI（替换数字ID为{id}占位符，避免高基数问题）</li>
     *   <li>记录响应时间指标（Timer）</li>
     *   <li>如果有异常，记录异常计数指标（Counter）</li>
     * </ol>
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理器对象
     * @param ex 处理过程中抛出的异常（如果有）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 获取请求开始时间
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime == null) {
            return;
        }

        // 计算请求耗时（毫秒）
        long duration = System.currentTimeMillis() - startTime;
        // 获取简化后的URI
        String uri = getSimplifiedUri(request);
        // 获取HTTP请求方法
        String method = request.getMethod();
        // 获取HTTP响应状态码
        int status = response.getStatus();

        // 记录响应时间指标
        Timer.builder("http.custom.request.duration")
                .description("HTTP 请求响应时间")
                .tag("method", method)
                .tag("uri", uri)
                .tag("status", String.valueOf(status))
                .register(registry)
                .record(duration, TimeUnit.MILLISECONDS);

        // 如果有异常，记录异常计数指标
        if (ex != null) {
            registry.counter("http.custom.request.exception",
                    "method", method,
                    "uri", uri,
                    "exception", ex.getClass().getSimpleName())
                .increment();
        }
    }

    /**
     * 简化URI，避免高基数问题
     *
     * <p>将URI中的数字ID替换为{id}占位符，防止因为ID不同导致指标基数过高。
     * 例如：/api/medicines/123 -> /api/medicines/{id}
     *
     * <p>高基数问题会导致Prometheus存储压力增大，因此需要对URI进行归一化处理。
     *
     * @param request HTTP请求对象
     * @return 简化后的URI字符串
     */
    private String getSimplifiedUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 将路径中的数字ID替换为占位符，避免高基数问题
        return uri.replaceAll("/\\d+", "/{id}");
    }
}
