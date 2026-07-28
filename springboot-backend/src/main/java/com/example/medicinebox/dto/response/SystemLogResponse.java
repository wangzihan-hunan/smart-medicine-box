package com.example.medicinebox.dto.response;

import com.example.medicinebox.entity.SystemLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统日志详情响应DTO
 * 用于返回单条系统操作日志的详细信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogResponse {

    // 日志ID
    private Integer id;
    // 操作用户ID
    private Integer userId;
    // 操作用户姓名
    private String userName;
    // 操作用户手机号
    private String userPhone;
    // 操作模块
    private String module;
    // 操作类型
    private String operation;
    // 操作描述
    private String description;
    // 请求方法（GET/POST等）
    private String requestMethod;
    // 请求URL
    private String requestUrl;
    // 操作IP地址
    private String ipAddress;
    // 操作是否成功
    private Boolean success;
    // 错误信息（失败时）
    private String errorMessage;
    // 操作耗时（毫秒）
    private Long durationMs;
    // 操作时间
    private LocalDateTime createdAt;

    /**
     * 从实体对象转换为响应DTO
     * @param log 系统日志实体
     * @return 系统日志响应DTO
     */
    public static SystemLogResponse fromEntity(SystemLog log) {
        return new SystemLogResponse(
                log.getId(),
                log.getUserId(),
                log.getUserName(),
                log.getUserPhone(),
                log.getModule(),
                log.getOperation(),
                log.getDescription(),
                log.getRequestMethod(),
                log.getRequestUrl(),
                log.getIpAddress(),
                log.getSuccess(),
                log.getErrorMessage(),
                log.getDurationMs(),
                log.getCreatedAt()
        );
    }
}
