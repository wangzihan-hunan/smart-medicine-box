package com.example.medicinebox.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用药提醒实体类
 * 对应数据库表：reminders
 * 业务含义：存储用户的用药提醒计划，包括提醒时间、用药剂量、服用频率等
 */
@Entity
@Table(name = "reminders", indexes = {
        @Index(name = "idx_reminders_medicine_name", columnList = "medicine_name"),
        @Index(name = "idx_reminders_user_id", columnList = "user_id"),
        @Index(name = "idx_reminders_taken", columnList = "taken"),
        @Index(name = "idx_reminders_medicine_id", columnList = "medicine_id"),
        @Index(name = "idx_reminders_prescription_id", columnList = "prescription_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    // 提醒记录唯一标识，主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 关联的药品ID
    @Column(name = "medicine_id")
    private Integer medicineId;

    // 药品名称（冗余字段，方便查询展示）
    @Column(name = "medicine_name", length = 100, nullable = false)
    private String medicineName;

    // 所属用户ID
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // 关联的处方ID（可选，根据处方生成的提醒）
    @Column(name = "prescription_id")
    private Integer prescriptionId;

    // 提醒时间，格式如：08:00、12:00、20:00
    @Column(length = 20)
    private String time;

    // 用药剂量，如：1片、2粒、10ml等
    @Column(length = 50)
    private String dosage;

    // 用药频率，如：每日一次、每日两次、每日三次等
    @Column(length = 50)
    private String frequency = "每日一次";

    // 是否重复提醒，true-按周期重复，false-仅单次
    @Column(name = "is_repeat")
    private Boolean isRepeat = false;

    // 是否激活，true-提醒有效，false-提醒已暂停/取消
    @Column(name = "is_active")
    private Boolean isActive = true;

    // 是否已服用，true-已服用，false-未服用
    @Column
    private Boolean taken = false;

    // 备注信息
    @Column(length = 255)
    private String remark;

    // 提醒开始日期
    @Column(name = "start_date")
    private LocalDate startDate;

    // 提醒结束日期
    @Column(name = "end_date")
    private LocalDate endDate;

    // 记录创建时间
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 记录最后更新时间
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 下次提醒时间，用于定时扫描推送
    @Column(name = "next_remind_time")
    private LocalDateTime nextRemindTime;

    // 重复结束类型：never(永不结束)、endDate(结束于日期)、count(重复N次)
    @Column(name = "repeat_end_type", length = 20)
    private String repeatEndType = "never";

    // 重复结束日期（repeatEndType=endDate时使用）
    @Column(name = "repeat_end_date")
    private LocalDate repeatEndDate;

    // 重复次数（repeatEndType=count时使用）
    @Column(name = "repeat_count")
    private Integer repeatCount = 0;

    // 已触发次数（用于count结束条件统计）
    @Column(name = "triggered_count")
    private Integer triggeredCount = 0;

    // 提前通知时间（分钟），0表示准时通知
    @Column(name = "advance_notice")
    private Integer advanceNotice = 0;

    // 提醒音效：default/gentle/urgent/melody/silent
    @Column(name = "sound_effect", length = 20)
    private String soundEffect = "default";

    // 乐观锁版本号，用于并发控制
    @Version
    private Long version;

    // 实体持久化前自动设置创建时间、更新时间及默认值
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isRepeat == null) isRepeat = false;
        if (isActive == null) isActive = true;
        if (taken == null) taken = false;
        if (frequency == null) frequency = "每日一次";
        if (nextRemindTime == null) {
            nextRemindTime = calculateInitialNextRemindTime();
        }
        if (repeatEndType == null) repeatEndType = "never";
        if (repeatCount == null) repeatCount = 0;
        if (triggeredCount == null) triggeredCount = 0;
        if (advanceNotice == null) advanceNotice = 0;
        if (soundEffect == null) soundEffect = "default";
    }

    /**
     * 计算初始的下次提醒时间
     * 根据提醒日期（startDate/endDate）和提醒时间（time）计算下一次实际触发时间
     *
     * @return 初始下次提醒时间
     */
    private LocalDateTime calculateInitialNextRemindTime() {
        LocalDate baseDate = startDate != null ? startDate : LocalDate.now();
        LocalTime remindTime = LocalTime.parse("08:00");
        try {
            if (time != null && !time.trim().isEmpty()) {
                String normalizedTime = time.trim();
                if (normalizedTime.length() == 5) {
                    remindTime = LocalTime.parse(normalizedTime);
                }
            }
        } catch (Exception e) {
            // 解析失败时使用默认时间
        }
        return LocalDateTime.of(baseDate, remindTime);
    }

    // 实体更新前自动设置更新时间
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 领域方法 ====================

    /**
     * 处理重复提醒的结束条件
     * 支持按次数结束（count）和按日期结束（endDate）两种模式
     *
     * @return true-已达到结束条件，不应再触发；false-未达到，可继续触发
     */
    public boolean shouldStopRepeat() {
        if (!Boolean.TRUE.equals(isRepeat)) {
            return false;
        }

        // 按次数结束：触发次数达到上限后自动停用
        if ("count".equals(repeatEndType) && repeatCount != null) {
            int newCount = (triggeredCount != null ? triggeredCount : 0) + 1;
            triggeredCount = newCount;
            if (newCount >= repeatCount) {
                isActive = false;
                return true;
            }
        }

        // 按日期结束：超过结束日期后自动停用
        if ("endDate".equals(repeatEndType) && repeatEndDate != null) {
            if (LocalDate.now().isAfter(repeatEndDate)) {
                isActive = false;
                return true;
            }
        }

        return false;
    }

    /**
     * 更新重复提醒的下次触发时间
     * 根据提醒时间和当前日期计算下次触发时间
     */
    public void advanceToNextRemindTime() {
        try {
            LocalTime remindTime = LocalTime.parse(time);
            LocalDate nextDate = LocalDate.now().plusDays(1);
            nextRemindTime = LocalDateTime.of(nextDate, remindTime);
        } catch (Exception e) {
            // 解析失败时保持现有值
        }
    }

    /** 标记为已服用 */
    public void markTaken() {
        this.taken = true;
    }

    /** 停用提醒 */
    public void deactivate() {
        this.isActive = false;
    }
}
