package com.example.medicinebox.service;

import com.example.medicinebox.config.AlertingProperties;
import com.example.medicinebox.constant.AlertStatus;
import com.example.medicinebox.entity.AlertRecord;
import com.example.medicinebox.repository.AlertRecordRepository;
import com.example.medicinebox.util.PaginationUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 系统告警服务类
 * 核心功能：周期性检查系统各项指标（异常率、响应时间、CPU使用率、内存使用率），
 * 超过阈值时触发告警，记录告警信息并发送邮件通知，支持告警风暴抑制
 */
@Service
public class AlertingService {

    private static final Logger log = LoggerFactory.getLogger(AlertingService.class);

    private final AlertingProperties properties;
    private final AlertRecordRepository alertRecordRepository;
    /** 邮件发送器（可选依赖，通过 Optional 构造函数注入，无 Mail 配置时自动为 null） */
    private final JavaMailSender mailSender;
    private final MeterRegistry meterRegistry;

    /**
     * 告警风暴抑制映射，使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<String, LocalDateTime> lastAlertTime = new ConcurrentHashMap<>();

    /**
     * 上一轮指标快照，用于 delta 计算，通过 AtomicLong 保证原子操作
     * 每次检查时计算 delta = 当前累计值 - 上一轮快照，仅反映最近检查窗口内的变化
     */
    private final AtomicLong previousTotalRequests = new AtomicLong(0);
    private final AtomicLong previousErrorRequests = new AtomicLong(0);
    private final AtomicLong previousResponseCount = new AtomicLong(0);
    private final AtomicLong previousResponseTotalTimeNs = new AtomicLong(0);

    public AlertingService(AlertingProperties properties,
                           AlertRecordRepository alertRecordRepository,
                           Optional<JavaMailSender> mailSender,
                           MeterRegistry meterRegistry) {
        this.properties = properties;
        this.alertRecordRepository = alertRecordRepository;
        this.mailSender = mailSender.orElse(null);
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        log.info("告警服务初始化完成, 告警启用: {}", properties.isEnabled());
    }

    /**
     * 定时检查各项指标 - 每分钟执行一次
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
     *
     * <p>通过 delta 计算（当前累计值 - 上一轮快照）获取最近检查窗口内的请求数据，
     * 异常率 = delta_errors / delta_total，避免累计值导致的历史数据稀释问题。
     */
    private void checkErrorRate() {
        try {
            Search search = meterRegistry.find("http.server.requests");
            long currentTotal = 0;
            long currentErrors = 0;

            java.util.Collection<io.micrometer.core.instrument.Meter> meterCollection = search.meters();
            for (io.micrometer.core.instrument.Meter meter : meterCollection) {
                if (meter instanceof io.micrometer.core.instrument.Timer) {
                    io.micrometer.core.instrument.Timer timer = (io.micrometer.core.instrument.Timer) meter;
                    long count = timer.count();
                    String status = timer.getId().getTag("status");
                    if (status != null && (status.startsWith("4") || status.startsWith("5"))) {
                        currentErrors += count;
                    }
                    currentTotal += count;
                }
            }

            // 计算本窗口内的增量
            long prevTotal = previousTotalRequests.getAndSet(currentTotal);
            long prevErrors = previousErrorRequests.getAndSet(currentErrors);
            long deltaTotal = safeDelta(currentTotal, prevTotal);
            long deltaErrors = safeDelta(currentErrors, prevErrors);

            if (deltaTotal == 0) {
                return;
            }

            double errorRate = (double) deltaErrors / deltaTotal;
            double threshold = properties.getRules().getErrorRate().getThreshold();

            if (errorRate > threshold) {
                String alertKey = "error_rate";
                if (shouldAlert(alertKey)) {
                    String content = String.format("API 异常率 %.2f%% 超过阈值 %.2f%%（最近窗口内请求 %d，错误 %d）",
                            errorRate * 100, threshold * 100, deltaTotal, deltaErrors);
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
     *
     * <p>通过 totalTime() 和 count() 的 delta 计算最近窗口内的平均响应时间，
     * 避免累计值导致的历史数据稀释问题。
     */
    private void checkResponseTime() {
        try {
            Search search = meterRegistry.find("http.custom.request.duration");
            java.util.Collection<io.micrometer.core.instrument.Meter> meterCollection = search.meters();

            if (meterCollection.isEmpty()) {
                return;
            }

            long currentCount = 0;
            long currentTotalTimeNs = 0;
            for (io.micrometer.core.instrument.Meter meter : meterCollection) {
                if (meter instanceof io.micrometer.core.instrument.Timer) {
                    io.micrometer.core.instrument.Timer timer = (io.micrometer.core.instrument.Timer) meter;
                    currentCount += timer.count();
                    currentTotalTimeNs += timer.totalTime(TimeUnit.NANOSECONDS);
                }
            }

            long prevCount = previousResponseCount.getAndSet(currentCount);
            long prevTotalTimeNs = previousResponseTotalTimeNs.getAndSet(currentTotalTimeNs);
            long deltaCount = safeDelta(currentCount, prevCount);
            long deltaTotalTimeNs = safeDelta(currentTotalTimeNs, prevTotalTimeNs);

            if (deltaCount == 0) {
                return;
            }

            double avgTimeMs = deltaTotalTimeNs / (double) deltaCount / 1_000_000.0;
            long threshold = properties.getRules().getResponseTime().getThresholdMs();

            if (avgTimeMs > threshold) {
                String alertKey = "response_time";
                if (shouldAlert(alertKey)) {
                    String content = String.format("API 平均响应时间 %.0fms 超过阈值 %dms",
                            avgTimeMs, threshold);
                    triggerAlert(alertKey, "WARNING", "API 响应时间告警", content,
                            String.format("%.0fms", avgTimeMs), threshold + "ms");
                }
            }
        } catch (Exception e) {
            log.error("检查响应时间失败", e);
        }
    }

    /**
     * 安全计算 delta 值
     * 当累计值异常回退（如应用重启）时，以当前值作为 delta
     */
    private long safeDelta(long current, long previous) {
        if (current < previous) {
            return current;
        }
        return current - previous;
    }

    /**
     * 检查CPU使用率
     */
    private void checkCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage() / osBean.getAvailableProcessors();

            if (cpuLoad < 0) return;

            double threshold = properties.getRules().getCpuUsage().getThreshold();

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
     * 同类型告警5分钟内只发送一次，使用 ConcurrentHashMap 保证线程安全
     */
    private boolean shouldAlert(String alertKey) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = lastAlertTime.get(alertKey);
        if (lastTime == null || now.isAfter(lastTime.plusMinutes(5))) {
            lastAlertTime.put(alertKey, now);
            return true;
        }
        return false;
    }

    /**
     * 触发告警
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
        alert.setStatus(AlertStatus.PENDING);

        try {
            alertRecordRepository.save(alert);
        } catch (Exception e) {
            log.error("保存告警记录失败", e);
        }

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
     */
    public AlertRecord manualTrigger(String type, String level, String title, String content) {
        AlertRecord alert = new AlertRecord();
        alert.setType(type);
        alert.setLevel(level);
        alert.setTitle(title);
        alert.setContent(content);
        alert.setStatus(AlertStatus.PENDING);
        return alertRecordRepository.save(alert);
    }

    /**
     * 分页查询告警列表，支持按状态筛选，pageSize 上限 100
     */
    @Transactional(readOnly = true)
    public Page<AlertRecord> getAlerts(String status, int page, int size) {
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize);
        if (status != null && !status.isEmpty()) {
            return alertRecordRepository.findByStatus(AlertStatus.valueOf(status), pageable);
        }
        return alertRecordRepository.findAll(pageable);
    }

    /**
     * 处理告警
     */
    public AlertRecord handleAlert(Long id, String handledBy, String remark, String status) {
        AlertRecord alert = alertRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警记录不存在: " + id));
        AlertStatus newStatus = AlertStatus.valueOf(status);
        if (AlertStatus.RESOLVED == newStatus) {
            alert.resolve(handledBy, remark);
        } else if (AlertStatus.IGNORED == newStatus) {
            alert.ignore(handledBy, remark);
        } else {
            alert.setHandledBy(handledBy);
            alert.setHandledAt(LocalDateTime.now());
            alert.setHandleRemark(remark);
            alert.setStatus(newStatus);
        }
        return alertRecordRepository.save(alert);
    }

    /**
     * 统计告警数量
     *
     * <p>使用一次聚合查询（COUNT + SUM + CASE WHEN）同时返回 total、pending、resolved、ignored 四种统计值。
     */
    public Map<String, Long> getAlertStats() {
        List<Object[]> result = alertRecordRepository.countStats();
        Map<String, Long> stats = new HashMap<>();

        if (!result.isEmpty() && result.get(0)[0] != null) {
            Object[] row = result.get(0);
            stats.put("total", ((Number) row[0]).longValue());
            stats.put("pending", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            stats.put("resolved", row[2] != null ? ((Number) row[2]).longValue() : 0L);
            stats.put("ignored", row[3] != null ? ((Number) row[3]).longValue() : 0L);
        } else {
            stats.put("total", 0L);
            stats.put("pending", 0L);
            stats.put("resolved", 0L);
            stats.put("ignored", 0L);
        }

        return stats;
    }
}