package com.example.medicinebox.dto.response;

import com.example.medicinebox.entity.Reminder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 用药提醒详情响应DTO
 * 用于返回单个用药提醒的详细信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResponse {
    // 提醒ID
    private Integer id;
    // 所属用户ID
    private Integer userId;
    // 关联药品ID
    private Integer medicineId;
    // 药品名称
    private String medicineName;
    // 关联处方ID
    private Integer prescriptionId;
    // 提醒时间
    private String time;
    // 用药剂量
    private String dosage;
    // 用药频次
    private String frequency;
    // 是否重复提醒
    private Boolean isRepeat;
    // 是否已服药
    private Boolean taken;
    // 备注信息
    private String remark;
    // 提醒开始日期
    private LocalDate startDate;
    // 提醒结束日期
    private LocalDate endDate;
    // 重复结束类型：never/endDate/count
    private String repeatEndType;
    // 重复结束日期
    private LocalDate repeatEndDate;
    // 重复次数
    private Integer repeatCount;
    // 已触发次数
    private Integer triggeredCount;
    // 提前通知时间（分钟）
    private Integer advanceNotice;
    // 提醒音效
    private String soundEffect;

    /**
     * 从实体对象转换为响应DTO
     * @param reminder 提醒实体
     * @return 提醒响应DTO
     */
    public static ReminderResponse fromEntity(Reminder reminder) {
        ReminderResponse response = new ReminderResponse();
        response.setId(reminder.getId());
        response.setUserId(reminder.getUserId());
        response.setMedicineId(reminder.getMedicineId());
        response.setMedicineName(reminder.getMedicineName());
        response.setPrescriptionId(reminder.getPrescriptionId());
        response.setTime(reminder.getTime());
        response.setDosage(reminder.getDosage());
        response.setFrequency(reminder.getFrequency());
        response.setIsRepeat(reminder.getIsRepeat());
        response.setTaken(reminder.getTaken());
        response.setRemark(reminder.getRemark());
        response.setStartDate(reminder.getStartDate());
        response.setEndDate(reminder.getEndDate());
        response.setRepeatEndType(reminder.getRepeatEndType());
        response.setRepeatEndDate(reminder.getRepeatEndDate());
        response.setRepeatCount(reminder.getRepeatCount());
        response.setTriggeredCount(reminder.getTriggeredCount());
        response.setAdvanceNotice(reminder.getAdvanceNotice());
        response.setSoundEffect(reminder.getSoundEffect());
        return response;
    }
}
