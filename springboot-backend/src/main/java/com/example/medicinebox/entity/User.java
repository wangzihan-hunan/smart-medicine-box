package com.example.medicinebox.entity;

import com.example.medicinebox.constant.UserRole;
import com.example.medicinebox.constant.UserStatus;
import com.example.medicinebox.constant.UserPlan;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：users
 * 业务含义：存储系统用户的基本信息、账号状态、套餐权限等
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_phone", columnList = "phone"),
        @Index(name = "idx_users_status", columnList = "status"),
        @Index(name = "idx_users_role", columnList = "role")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    // 用户唯一标识，主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 手机号，用户登录账号，唯一且不可为空
    @Column(unique = true, length = 20, nullable = false)
    private String phone;

    // 密码，加密存储
    @Column(length = 255, nullable = false)
    private String password;

    // 用户昵称/姓名
    @Column(name = "user_name", length = 50)
    private String name;

    // 用户角色：USER(普通用户)、ADMIN(管理员)等
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserRole role = UserRole.USER;

    // 账号状态：ACTIVE(正常)、LOCKED(锁定)、DISABLED(禁用)等
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // 手机号是否已验证
    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;

    // 登录失败次数，用于账号锁定策略
    @Column(name = "login_attempts")
    private Integer loginAttempts = 0;

    // 账号锁定时间，超过锁定时长后自动解锁
    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    // 最后一次登录时间
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    // 最后一次登录IP地址
    @Column(name = "last_ip", length = 50)
    private String lastIp;

    // 用户套餐类型：FREE(免费版)、BASIC(基础版)、PROFESSIONAL(专业版)
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", length = 20)
    private UserPlan plan = UserPlan.FREE;

    // 套餐到期时间
    @Column(name = "plan_expire_at")
    private LocalDateTime planExpireAt;

    // 当月已使用的AI识别次数
    @Column(name = "month_ai_count")
    private Integer monthAiCount = 0;

    // 当月AI识别次数上限
    @Column(name = "month_ai_limit")
    private Integer monthAiLimit = 5;

    // 是否需要强制修改密码（默认账号首次登录时为true）
    @Column(name = "must_change_password")
    private Boolean mustChangePassword = false;

    // 上月度重置时间，用于每月重置AI识别次数
    @Column(name = "month_reset_at")
    private LocalDateTime monthResetAt;

    // 记录创建时间
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 记录最后更新时间
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 实体持久化前自动设置创建时间、更新时间及默认值
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (role == null) role = UserRole.USER;
        if (status == null) status = UserStatus.ACTIVE;
        if (phoneVerified == null) phoneVerified = false;
        if (loginAttempts == null) loginAttempts = 0;
        if (plan == null) plan = UserPlan.FREE;
        if (monthAiCount == null) monthAiCount = 0;
        if (monthAiLimit == null) monthAiLimit = 5;
        if (monthResetAt == null) monthResetAt = LocalDateTime.now();
        if (mustChangePassword == null) mustChangePassword = false;
    }

    // 实体更新前自动设置更新时间
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 领域方法 ====================

    /** 账号是否处于激活状态 */
    public boolean isActive() {
        return UserStatus.ACTIVE == status;
    }

    /** 账号是否被锁定 */
    public boolean isLocked() {
        return UserStatus.LOCKED == status;
    }

    /** 账号是否被封禁 */
    public boolean isBlocked() {
        return UserStatus.BLOCKED == status;
    }

    /** 账号是否被冻结 */
    public boolean isSuspended() {
        return UserStatus.SUSPENDED == status;
    }

    /** 是否为管理员 */
    public boolean isAdmin() {
        return UserRole.ADMIN == role;
    }

    /**
     * 判断锁定期是否已过，可以自动解锁
     * @param lockDurationMinutes 锁定时长（分钟）
     */
    public boolean shouldAutoUnlock(long lockDurationMinutes) {
        return isLocked() && lockTime != null
                && LocalDateTime.now().isAfter(lockTime.plusMinutes(lockDurationMinutes));
    }

    /** 解锁账号：重置状态、锁定时间和登录尝试次数 */
    public void unlock() {
        this.status = UserStatus.ACTIVE;
        this.lockTime = null;
        this.loginAttempts = 0;
    }

    /**
     * 增加登录失败次数，达到阈值时自动锁定
     * @param maxAttempts 最大尝试次数阈值
     */
    public void incrementLoginAttempts(int maxAttempts) {
        this.loginAttempts = (this.loginAttempts == null ? 0 : this.loginAttempts) + 1;
        if (this.loginAttempts >= maxAttempts) {
            this.status = UserStatus.LOCKED;
            this.lockTime = LocalDateTime.now();
        }
    }

    /** 重置登录尝试次数 */
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.lastLoginTime = LocalDateTime.now();
    }

    /** 封禁账号 */
    public void block() {
        this.status = UserStatus.BLOCKED;
        this.lockTime = LocalDateTime.now();
    }

    /** 冻结账号 */
    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.lockTime = LocalDateTime.now();
    }
}
