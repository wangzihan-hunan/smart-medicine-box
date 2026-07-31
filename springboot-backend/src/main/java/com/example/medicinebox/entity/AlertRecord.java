package com.example.medicinebox.entity;

import com.example.medicinebox.constant.AlertStatus;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 告警记录实体类
 * 对应数据库表：alert_records
 * 业务含义：记录系统告警的历史信息，包括告警类型、级别、状态及处理情况
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "alert_records", indexes = {
    @Index(name = "idx_alert_level", columnList = "level"),
    @Index(name = "idx_alert_type", columnList = "type"),
    @Index(name = "idx_alert_status", columnList = "status"),
    @Index(name = "idx_alert_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class AlertRecord {

    // 告警记录唯一标识，主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 告警类型：error_rate(错误率)、response_time(响应时间)、cpu_usage(CPU使用率)、memory_usage(内存使用率)、custom(自定义)
    @Column(nullable = false, length = 50)
    private String type;

    // 告警级别：INFO(信息)、WARNING(警告)、CRITICAL(严重)
    @Column(nullable = false, length = 20)
    private String level;

    // 告警标题
    @Column(nullable = false, length = 200)
    private String title;

    // 告警详细内容
    @Column(columnDefinition = "TEXT")
    private String content;

    // 触发告警的指标实际值
    @Column(name = "metric_value", length = 100)
    private String metricValue;

    // 告警阈值
    @Column(name = "threshold_value", length = 100)
    private String thresholdValue;

    // 告警状态：PENDING(待处理)、RESOLVED(已解决)、IGNORED(已忽略)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status = AlertStatus.PENDING;

    // 处理人
    @Column(name = "handled_by", length = 50)
    private String handledBy;

    // 处理时间
    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    // 处理备注
    @Column(name = "handle_remark", columnDefinition = "TEXT")
    private String handleRemark;

    // 告警创建时间
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==================== 领域方法 ====================

    /** 处理告警：标记为已解决 */
    public void resolve(String handledBy, String remark) {
        this.status = AlertStatus.RESOLVED;
        this.handledBy = handledBy;
        this.handledAt = LocalDateTime.now();
        this.handleRemark = remark;
    }

    /** 忽略告警 */
    public void ignore(String handledBy, String remark) {
        this.status = AlertStatus.IGNORED;
        this.handledBy = handledBy;
        this.handledAt = LocalDateTime.now();
        this.handleRemark = remark;
    }

    /** 告警是否待处理 */
    public boolean isPending() {
        return AlertStatus.PENDING == status;
    }

    /** 告警是否已处理 */
    public boolean isResolved() {
        return AlertStatus.RESOLVED == status;
    }
}