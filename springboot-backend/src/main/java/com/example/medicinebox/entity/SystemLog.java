package com.example.medicinebox.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统日志实体类
 * 对应数据库表：system_logs
 * 业务含义：记录系统操作日志，包括用户操作、请求信息、响应结果等，用于审计和问题排查
 */
@Entity
@Table(name = "system_logs", indexes = {
        @Index(name = "idx_system_logs_user_id", columnList = "user_id"),
        @Index(name = "idx_system_logs_operation", columnList = "operation"),
        @Index(name = "idx_system_logs_module", columnList = "module"),
        @Index(name = "idx_system_logs_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemLog {

    // 日志唯一标识，主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 操作用户ID
    @Column(name = "user_id")
    private Integer userId;

    // 操作用户名称
    @Column(name = "user_name", length = 50)
    private String userName;

    // 操作用户手机号
    @Column(name = "user_phone", length = 20)
    private String userPhone;

    // 所属模块：如 user(用户模块)、medicine(药品模块)、reminder(提醒模块)等
    @Column(length = 50)
    private String module;

    // 操作类型：如 login(登录)、create(新增)、update(更新)、delete(删除)等
    @Column(length = 100)
    private String operation;

    // 操作描述
    @Column(length = 255)
    private String description;

    // 请求方法：GET、POST、PUT、DELETE等
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    // 请求URL
    @Column(name = "request_url", length = 500)
    private String requestUrl;

    // 请求参数，JSON格式
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    // 请求IP地址
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    // 用户代理（浏览器/客户端信息）
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // 响应状态码
    @Column(name = "response_status")
    private Integer responseStatus;

    // 操作是否成功
    @Column(name = "success")
    private Boolean success = true;

    // 错误信息（操作失败时记录）
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // 执行耗时（毫秒）
    @Column(name = "duration_ms")
    private Long durationMs;

    // 日志创建时间
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 实体持久化前自动设置创建时间及默认值
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (success == null) success = true;
    }
}
