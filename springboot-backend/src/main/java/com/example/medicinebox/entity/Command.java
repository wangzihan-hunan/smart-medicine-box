package com.example.medicinebox.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备指令实体类
 * 对应数据库表：commands
 * 业务含义：存储发送给智能药盒设备的控制指令，如出药、灯光提示、声音提醒等
 */
@Entity
@Table(name = "commands", indexes = {
        @Index(name = "idx_commands_device_id", columnList = "device_id"),
        @Index(name = "idx_commands_user_id", columnList = "user_id"),
        @Index(name = "idx_commands_status", columnList = "status"),
        @Index(name = "idx_commands_medicine_id", columnList = "medicine_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Command {

    // 指令唯一标识，主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 目标设备ID
    @Column(name = "device_id", length = 50)
    private String deviceId;

    // 所属用户ID
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // 关联的药品ID
    @Column(name = "medicine_id")
    private Integer medicineId;

    // 指令动作类型：如 DISPENSE(出药)、BEEP(蜂鸣)、LIGHT(灯光)等
    @Column(length = 50)
    private String action;

    // 指令参数，JSON格式字符串
    @Column(length = 255)
    private String params;

    // 执行时间（定时指令）
    @Column(length = 50)
    private String time;

    // 指令状态：pending(待执行)、executing(执行中)、success(成功)、failed(失败)
    @Column(length = 20)
    private String status = "pending";

    // 记录创建时间
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 记录最后更新时间
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 乐观锁版本号，用于并发控制
    @Version
    private Long version;

    // 实体持久化前自动设置创建时间、更新时间及默认值
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "pending";
    }

    // 实体更新前自动设置更新时间
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 领域方法 ====================

    /** 标记指令为已发送 */
    public void markSent() {
        this.status = "已发送";
    }

    /** 标记指令为已执行 */
    public void markExecuted() {
        this.status = "已执行";
    }

    /** 标记指令为执行失败 */
    public void markFailed() {
        this.status = "失败";
    }

    /** 指令是否处于待执行状态 */
    public boolean isPending() {
        return "pending".equals(status);
    }

    /** 指令是否已执行 */
    public boolean isExecuted() {
        return "已执行".equals(status);
    }
}