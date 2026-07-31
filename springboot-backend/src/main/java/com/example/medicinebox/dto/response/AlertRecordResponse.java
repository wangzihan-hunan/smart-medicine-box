package com.example.medicinebox.dto.response;

import com.example.medicinebox.entity.AlertRecord;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警记录详情响应DTO
 * 用于返回单条系统告警记录的详细信息
 */
@Data
@NoArgsConstructor
public class AlertRecordResponse {
    // 告警记录ID
    private Long id;
    // 告警类型
    private String type;
    // 告警级别
    private String level;
    // 告警标题
    private String title;
    // 告警内容
    private String content;
    // 告警指标值
    private String metricValue;
    // 告警阈值
    private String thresholdValue;
    // 告警状态
    private String status;
    // 处理人
    private String handledBy;
    // 处理时间
    private LocalDateTime handledAt;
    // 处理备注
    private String handleRemark;
    // 告警创建时间
    private LocalDateTime createdAt;

    /**
     * 从实体对象转换为响应DTO
     * @param entity 告警记录实体
     * @return 告警记录响应DTO
     */
    public static AlertRecordResponse from(AlertRecord entity) {
        AlertRecordResponse response = new AlertRecordResponse();
        response.setId(entity.getId());
        response.setType(entity.getType());
        response.setLevel(entity.getLevel());
        response.setTitle(entity.getTitle());
        response.setContent(entity.getContent());
        response.setMetricValue(entity.getMetricValue());
        response.setThresholdValue(entity.getThresholdValue());
        response.setStatus(entity.getStatus().name());
        response.setHandledBy(entity.getHandledBy());
        response.setHandledAt(entity.getHandledAt());
        response.setHandleRemark(entity.getHandleRemark());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
