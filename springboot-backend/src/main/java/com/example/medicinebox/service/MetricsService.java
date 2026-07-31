package com.example.medicinebox.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务指标服务类
 * 基于Micrometer提供业务相关的指标采集能力，包括：
 * - 用户注册/登录计数
 * - AI识别调用统计（成功/失败）
 * - OCR识别调用统计
 * - 业务异常统计
 * - 药方上传统计
 * - API调用耗时统计
 * - 活跃用户/在线用户统计（Gauge）
 */
@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final MeterRegistry registry;

    // 业务计数器 - 用户注册计数
    private final Counter userRegisterCounter;
    // 业务计数器 - 用户登录成功计数
    private final Counter userLoginSuccessCounter;
    // 业务计数器 - 用户登录失败计数
    private final Counter userLoginFailCounter;
    // 业务计数器 - AI识别总调用计数
    private final Counter aiRecognitionCounter;
    // 业务计数器 - AI识别成功计数
    private final Counter aiRecognitionSuccessCounter;
    // 业务计数器 - AI识别失败计数
    private final Counter aiRecognitionFailCounter;
    // 业务计数器 - OCR识别调用计数
    private final Counter ocrRecognitionCounter;
    // 业务计数器 - 业务异常计数
    private final Counter businessExceptionCounter;
    // 业务计数器 - 药方上传计数
    private final Counter prescriptionUploadCounter;

    // Gauge指标 - 活跃用户数
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    // Gauge指标 - 在线用户数
    private final AtomicInteger onlineUsers = new AtomicInteger(0);

    /**
     * 构造函数，初始化所有业务指标计数器并注册到指标注册表
     *
     * @param registry Micrometer指标注册表
     */
    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
        
        // 注册用户注册计数器
        this.userRegisterCounter = Counter.builder("business.user.register")
                .description("用户注册总数")
                .register(registry);
        
        // 注册用户登录计数器（成功）
        this.userLoginSuccessCounter = Counter.builder("business.user.login")
                .description("用户登录总数")
                .tag("result", "success")
                .register(registry);
        
        // 注册用户登录计数器（失败）
        this.userLoginFailCounter = Counter.builder("business.user.login")
                .description("用户登录总数")
                .tag("result", "fail")
                .register(registry);
        
        // 注册AI识别计数器（总数
        this.aiRecognitionCounter = Counter.builder("business.ai.recognition")
                .description("AI 识别调用总数")
                .register(registry);
        
        // 注册AI识别计数器（成功）
        this.aiRecognitionSuccessCounter = Counter.builder("business.ai.recognition")
                .description("AI 识别调用总数")
                .tag("result", "success")
                .register(registry);
        
        // 注册AI识别计数器（失败）
        this.aiRecognitionFailCounter = Counter.builder("business.ai.recognition")
                .description("AI 识别调用总数")
                .tag("result", "fail")
                .register(registry);
        
        // 注册OCR识别计数器
        this.ocrRecognitionCounter = Counter.builder("business.ocr.recognition")
                .description("OCR 识别调用总数")
                .register(registry);
        
        // 注册业务异常计数器
        this.businessExceptionCounter = Counter.builder("business.exception")
                .description("业务异常总数")
                .register(registry);
        
        // 注册药方上传计数器
        this.prescriptionUploadCounter = Counter.builder("business.prescription.upload")
                .description("药方上传总数")
                .register(registry);
        
        // 注册Gauge指标（活跃用户数、在线用户数）
        registry.gauge("business.user.active", activeUsers);
        registry.gauge("business.user.online", onlineUsers);
        
        log.info("业务指标服务初始化完成");
    }

    /**
     * 记录一次用户注册
     */
    public void recordUserRegister() {
        userRegisterCounter.increment();
    }

    /**
     * 记录一次用户登录
     *
     * @param success 是否登录成功
     */
    public void recordUserLogin(boolean success) {
        if (success) {
            userLoginSuccessCounter.increment();
        } else {
            userLoginFailCounter.increment();
        }
    }

    /**
     * 记录一次AI识别调用
     * 同时增加总调用计数和成功/失败分项计数
     *
     * @param success 是否识别成功
     */
    public void recordAiRecognition(boolean success) {
        aiRecognitionCounter.increment();
        if (success) {
            aiRecognitionSuccessCounter.increment();
        } else {
            aiRecognitionFailCounter.increment();
        }
    }

    /**
     * 记录一次OCR识别调用
     */
    public void recordOcrRecognition() {
        ocrRecognitionCounter.increment();
    }

    /**
     * 记录一次药方上传
     */
    public void recordPrescriptionUpload() {
        prescriptionUploadCounter.increment();
    }

    /**
     * 记录业务异常
     * 按异常类型分别计数，同时累计总异常数
     *
     * @param type 异常类型
     */
    public void recordBusinessException(String type) {
        registry.counter("business.exception", "type", type).increment();
        businessExceptionCounter.increment();
    }

    /**
     * 记录API调用耗时
     * 按API路径分别统计响应时间
     *
     * @param api API路径/名称
     * @param millis 耗时（毫秒）
     */
    public void recordApiLatency(String api, long millis) {
        Timer.builder("business.api.latency")
                .description("业务 API 调用耗时")
                .tag("api", api)
                .register(registry)
                .record(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录自定义计数器
     * 支持动态创建带标签的计数器并递增
     *
     * @param name 计数器名称
     * @param tags 标签数组（key-value交替）
     */
    public void incrementCounter(String name, String... tags) {
        Counter counter = Counter.builder(name)
                .tags(Tags.of(tags))
                .register(registry);
        counter.increment();
    }

    /**
     * 更新活跃用户数（Gauge指标）
     *
     * @param count 活跃用户数
     */
    public void setActiveUsers(int count) {
        activeUsers.set(count);
    }

    /**
     * 更新在线用户数（Gauge指标）
     *
     * @param count 在线用户数
     */
    public void setOnlineUsers(int count) {
        onlineUsers.set(count);
    }
}
