package com.example.medicinebox.service;

import com.example.medicinebox.config.AlertingProperties;
import com.example.medicinebox.entity.AlertRecord;
import com.example.medicinebox.repository.AlertRecordRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 系统告警服务类
 * 核心功能：周期性检查系统各项指标（异常率、响应时间、CPU使用率、内存使用率），
 * 超过阈值时触发告警，记录告警信息并发送邮件通知，支持告警风暴抑制
 */
@Service
public class AlertingService {

    private static final Logger log = LoggerFactory.getLogger(AlertingService.class);

    // 告警配置属性
    @Autowired
    private AlertingProperties properties;

    // 告警记录数据访问层
    @Autowired
    private AlertRecordRepository alertRecordRepository;

    // 邮件发送器（可选配置）
    @Autowired(required = false)
    private JavaMailSender mailSender;

    // 指标注册表
    @Autowired
    private MeterRegistry meterRegistry;

    // 上次告警时间映射，用于告警风暴抑制（避免短时间内重复告警）
    private final Map<String, LocalDateTime> lastAlertTime = new HashMap<>();

    /**
     * 服务初始化方法
     * Bean初始化完成后执行，输出告警服务启用状态
     */
    @PostConstruct
    public void init() {
        log.info("告警服务初始化完成, 告警启用: {}", properties.isEnabled());
    }

    /**
     * 定时检查各项指标 - 每分钟执行一次
     * 依次检查异常率、响应时间、CPU使用率、内存使用率
     */
    @Scheduled(fixedRate = 60000)
    public void checkMetrics() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            checkErrorRate();
            checkResponseTime();
            checkCpuUsage();
            checkMemoryUsage();
        } catch (Exception e) {
            log.error("告警检查异常", e);
        }
    }

    /**
     * 检查API异常率
     * 从Micrometer指标中统计HTTP请求错误率（4xx/5xx状态码），超过阈值则触发告警
     */
    private void checkErrorRate() {
        try {
            Search search = meterRegistry.find("http.server.requests");
            // 计算总请求数和错误请求数
            long totalRequests = 0;
            long errorRequests = 0;
            
            java.util.Collection<io.micrometer.core.instrument.Meter> meterCollection = search.meters();
            for (io.micrometer.core.instrument.Meter meter : meterCollection) {
                if (meter instanceof io.micrometer.core.instrument.Timer) {
                    io.micrometer.core.instrument.Timer timer = (io.micrometer.core.instrument.Timer) meter;
                    long count = timer.count();
                    String status = timer.getId().getTag("status");
                    // 4xx和5xx状态码视为错误请求
                    if (status != null && (status.startsWith("4") || status.startsWith("5"))) {
                        errorRequests += count;
                    }
                    totalRequests += count;
                }
            }
            
            // 无请求时不检查
            if (totalRequests == 0) {
                return;
            }
            
            double errorRate = (double) errorRequests / totalRequests;
            double threshold = properties.getRules().getErrorRate().getThreshold();
            
            // 异常率超过阈值且满足告警抑制条件则触发告警
            if (errorRate > threshold) {
                String alertKey = "error_rate";
                if (shouldAlert(alertKey)) {
                    String content = String.format("API 异常率 %.2f%% 超过阈值 %.2f%%（过去 %d 秒内总请求 %d，错误 %d）",
                            errorRate * 100, threshold * 100,
                            properties.getRules().getErrorRate().getWindowSeconds(),
                            totalRequests, errorRequests);
                    triggerAlert(alertKey, "WARNING", "API 异常率告警", content,
                            String.format("%.4f", errorRate), String.format("%.4f", threshold));
                }
            }
        } catch (Exception e) {
            log.error("检查异常率失败", e);
        }
    }

    /**
     * 检查API平均响应时间
     * 从自定义指标中计算平均响应时间，超过阈值则触发告警
     */
    private void checkResponseTime() {
        try {
            Search search = meterRegistry.find("http.custom.request.duration");
            java.util.Collection<io.micrometer.core.instrument.Meter> meterCollection = search.meters();
            
            if (meterCollection.isEmpty()) {
                return;
            }
            
            long totalCount = 0;
            double totalTimeMs = 0;
            for (io.micrometer.core.instrument.Meter meter : meterCollection) {
                if (meter instanceof io.micrometer.core.instrument.Timer) {
                    io.micrometer.core.instrument.Timer timer = (io.micrometer.core.instrument.Timer) meter;
                    long count = timer.count();
                    double mean = timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
                    totalCount += count;
                    totalTimeMs += mean * count;
                }
            }
            
            if (totalCount == 0) {
                return;
            }
            
            double avgTime = totalTimeMs / totalCount;
            long threshold = properties.getRules().getResponseTime().getThresholdMs();
            
            if (avgTime > threshold) {
                String alertKey = "response_time";
                if (shouldAlert(alertKey)) {
                    String content = String.format("API 平均响应时间 %.0fms 超过阈值 %dms",
                            avgTime, threshold);
                    triggerAlert(alertKey, "WARNING", "API 响应时间告警", content,
                            String.format("%.0fms", avgTime), threshold + "ms");
                }
            }
        } catch (Exception e) {
            log.error("检查响应时间失败", e);
        }
    }

    /**
     * 检查CPU使用率
     * 通过JMX获取系统负载，计算每CPU平均负载，超过阈值则触发严重告警
     */
    private void checkCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage() / osBean.getAvailableProcessors();
            
            if (cpuLoad < 0) return; // 系统不支持时返回 -1
            
            double threshold = properties.getRules().getCpuUsage().getThreshold();
            
            // CPU负载超过阈值则触发严重告警
            if (cpuLoad > threshold) {
                String alertKey = "cpu_usage";
                if (shouldAlert(alertKey)) {
                    String content = String.format("CPU 负载 %.2f 超过阈值 %.2f", cpuLoad, threshold);
                    triggerAlert(alertKey, "CRITICAL", "CPU 负载告警", content,
                            String.format("%.2f", cpuLoad), String.format("%.2f", threshold));
                }
            }
        } catch (Exception e) {
            log.error("检查 CPU 使用率失败", e);
        }
    }

    /**
     * 检查JVM堆内存使用率
     * 通过JMX获取堆内存使用情况，超过阈值则触发严重告警
     */
    private void checkMemoryUsage() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long used = memoryBean.getHeapMemoryUsage().getUsed();
            long max = memoryBean.getHeapMemoryUsage().getMax();
            
            if (max <= 0) return;
            
            double usage = (double) used / max;
            double threshold = properties.getRules().getMemoryUsage().getThreshold();
            
            if (usage > threshold) {
                String alertKey = "memory_usage";
                if (shouldAlert(alertKey)) {
                    String content = String.format("JVM 堆内存使用率 %.2f%% 超过阈值 %.2f%%（已用 %d MB / 最大 %d MB）",
                            usage * 100, threshold * 100, used / 1024 / 1024, max / 1024 / 1024);
                    triggerAlert(alertKey, "CRITICAL", "内存使用率告警", content,
                            String.format("%.4f", usage), String.format("%.4f", threshold));
                }
            }
        } catch (Exception e) {
            log.error("检查内存使用率失败", e);
        }
    }

    /**
     * 判断是否应该触发告警（告警风暴抑制）
     * 同类型告警5分钟内只发送一次，避免告警风暴
     *
     * @param alertKey 告警类型键
     * @return true-应该触发告警，false-抑制告警
     */
    private boolean shouldAlert(String alertKey) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = lastAlertTime.get(alertKey);
        // 首次告警或距离上次告警超过5分钟则触发
        if (lastTime == null || now.isAfter(lastTime.plusMinutes(5))) {
            lastAlertTime.put(alertKey, now);
            return true;
        }
        return false;
    }

    /**
     * 触发告警
     * 保存告警记录到数据库，根据配置决定是否发送邮件通知
     *
     * @param type 告警类型
     * @param level 告警级别（WARNING/CRITICAL等）
     * @param title 告警标题
     * @param content 告警内容
     * @param metricValue 当前指标值
     * @param threshold 阈值
     */
    public void triggerAlert(String type, String level, String title, String content,
                              String metricValue, String threshold) {
        log.warn("触发告警: {} - {} - {}", type, level, title);
        
        AlertRecord alert = new AlertRecord();
        alert.setType(type);
        alert.setLevel(level);
        alert.setTitle(title);
        alert.setContent(content);
        alert.setMetricValue(metricValue);
        alert.setThresholdValue(threshold);
        alert.setStatus("PENDING");
        
        try {
            alertRecordRepository.save(alert);
        } catch (Exception e) {
            log.error("保存告警记录失败", e);
        }
        
        // 邮件告警功能启用且邮件发送器存在时，发送邮件
        if (properties.getEmail().isEnabled() && mailSender != null) {
            try {
                sendEmailAlert(title, content, level);
            } catch (Exception e) {
                log.error("发送告警邮件失败", e);
            }
        }
    }

    /**
     * 发送告警邮件
     *
     * @param title 邮件标题
     * @param content 邮件内容
     * @param level 告警级别
     */
    private void sendEmailAlert(String title, String content, String level) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getEmail().getFrom());
        message.setTo(properties.getEmail().getTo().split(","));
        message.setSubject("[智能药箱告警][" + level + "] " + title);
        message.setText(content + "\n\n告警时间: " + LocalDateTime.now());
        mailSender.send(message);
    }

    /**
     * 手动触发告警（用于测试）
     *
     * @param type 告警类型
     * @param level 告警级别
     * @param title 告警标题
     * @param content 告警内容
     * @return 保存后的告警记录
     */
    public AlertRecord manualTrigger(String type, String level, String title, String content) {
        AlertRecord alert = new AlertRecord();
        alert.setType(type);
        alert.setLevel(level);
        alert.setTitle(title);
        alert.setContent(content);
        alert.setStatus("PENDING");
        return alertRecordRepository.save(alert);
    }

    /**
     * 分页查询告警列表
     * 支持按状态筛选
     *
     * @param status 告警状态（可选）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 告警记录分页结果
     */
    public Page<AlertRecord> getAlerts(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null && !status.isEmpty()) {
            return alertRecordRepository.findByStatus(status, pageable);
        }
        return alertRecordRepository.findAll(pageable);
    }

    /**
     * 处理告警
     * 设置处理人、处理时间、处理备注和处理状态
     *
     * @param id 告警记录ID
     * @param handledBy 处理人
     * @param remark 处理备注
     * @param status 处理后状态
     * @return 更新后的告警记录
     * @throws IllegalArgumentException 告警记录不存在时抛出
     */
    public AlertRecord handleAlert(Long id, String handledBy, String remark, String status) {
        AlertRecord alert = alertRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警记录不存在: " + id));
        alert.setHandledBy(handledBy);
        alert.setHandledAt(LocalDateTime.now());
        alert.setHandleRemark(remark);
        alert.setStatus(status);
        return alertRecordRepository.save(alert);
    }

    /**
     * 统计告警数量
     * 包含总数、待处理数、已解决数、已忽略数
     *
     * @return 告警统计Map
     */
    public Map<String, Long> getAlertStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", alertRecordRepository.count());
        stats.put("pending", alertRecordRepository.countByStatus("PENDING"));
        stats.put("resolved", alertRecordRepository.countByStatus("RESOLVED"));
        stats.put("ignored", alertRecordRepository.countByStatus("IGNORED"));
        return stats;
    }
}
