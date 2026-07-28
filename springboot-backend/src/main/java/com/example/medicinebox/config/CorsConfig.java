package com.example.medicinebox.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS跨域配置类
 *
 * <p>配置跨域资源共享（Cross-Origin Resource Sharing）策略，
 * 允许前端应用从不同的域名或端口访问后端API。
 *
 * <p>主要配置内容：
 * <ul>
 *   <li>允许的跨域来源（从配置文件读取）</li>
 *   <li>允许的请求头（所有）</li>
 *   <li>允许的请求方法（所有：GET/POST/PUT/DELETE等）</li>
 *   <li>允许携带凭证（Cookie/Authorization头）</li>
 *   <li>预检请求缓存时间（3600秒）</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Configuration
public class CorsConfig {

    /**
     * 允许的跨域来源列表
     * 从配置文件cors.allowed-origins读取，多个来源用逗号分隔
     * 例如：http://localhost:3000,https://example.com
     */
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * CORS过滤器Bean
     *
     * <p>创建并配置CorsFilter，对所有路径应用跨域配置。
     *
     * <p>配置细节：
     * <ul>
     *   <li>allowedOrigins: 从配置文件读取，支持多个来源（逗号分隔）</li>
     *   <li>allowedHeaders: 允许所有请求头</li>
     *   <li>allowedMethods: 允许所有HTTP方法</li>
     *   <li>allowCredentials: 允许携带认证凭证（如Cookie、Authorization头）</li>
     *   <li>maxAge: 预检请求（OPTIONS）的缓存时间，3600秒（1小时）</li>
     * </ul>
     *
     * @return CorsFilter跨域过滤器实例
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 解析配置文件中的允许来源，逗号分隔
        String[] origins = allowedOrigins.split(",");
        for (String origin : origins) {
            config.addAllowedOrigin(origin.trim());
        }

        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许所有请求方法（GET, POST, PUT, DELETE等）
        config.addAllowedMethod("*");
        // 允许携带凭证（如Cookie、Authorization头）
        config.setAllowCredentials(true);
        // 预检请求缓存时间，单位：秒（3600秒 = 1小时）
        config.setMaxAge(3600L);

        // 对所有URL路径应用此CORS配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
