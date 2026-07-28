package com.example.medicinebox.config;

import com.example.medicinebox.interceptor.MetricsInterceptor;
import com.example.medicinebox.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 *
 * <p>实现WebMvcConfigurer接口，配置Spring MVC相关的设置，包括：
 * <ul>
 *   <li>静态资源处理器映射</li>
 *   <li>自定义拦截器注册</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 限流拦截器，用于控制API请求频率 */
    private final RateLimitInterceptor rateLimitInterceptor;

    /** 指标采集拦截器，用于收集API请求的性能指标 */
    private final MetricsInterceptor metricsInterceptor;

    /**
     * 构造函数，注入拦截器依赖
     *
     * @param rateLimitInterceptor 限流拦截器
     * @param metricsInterceptor 指标采集拦截器
     */
    public WebConfig(RateLimitInterceptor rateLimitInterceptor, MetricsInterceptor metricsInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.metricsInterceptor = metricsInterceptor;
    }

    /**
     * 添加静态资源处理器
     *
     * <p>配置静态资源的访问路径和存储位置：
     * <ul>
     *   <li>访问路径：/static/**</li>
     *   <li>classpath:/static/ - 项目classpath下的static目录（打包在jar内）</li>
     *   <li>file:./static/ - 项目运行目录下的static目录（外部静态文件）</li>
     * </ul>
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/", "file:./static/");
    }

    /**
     * 添加自定义拦截器
     *
     * <p>注册两个拦截器：
     * <ol>
     *   <li><b>限流拦截器（RateLimitInterceptor）</b>：
     *       <ul>
     *         <li>拦截路径：登录接口、注册接口、所有/api/**路径</li>
     *         <li>作用：防止恶意请求刷屏，保护系统安全</li>
     *       </ul>
     *   </li>
     *   <li><b>指标采集拦截器（MetricsInterceptor）</b>：
     *       <ul>
     *         <li>拦截路径：所有/api/**路径</li>
     *         <li>作用：收集请求响应时间、状态码等性能指标</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // API 限流拦截器
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/auth/login", "/api/auth/register")
                .addPathPatterns("/api/**");

        // 指标采集拦截器（采集所有 API 指标）
        registry.addInterceptor(metricsInterceptor)
                .addPathPatterns("/api/**");
    }
}
