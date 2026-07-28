package com.example.medicinebox.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 指标监控配置类
 *
 * <p>配置Micrometer指标监控相关设置，扩展Spring Boot Actuator的指标功能。
 * 用于注册自定义业务指标和公共标签。
 *
 * <p>主要功能：
 * <ul>
 *   <li>配置全局公共标签（如service名称），便于在Prometheus中区分不同服务</li>
 *   <li>预留JVM指标扩展点（当前使用Micrometer自动注册的JVM指标）</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Configuration
public class MetricsConfig {

    /**
     * JVM指标绑定器Bean
     *
     * <p>预留的JVM指标扩展点。
     * Micrometer已自动注册了常用的JVM指标（如堆内存、线程、GC等），
     * 如需添加自定义JVM相关指标，可在此方法中注册。
     *
     * @return MeterBinder JVM指标绑定器
     */
    @Bean
    public MeterBinder jvmMetricsBinder() {
        return registry -> {
            // JVM 指标已由 Micrometer 自动注册
            // 这里可以添加自定义的 JVM 指标配置
        };
    }

    /**
     * 配置指标公共标签
     *
     * <p>为所有指标添加公共标签"service=medicine-box"，
     * 便于在Prometheus/Grafana中按服务维度筛选和聚合指标。
     *
     * <p>公共标签会自动附加到所有Micrometer注册的指标上，
     * 包括系统指标、JVM指标和自定义业务指标。
     *
     * @param registry MeterRegistry指标注册表
     * @return MeterRegistry.Config 指标配置对象
     */
    @Bean
    public MeterRegistry.Config metricsCommonTags(MeterRegistry registry) {
        return registry.config().commonTags("service", "medicine-box");
    }
}
